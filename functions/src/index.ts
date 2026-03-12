import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { onRequest } from "firebase-functions/v2/https";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineString } from "firebase-functions/params";

// Environment variables for PesePay
const pesepayKey      = defineString("PESEPAY_INTEGRATION_KEY",      { default: "" });
const pesepayPassword = defineString("PESEPAY_INTEGRATION_PASSWORD", { default: "" });
const paymentModeParam = defineString("PAYMENT_MODE", { default: "demo" });

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();

const BROKERAGE_SUBSCRIPTION_FEE_USD = 100.0;
const BROKERAGE_MINIMUM_FLOAT_USD = 40.0;
const BROKERAGE_COMMISSION_RATE = 0.15;
const BROKERAGE_LEDGER_COLLECTION = "brokerage_float_ledger";
const BROKERAGE_TOPUP_COLLECTION = "brokerage_topup_requests";

function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

function resolvePaymentMode(): "demo" | "live" {
  const rawMode = (paymentModeParam.value() || process.env.PAYMENT_MODE || "auto").toLowerCase();
  if (rawMode === "demo") return "demo";
  if (rawMode === "live") return "live";

  const key = pesepayKey.value() || process.env.PESEPAY_INTEGRATION_KEY || "";
  const password = pesepayPassword.value() || process.env.PESEPAY_INTEGRATION_PASSWORD || "";
  const isDemoCredentials =
    !key || !password ||
    key === "demo" ||
    key === "demo_key_for_testing" ||
    password === "demo_password_for_testing";

  return isDemoCredentials ? "demo" : "live";
}

function buildHostedSimulatorUrl(path: string, params: Record<string, string | number | boolean>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => query.set(key, String(value)));
  return `https://rentout-12239.web.app/${path}?${query.toString()}`;
}

async function syncBrokeragePropertyUnlockState(brokerageId: string, isFrozen: boolean, balance: number, minimumFloat: number) {
  const propsSnap = await db.collection("properties").where("landlordId", "==", brokerageId).get();
  if (propsSnap.empty) return;

  const reason = isFrozen
    ? `Tenant unlocks are temporarily frozen while the brokerage insurance float is below $${minimumFloat.toFixed(2)}. Current balance: $${balance.toFixed(2)}`
    : "";

  const batch = db.batch();
  propsSnap.docs.forEach((doc) => {
    batch.update(doc.ref, {
      brokerageUnlockEnabled: !isFrozen,
      brokerageFreezeReason: reason,
    });
  });
  await batch.commit();
}

async function assertBrokerageCanUnlock(propertyId: string, landlordId: string, amount: number) {
  const propertyDoc = await db.collection("properties").doc(propertyId).get();
  const propertyData = propertyDoc.data() || {};
  const providerSubtype = propertyData.providerSubtype || "";

  if (providerSubtype !== "brokerage") {
    return { providerSubtype };
  }

  const userDoc = await db.collection("users").doc(landlordId).get();
  const userData = userDoc.data() || {};
  const currentBalance = roundMoney(userData.brokerageFloatBalanceUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD);
  const minimumFloat = roundMoney(userData.brokerageMinimumFloatUsd ?? BROKERAGE_MINIMUM_FLOAT_USD);
  const commissionRate = userData.brokerageCommissionRate ?? BROKERAGE_COMMISSION_RATE;
  const deduction = roundMoney(amount * commissionRate);
  const projectedBalance = roundMoney(currentBalance - deduction);

  if (projectedBalance < minimumFloat || userData.brokerageIsFrozen === true) {
    await db.collection("users").doc(landlordId).set({
      brokerageSubscriptionFeeUsd: userData.brokerageSubscriptionFeeUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD,
      brokerageFloatBalanceUsd: currentBalance,
      brokerageMinimumFloatUsd: minimumFloat,
      brokerageCommissionRate: commissionRate,
      brokerageIsFrozen: true,
    }, { merge: true });
    await syncBrokeragePropertyUnlockState(landlordId, true, currentBalance, minimumFloat);
    throw new HttpsError(
      "failed-precondition",
      `This brokerage's insurance float is below the minimum safety floor of $${minimumFloat.toFixed(2)}. Ask the brokerage to top up before unlocking.`
    );
  }

  return { providerSubtype };
}

async function applyBrokerageUnlockCharge(params: {
  transactionId: string;
  tenantId: string;
  landlordId: string;
  propertyId: string;
  amount: number;
}) {
  const { transactionId, tenantId, landlordId, propertyId, amount } = params;
  const propertyDoc = await db.collection("properties").doc(propertyId).get();
  const propertyData = propertyDoc.data() || {};
  const providerSubtype = propertyData.providerSubtype || "";

  if (providerSubtype !== "brokerage") {
    await db.collection("transactions").doc(transactionId).set({ providerSubtype }, { merge: true });
    return { blocked: false, providerSubtype };
  }

  const userRef = db.collection("users").doc(landlordId);
  const transactionRef = db.collection("transactions").doc(transactionId);
  const result = await db.runTransaction(async (tx) => {
    const [userSnap, transactionSnap] = await Promise.all([tx.get(userRef), tx.get(transactionRef)]);
    const userData = userSnap.data() || {};
    const transactionData = transactionSnap.data() || {};

    if (transactionData.brokerageSettlementStatus === "applied") {
      return {
        blocked: false,
        providerSubtype,
        balance: transactionData.brokerageFloatAfter ?? userData.brokerageFloatBalanceUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD,
        minimumFloat: userData.brokerageMinimumFloatUsd ?? BROKERAGE_MINIMUM_FLOAT_USD,
      };
    }

    const currentBalance = roundMoney(userData.brokerageFloatBalanceUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD);
    const minimumFloat = roundMoney(userData.brokerageMinimumFloatUsd ?? BROKERAGE_MINIMUM_FLOAT_USD);
    const commissionRate = userData.brokerageCommissionRate ?? BROKERAGE_COMMISSION_RATE;
    const deduction = roundMoney(amount * commissionRate);
    const nextBalance = roundMoney(currentBalance - deduction);

    if (nextBalance < minimumFloat) {
      tx.set(userRef, {
        brokerageSubscriptionFeeUsd: userData.brokerageSubscriptionFeeUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD,
        brokerageFloatBalanceUsd: currentBalance,
        brokerageMinimumFloatUsd: minimumFloat,
        brokerageCommissionRate: commissionRate,
        brokerageIsFrozen: true,
        brokerageLastTransactionId: transactionId,
      }, { merge: true });
      tx.set(transactionRef, {
        providerSubtype,
        brokerageDeductionAmount: deduction,
        brokerageFloatBefore: currentBalance,
        brokerageFloatAfter: currentBalance,
        brokerageSettlementStatus: "blocked",
        brokerageStatusMessage: `Brokerage float cannot go below $${minimumFloat.toFixed(2)}.`,
      }, { merge: true });
      return { blocked: true, providerSubtype, balance: currentBalance, minimumFloat };
    }

    const ledgerRef = db.collection(BROKERAGE_LEDGER_COLLECTION).doc();
    const now = Date.now();
    tx.set(ledgerRef, {
      id: ledgerRef.id,
      brokerageId: landlordId,
      type: "unlock_deduction",
      direction: "debit",
      amount: deduction,
      currency: "USD",
      balanceBefore: currentBalance,
      balanceAfter: nextBalance,
      createdAt: now,
      relatedTransactionId: transactionId,
      propertyId,
      tenantId,
      title: "Tenant unlock deduction",
      description: `15% RentOut deduction applied to unlock transaction ${transactionId}`,
      status: "success",
      performedBy: "system",
    });

    tx.set(userRef, {
      brokerageSubscriptionFeeUsd: userData.brokerageSubscriptionFeeUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD,
      brokerageFloatBalanceUsd: nextBalance,
      brokerageMinimumFloatUsd: minimumFloat,
      brokerageCommissionRate: commissionRate,
      brokerageIsFrozen: nextBalance < minimumFloat,
      brokerageLastDeductionAt: now,
      brokerageLastTransactionId: transactionId,
    }, { merge: true });

    tx.set(transactionRef, {
      providerSubtype,
      brokerageDeductionAmount: deduction,
      brokerageFloatBefore: currentBalance,
      brokerageFloatAfter: nextBalance,
      brokerageLedgerEntryId: ledgerRef.id,
      brokerageSettlementStatus: "applied",
      brokerageStatusMessage: "15% brokerage float deduction applied.",
    }, { merge: true });

    return { blocked: false, providerSubtype, balance: nextBalance, minimumFloat };
  });

  await syncBrokeragePropertyUnlockState(landlordId, result.balance < result.minimumFloat, result.balance, result.minimumFloat);
  return result;
}

async function applyBrokerageTopUpCredit(params: {
  requestId: string;
  brokerageId: string;
  amountUsd: number;
  paymentProvider: string;
  paymentReference: string;
  performedBy: string;
}) {
  const { requestId, brokerageId, amountUsd, paymentProvider, paymentReference, performedBy } = params;
  const userRef = db.collection("users").doc(brokerageId);
  const requestRef = db.collection(BROKERAGE_TOPUP_COLLECTION).doc(requestId);

  const result = await db.runTransaction(async (tx) => {
    const [userSnap, requestSnap] = await Promise.all([tx.get(userRef), tx.get(requestRef)]);
    const userData = userSnap.data() || {};
    const requestData = requestSnap.data() || {};

    if (requestData.status === "success") {
      return {
        nextBalance: roundMoney(requestData.balanceAfter ?? userData.brokerageFloatBalanceUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD),
        minimumFloat: roundMoney(userData.brokerageMinimumFloatUsd ?? BROKERAGE_MINIMUM_FLOAT_USD),
      };
    }

    const currentBalance = roundMoney(userData.brokerageFloatBalanceUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD);
    const minimumFloat = roundMoney(userData.brokerageMinimumFloatUsd ?? BROKERAGE_MINIMUM_FLOAT_USD);
    const nextBalance = roundMoney(currentBalance + amountUsd);
    const now = Date.now();
    const ledgerRef = db.collection(BROKERAGE_LEDGER_COLLECTION).doc();

    tx.set(ledgerRef, {
      id: ledgerRef.id,
      brokerageId,
      type: "top_up",
      direction: "credit",
      amount: roundMoney(amountUsd),
      currency: "USD",
      balanceBefore: currentBalance,
      balanceAfter: nextBalance,
      createdAt: now,
      relatedTransactionId: requestId,
      propertyId: "",
      tenantId: "",
      title: "Brokerage float top-up",
      description: `Brokerage topped up insurance float by $${roundMoney(amountUsd).toFixed(2)}`,
      status: "success",
      performedBy,
    });

    tx.set(userRef, {
      brokerageSubscriptionFeeUsd: userData.brokerageSubscriptionFeeUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD,
      brokerageFloatBalanceUsd: nextBalance,
      brokerageMinimumFloatUsd: minimumFloat,
      brokerageCommissionRate: userData.brokerageCommissionRate ?? BROKERAGE_COMMISSION_RATE,
      brokerageIsFrozen: nextBalance < minimumFloat,
      brokerageLastTopUpAt: now,
    }, { merge: true });

    tx.set(requestRef, {
      status: "success",
      completedAt: now,
      ledgerEntryId: ledgerRef.id,
      paymentProvider,
      paymentReference,
      message: "Brokerage top-up credited successfully.",
      balanceBefore: currentBalance,
      balanceAfter: nextBalance,
    }, { merge: true });

    return { nextBalance, minimumFloat };
  });

  await syncBrokeragePropertyUnlockState(brokerageId, result.nextBalance < result.minimumFloat, result.nextBalance, result.minimumFloat);
  return result;
}

// ─── 1. onUserCreate — Set default role & status when user doc is first created ─
// The client writes a minimal user doc on signup; this function fills in defaults.
export const onUserCreate = onDocumentCreated("users/{uid}", async (event) => {
  const data = event.data?.data();
  if (!data) return;
  try {
    logger.info(`New user doc created: ${event.params.uid}`);
    await event.data?.ref.set({
      role:      data.role      ?? "tenant",
      status:    data.status    ?? "active",
      createdAt: data.createdAt ?? admin.firestore.FieldValue.serverTimestamp(),
      brokerageSubscriptionFeeUsd: data.providerSubtype === "brokerage" ? (data.brokerageSubscriptionFeeUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD) : (data.brokerageSubscriptionFeeUsd ?? 0),
      brokerageFloatBalanceUsd: data.providerSubtype === "brokerage" ? (data.brokerageFloatBalanceUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD) : (data.brokerageFloatBalanceUsd ?? 0),
      brokerageMinimumFloatUsd: data.brokerageMinimumFloatUsd ?? BROKERAGE_MINIMUM_FLOAT_USD,
      brokerageCommissionRate: data.providerSubtype === "brokerage" ? (data.brokerageCommissionRate ?? BROKERAGE_COMMISSION_RATE) : (data.brokerageCommissionRate ?? 0),
      brokerageIsFrozen: data.brokerageIsFrozen ?? false,
      brokerageLastTopUpAt: data.brokerageLastTopUpAt ?? (data.providerSubtype === "brokerage" ? Date.now() : 0),
      brokerageLastDeductionAt: data.brokerageLastDeductionAt ?? 0,
      brokerageLastTransactionId: data.brokerageLastTransactionId ?? "",
    }, { merge: true });
    logger.info(`User defaults set for ${event.params.uid}`);

    // Send welcome notification
    const userRole = data.role ?? "tenant";
    const roleLabel = userRole === "landlord" ? "Landlord"
                    : userRole === "tenant"   ? "Tenant"
                    : userRole === "agent"    ? "Agent"
                    : userRole === "brokerage"? "Brokerage"
                    : "User";
    await sendNotification({
      recipientId: event.params.uid,
      role:        userRole,
      type:        "welcome",
      title:       "👋 Welcome to RentOut!",
      message:     `Welcome, ${roleLabel}! Your account is ready. ${userRole === "tenant" ? "Browse properties and unlock contact details to get in touch with landlords." : "Start listing your properties and reach thousands of tenants."}`,
    });
  } catch (error) {
    logger.error(`Error setting user defaults: ${error}`);
  }
});

// ─── 2. updateUserRole — Called from app after role selection ─────────────────
export const updateUserRole = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Must be logged in.");
  }
  const { role } = request.data as { role: string };
  if (!["landlord", "tenant"].includes(role)) {
    throw new HttpsError("invalid-argument", "Invalid role. Must be landlord or tenant.");
  }
  await db.collection("users").doc(request.auth.uid).update({ role });
  logger.info(`User ${request.auth.uid} role updated to ${role}`);
  return { success: true, role };
});

// ─── 3. initiatePayment — Called by client to start PesePay checkout ─────────
// In demo/test mode (demo_key_for_testing): auto-creates the unlock doc
// immediately so the flow works end-to-end without a real PesePay account.
// In production: creates a PesePay checkout session and returns the redirect URL;
// the verifyPesePay webhook writes the unlock doc after real payment completes.
export const initiatePayment = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Must be logged in.");
  }

  const { propertyId, landlordId, successUrl, cancelUrl } =
    request.data as {
      propertyId: string;
      landlordId: string;
      successUrl?: string;
      cancelUrl?:  string;
    };

  if (!propertyId || !landlordId) {
    throw new HttpsError("invalid-argument", "propertyId and landlordId are required.");
  }

  const tenantId = request.auth.uid;
  const unlockId = `${tenantId}_${propertyId}`;
  const { providerSubtype } = await assertBrokerageCanUnlock(propertyId, landlordId, 10.0);

  // Idempotency — already unlocked
  const existingUnlock = await db.collection("unlocks").doc(unlockId).get();
  if (existingUnlock.exists) {
    // Already unlocked — return success immediately so client doesn't re-poll
    const existingTxSnap = await db.collection("transactions")
      .where("tenantId", "==", tenantId)
      .where("propertyId", "==", propertyId)
      .where("status", "==", "success")
      .limit(1)
      .get();
    const existingTxId = existingTxSnap.empty ? "" : existingTxSnap.docs[0].id;
    return { success: true, alreadyUnlocked: true, unlockId, transactionId: existingTxId, demoMode: true };
  }

  // Create pending transaction record
  const transactionRef = await db.collection("transactions").add({
    tenantId,
    propertyId,
    landlordId,
    amount:           10.0,
    currency:         "USD",
    status:           "pending",
    paymentProvider:  "pesepay",
    paymentReference: `REF-${Date.now()}-${tenantId.substring(0, 6)}`,
    createdAt:        Date.now(),
    providerSubtype,
    brokerageSettlementStatus: providerSubtype === "brokerage" ? "pending" : "not_applicable",
  });

  // Resolve credentials — prefer env params, fall back to process.env
  const pesepayIntegrationKey      = pesepayKey.value()      || process.env.PESEPAY_INTEGRATION_KEY      || "demo";
  const pesepayIntegrationPassword = pesepayPassword.value() || process.env.PESEPAY_INTEGRATION_PASSWORD || "demo";

  const isDemoMode = resolvePaymentMode() === "demo";

  try {
    let checkoutUrl: string;

    if (isDemoMode) {
      // ── Demo / test mode ────────────────────────────────────────────────────
      // Auto-create the unlock doc immediately so the app can confirm it
      // without any external redirect or webhook.
      logger.info(`Demo payment initiated: transaction=${transactionRef.id}`);

      const brokerageSettlement = await applyBrokerageUnlockCharge({
        transactionId: transactionRef.id,
        tenantId,
        landlordId,
        propertyId,
        amount: 10.0,
      });
      if (brokerageSettlement.blocked) {
        await transactionRef.update({ status: "failed" });
        throw new HttpsError("failed-precondition", "Brokerage float is below the minimum threshold. Unlock blocked.");
      }

      await db.collection("unlocks").doc(unlockId).set({
        id:            unlockId,
        tenantId,
        propertyId,
        transactionId: transactionRef.id,
        unlockedAt:    admin.firestore.FieldValue.serverTimestamp(),
        isDemo:        true,
      });

      await transactionRef.update({ status: "success" });

      // Fetch property details for notification messages
      const propSnap = await db.collection("properties").doc(propertyId).get();
      const propData  = propSnap.data() || {};
      const propTitle = propData.title ?? "a property";
      const ownerId   = propData.landlordId ?? landlordId;
      const ownerRole = providerSubtype === "brokerage" ? "brokerage"
                      : providerSubtype === "agent"     ? "agent"
                      : "landlord";

      // Notify tenant — unlock success
      await sendNotification({
        recipientId:   tenantId,
        role:          "tenant",
        type:          "unlock_success",
        title:         "🔑 Contact Details Unlocked!",
        message:       `You can now view the full contact details for "${propTitle}". Tap to see landlord info and address.`,
        propertyId,
        propertyTitle: propTitle,
      });

      // Notify the listing owner — their property was unlocked
      if (ownerId) {
        await sendNotification({
          recipientId:   ownerId,
          role:          ownerRole,
          type:          "property_unlocked",
          title:         "🔓 Property Unlocked by Tenant",
          message:       `A tenant has paid to unlock contact details for "${propTitle}".`,
          propertyId,
          propertyTitle: propTitle,
        });
      }

      checkoutUrl = buildHostedSimulatorUrl("payment-simulator.html", {
        transactionId: transactionRef.id,
        tenantId,
        propertyId,
      });
      logger.info(`Demo unlock auto-created: ${unlockId}, transaction: ${transactionRef.id}`);
    } else {
      // ── Production PesePay ──────────────────────────────────────────────────
      const payload = {
        integrationKey:      pesepayIntegrationKey,
        integrationPassword: pesepayIntegrationPassword,
        amount:              10.0,
        currencyCode:        "USD",
        reference:           transactionRef.id,
        productDescription:  `Unlock contact for property ${propertyId}`,
        customerEmail:       request.auth.token?.email       || "",
        customerPhone:       "",
        customerName:        request.auth.token?.name        || "",
        returnUrl:           successUrl,
        cancelUrl:           cancelUrl,
        notificationUrl:     `https://us-central1-rentout-12239.cloudfunctions.net/verifyPesePay`,
        paymentMethod:       "",
      };

      const response = await fetch("https://www.pesepay.com/api/payments/initialize", {
        method:  "POST",
        headers: { "Content-Type": "application/json" },
        body:    JSON.stringify(payload),
      });

      if (!response.ok) {
        const errorText = await response.text();
        logger.error(`PesePay API error: ${errorText}`);
        throw new HttpsError("internal", "Payment provider error.");
      }

      const data: any = await response.json();
      checkoutUrl = data.redirectUrl || data.checkoutUrl;
      if (!checkoutUrl) {
        throw new HttpsError("internal", "Invalid response from payment provider.");
      }

      await transactionRef.update({ paymentReference: payload.reference });
    }

    logger.info(`Payment initiated: transaction=${transactionRef.id}, tenant=${tenantId}, demo=${isDemoMode}`);
    return {
      success:       true,
      transactionId: transactionRef.id,
      checkoutUrl,
      unlockId,
      demoMode:      isDemoMode,
    };
  } catch (error) {
    logger.error(`Payment initiation failed: ${error}`);
    await transactionRef.update({ status: "failed" });
    throw new HttpsError("internal", "Failed to initiate payment.");
  }
});

// ─── Mock Payment Handler for Testing ───────────────────────────────────────
async function handleMockPayment(req: any, res: any) {
  try {
    logger.info("Processing mock payment");
    
    // Mock payment data for testing
    const mockPaymentData = {
      reference: `MOCK_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      tenantId: req.body.tenantId || "mock_tenant_123",
      propertyId: req.body.propertyId || "mock_property_456",
      amount: req.body.amount || 10,
      status: req.body.status || "SUCCESS", // Default to success for testing
      currency: req.body.currency || "USD",
    };

    const { reference, tenantId, propertyId, amount, status, currency } = mockPaymentData;

    logger.info(`Mock payment: ref=${reference}, tenant=${tenantId}, property=${propertyId}, status=${status}`);

    // Validate amount — must be $10 USD (or accept mock amounts)
    if (amount < 5 || currency !== "USD") {
      res.status(400).json({ error: "Invalid payment amount or currency." });
      return;
    }

    // Handle different mock statuses
    if (status !== "SUCCESS" && status !== "PAID") {
      logger.warn(`Mock payment not successful: ${status}`);
      // Fetch property to get landlordId for failed transaction tracking
      const propertyDoc = await db.collection("properties").doc(propertyId).get();
      const landlordId = propertyDoc.exists ? propertyDoc.data()?.landlordId : "";
      // Write failed transaction
      await db.collection("transactions").add({
        tenantId,
        propertyId,
        landlordId,
        amount,
        currency,
        status:           "failed",
        paymentProvider:  "pesepay_mock",
        paymentReference: reference,
        createdAt:        Date.now(),
      });
      res.status(200).json({ success: false, message: "Payment not successful.", mockMode: true });
      return;
    }

    // Check if already unlocked — prevent double charging
    const unlockId = `${tenantId}_${propertyId}`;
    const existingUnlock = await db.collection("unlocks").doc(unlockId).get();
    if (existingUnlock.exists) {
      logger.info(`Property ${propertyId} already unlocked by ${tenantId} (mock)`);
      res.status(200).json({ success: true, message: "Already unlocked.", alreadyUnlocked: true, mockMode: true });
      return;
    }

    // Fetch property to get landlordId
    const propertyDoc = await db.collection("properties").doc(propertyId).get();
    const landlordId = propertyDoc.exists ? propertyDoc.data()?.landlordId : "";

    // Write transaction record
    const transactionRef = await db.collection("transactions").add({
      tenantId,
      propertyId,
      landlordId,
      amount,
      currency,
      status:           "pending",
      paymentProvider:  "pesepay_mock",
      paymentReference: reference,
      createdAt:        Date.now(),
      providerSubtype: propertyDoc.data()?.providerSubtype ?? "",
    });

    const brokerageSettlement = await applyBrokerageUnlockCharge({
      transactionId: transactionRef.id,
      tenantId,
      landlordId,
      propertyId,
      amount,
    });
    if (brokerageSettlement.blocked) {
      await transactionRef.update({ status: "failed" });
      res.status(409).json({ success: false, message: "Brokerage float is below the minimum threshold.", mockMode: true });
      return;
    }

    // Write unlock record — this is what the app checks
    await db.collection("unlocks").doc(unlockId).set({
      id:            unlockId,
      tenantId,
      propertyId,
      transactionId: transactionRef.id,
      unlockedAt:    admin.firestore.FieldValue.serverTimestamp(),
    });

    await transactionRef.update({ status: "success" });

    // Notifications — tenant + property owner
    const mockPropTitle = propertyDoc.data()?.title ?? "a property";
    const mockOwnerId   = landlordId;
    const mockOwnerRole = (propertyDoc.data()?.providerSubtype === "brokerage") ? "brokerage"
                        : (propertyDoc.data()?.providerSubtype === "agent")     ? "agent"
                        : "landlord";
    await sendNotification({
      recipientId:   tenantId,
      role:          "tenant",
      type:          "unlock_success",
      title:         "🔑 Contact Details Unlocked!",
      message:       `You can now view the full contact details for "${mockPropTitle}". Tap to see landlord info and address.`,
      propertyId,
      propertyTitle: mockPropTitle,
    });
    if (mockOwnerId) {
      await sendNotification({
        recipientId:   mockOwnerId,
        role:          mockOwnerRole,
        type:          "property_unlocked",
        title:         "🔓 Property Unlocked by Tenant",
        message:       `A tenant has paid to unlock contact details for "${mockPropTitle}".`,
        propertyId,
        propertyTitle: mockPropTitle,
      });
    }

    logger.info(`Mock unlock written: ${unlockId}`);
    res.status(200).json({ 
      success: true, 
      unlockId, 
      transactionId: transactionRef.id,
      mockMode: true,
      message: "Mock payment processed successfully"
    });

  } catch (error) {
    logger.error(`Mock payment error: ${error}`);
    res.status(500).json({ error: "Internal server error.", mockMode: true });
  }
}

// ─── 3. verifyPesePay — Webhook from PesePay after $10 payment ───────────────
export const verifyPesePay = onRequest(async (req, res) => {
  // Only accept POST requests from PesePay
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }

  // Mock mode for testing without real PesePay integration
  const isMockMode = (!process.env.PESEPAY_INTEGRATION_KEY || process.env.PESEPAY_INTEGRATION_KEY === "demo_key_for_testing") ||
                     (!process.env.PESEPAY_INTEGRATION_PASSWORD || process.env.PESEPAY_INTEGRATION_PASSWORD === "demo_password_for_testing");
  
  if (isMockMode) {
    logger.info("Using mock payment mode for testing");
    return handleMockPayment(req, res);
  }

  try {
    const {
      reference,
      tenantId,
      propertyId,
      amount,
      status,
      currency,
    } = req.body as {
      reference:  string;
      tenantId:   string;
      propertyId: string;
      amount:     number;
      status:     string;
      currency:   string;
    };

    logger.info(`PesePay webhook received: ref=${reference}, tenant=${tenantId}, property=${propertyId}`);

    // Validate required fields
    if (!reference || !tenantId || !propertyId || !amount) {
      res.status(400).json({ error: "Missing required fields." });
      return;
    }

    // Validate amount — must be $10 USD
    if (amount < 10 || currency !== "USD") {
      res.status(400).json({ error: "Invalid payment amount or currency." });
      return;
    }

    // Validate payment status from PesePay
    if (status !== "SUCCESS" && status !== "PAID") {
      logger.warn(`Payment not successful: ${status}`);
      // Fetch property to get landlordId for failed transaction tracking
      const propertyDoc = await db.collection("properties").doc(propertyId).get();
      const landlordId = propertyDoc.exists ? propertyDoc.data()?.landlordId : "";
      // Write failed transaction
      await db.collection("transactions").add({
        tenantId,
        propertyId,
        landlordId,
        amount,
        currency,
        status:           "failed",
        paymentProvider:  "pesepay",
        paymentReference: reference,
        createdAt:        Date.now(),
      });
      res.status(200).json({ success: false, message: "Payment not successful." });
      return;
    }

    // Check if already unlocked — prevent double charging
    const unlockId = `${tenantId}_${propertyId}`;
    const existingUnlock = await db.collection("unlocks").doc(unlockId).get();
    if (existingUnlock.exists) {
      logger.info(`Property ${propertyId} already unlocked by ${tenantId}`);
      res.status(200).json({ success: true, message: "Already unlocked.", alreadyUnlocked: true });
      return;
    }

    // Fetch property to get landlordId
    const propertyDoc = await db.collection("properties").doc(propertyId).get();
    const landlordId = propertyDoc.exists ? propertyDoc.data()?.landlordId : "";

    // Write transaction record
    const transactionRef = await db.collection("transactions").add({
      tenantId,
      propertyId,
      landlordId,
      amount,
      currency,
      status:           "pending",
      paymentProvider:  "pesepay",
      paymentReference: reference,
      createdAt:        Date.now(),
      providerSubtype: propertyDoc.data()?.providerSubtype ?? "",
    });

    const brokerageSettlement = await applyBrokerageUnlockCharge({
      transactionId: transactionRef.id,
      tenantId,
      landlordId,
      propertyId,
      amount,
    });
    if (brokerageSettlement.blocked) {
      await transactionRef.update({ status: "failed" });
      res.status(409).json({ success: false, message: "Brokerage float is below the minimum threshold." });
      return;
    }

    // Write unlock record — this is what the app checks
    await db.collection("unlocks").doc(unlockId).set({
      id:            unlockId,
      tenantId,
      propertyId,
      transactionId: transactionRef.id,
      unlockedAt:    admin.firestore.FieldValue.serverTimestamp(),
    });

    await transactionRef.update({ status: "success" });

    // Notifications — tenant + property owner
    const realPropTitle = propertyDoc.data()?.title ?? "a property";
    const realOwnerId   = landlordId;
    const realOwnerRole = (propertyDoc.data()?.providerSubtype === "brokerage") ? "brokerage"
                        : (propertyDoc.data()?.providerSubtype === "agent")     ? "agent"
                        : "landlord";
    await sendNotification({
      recipientId:   tenantId,
      role:          "tenant",
      type:          "unlock_success",
      title:         "🔑 Contact Details Unlocked!",
      message:       `You can now view the full contact details for "${realPropTitle}". Tap to see landlord info and address.`,
      propertyId,
      propertyTitle: realPropTitle,
    });
    if (realOwnerId) {
      await sendNotification({
        recipientId:   realOwnerId,
        role:          realOwnerRole,
        type:          "property_unlocked",
        title:         "🔓 Property Unlocked by Tenant",
        message:       `A tenant has paid to unlock contact details for "${realPropTitle}".`,
        propertyId,
        propertyTitle: realPropTitle,
      });
    }

    logger.info(`Unlock written: ${unlockId}`);
    res.status(200).json({ success: true, unlockId, transactionId: transactionRef.id });

  } catch (error) {
    logger.error(`PesePay webhook error: ${error}`);
    res.status(500).json({ error: "Internal server error." });
  }
});

// ─── Notification helper ──────────────────────────────────────────────────────
// 1. Writes a Firestore notification document (in-app notification centre)
// 2. Sends an FCM push notification to the recipient's registered device(s)
//    so they receive a system-level alert even when the app is closed.
async function sendNotification(params: {
  recipientId:    string;
  role:           string;
  type:           string;
  title:          string;
  message:        string;
  propertyId?:    string;
  propertyTitle?: string;
}) {
  // 1. Write in-app notification document
  await db.collection("notifications").add({
    recipientId:   params.recipientId,
    role:          params.role,
    type:          params.type,
    title:         params.title,
    message:       params.message,
    propertyId:    params.propertyId   ?? "",
    propertyTitle: params.propertyTitle ?? "",
    isRead:        false,
    createdAt:     Date.now(),
  });

  // 2. Send FCM push to all registered tokens for this user
  try {
    const userDoc = await db.collection("users").doc(params.recipientId).get();
    const userData = userDoc.data() || {};

    // Support both a single token (string) and multiple tokens (array)
    const rawToken = userData.fcmToken;
    const tokens: string[] = Array.isArray(rawToken)
      ? rawToken.filter((t: any) => typeof t === "string" && t.length > 0)
      : typeof rawToken === "string" && rawToken.length > 0
        ? [rawToken]
        : [];

    if (tokens.length === 0) {
      logger.info(`sendNotification: no FCM token for user ${params.recipientId} — skipping push`);
      return;
    }

    // Build FCM message — use data payload so foreground handler also fires
    const fcmPayload = {
      notification: {
        title: params.title,
        body:  params.message,
      },
      data: {
        type:          params.type,
        recipientId:   params.recipientId,
        role:          params.role,
        propertyId:    params.propertyId   ?? "",
        propertyTitle: params.propertyTitle ?? "",
        title:         params.title,
        message:       params.message,
        click_action:  "FLUTTER_NOTIFICATION_CLICK",
      },
      android: {
        notification: {
          channelId: "rentout_default",
          priority:  "high" as const,
          sound:     "default",
        },
        priority: "high" as const,
      },
      apns: {
        payload: {
          aps: {
            sound: "default",
            badge: 1,
          },
        },
      },
    };

    if (tokens.length === 1) {
      await admin.messaging().send({ ...fcmPayload, token: tokens[0] });
      logger.info(`FCM push sent to ${params.recipientId} (${params.type})`);
    } else {
      const response = await admin.messaging().sendEachForMulticast({
        ...fcmPayload,
        tokens,
      });
      logger.info(`FCM multicast to ${params.recipientId}: ${response.successCount} ok, ${response.failureCount} failed`);

      // Prune stale tokens that returned registration-not-found errors
      const staleTokens: string[] = [];
      response.responses.forEach((r, idx) => {
        if (!r.success &&
            (r.error?.code === "messaging/registration-token-not-registered" ||
             r.error?.code === "messaging/invalid-registration-token")) {
          staleTokens.push(tokens[idx]);
        }
      });
      if (staleTokens.length > 0) {
        const cleanedTokens = tokens.filter(t => !staleTokens.includes(t));
        await db.collection("users").doc(params.recipientId).update({
          fcmToken: cleanedTokens.length === 1 ? cleanedTokens[0] : cleanedTokens,
        });
        logger.info(`Pruned ${staleTokens.length} stale FCM token(s) for ${params.recipientId}`);
      }
    }
  } catch (fcmError) {
    // Non-fatal — the Firestore notification was already written
    logger.warn(`FCM push failed for ${params.recipientId}: ${fcmError}`);
  }
}

// ─── 4. onPropertyStatusChange — Notify provider when admin approves/rejects/flags
export const onPropertyStatusChange = onDocumentUpdated(
  "properties/{propertyId}",
  async (event) => {
    const before = event.data?.before.data();
    const after  = event.data?.after.data();
    if (!before || !after) return;

    // Only fire when status actually changed
    if (before.status === after.status) return;

    const propertyId    = event.params.propertyId;
    const propertyTitle = after.title ?? "Your property";
    const recipientId   = after.landlordId ?? "";
    // Determine the role of the listing owner so the badge goes to the right dashboard
    const providerRole  = (after.providerSubtype === "brokerage")
      ? "brokerage"
      : (after.providerSubtype === "agent" ? "agent" : "landlord");

    try {
      if (after.status === "approved") {
        await sendNotification({
          recipientId,
          role:          providerRole,
          type:          "listing_approved",
          title:         "🎉 Listing Approved!",
          message:       `Great news! Your property "${propertyTitle}" has been approved and is now live on RentOut.`,
          propertyId,
          propertyTitle,
        });
      } else if (after.status === "rejected") {
        await sendNotification({
          recipientId,
          role:          providerRole,
          type:          "listing_rejected",
          title:         "❌ Listing Rejected",
          message:       `Your property "${propertyTitle}" was not approved. Please review the admin feedback and resubmit.`,
          propertyId,
          propertyTitle,
        });
      } else if (after.status === "flagged") {
        await sendNotification({
          recipientId,
          role:          providerRole,
          type:          "listing_flagged",
          title:         "🚩 Listing Flagged",
          message:       `Your property "${propertyTitle}" has been flagged for review. Our team will reach out shortly.`,
          propertyId,
          propertyTitle,
        });
      } else if (after.status === "pending" && before.status !== "pending") {
        // Re-submitted listing moved back to pending
        await sendNotification({
          recipientId,
          role:          providerRole,
          type:          "listing_pending",
          title:         "⏳ Listing Under Review",
          message:       `Your property "${propertyTitle}" has been submitted and is under review by the RentOut team.`,
          propertyId,
          propertyTitle,
        });
      }

      logger.info(`Notification sent to ${recipientId} (${providerRole}) for property ${propertyId} — status: ${after.status}`);
    } catch (error) {
      logger.error(`Error sending property status notification: ${error}`);
    }
  }
);

// ─── 5. suspendUser — Admin callable to suspend/reactivate accounts ───────────
export const suspendUser = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Must be logged in.");
  }

  // Verify caller is admin
  const callerDoc = await db.collection("users").doc(request.auth.uid).get();
  if (callerDoc.data()?.role !== "admin") {
    throw new HttpsError("permission-denied", "Only admins can suspend users.");
  }

  const { userId, action } = request.data as { userId: string; action: "suspend" | "reactivate" };
  if (!userId || !action) {
    throw new HttpsError("invalid-argument", "userId and action are required.");
  }

  const newStatus = action === "suspend" ? "suspended" : "active";

  // Update Firestore
  await db.collection("users").doc(userId).update({ status: newStatus });

  // Disable/enable Firebase Auth account
  await admin.auth().updateUser(userId, { disabled: action === "suspend" });

  // Notify the user about their account status change
  try {
    const targetUserDoc = await db.collection("users").doc(userId).get();
    const targetRole    = targetUserDoc.data()?.role ?? "tenant";
    if (action === "suspend") {
      await sendNotification({
        recipientId: userId,
        role:        targetRole,
        type:        "account_suspended",
        title:       "⚠️ Account Suspended",
        message:     "Your RentOut account has been suspended by an administrator. Please contact support for assistance.",
      });
    } else {
      await sendNotification({
        recipientId: userId,
        role:        targetRole,
        type:        "system",
        title:       "✅ Account Reactivated",
        message:     "Your RentOut account has been reactivated. You can now access all features again.",
      });
    }
  } catch (notifError) {
    logger.warn(`Could not send suspension notification to ${userId}: ${notifError}`);
  }

  logger.info(`User ${userId} ${action}ed by admin ${request.auth.uid}`);
  return { success: true, userId, status: newStatus };
});

// ─── 6. confirmPayment — Lightweight callable to check if payment/unlock confirmed ─
// Called by client after initiatePayment when it cannot confirm via Firestore
// directly (e.g. App Check latency). Returns the server-authoritative unlock state.
export const confirmPayment = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Must be logged in.");
  }

  const { propertyId } = request.data as { propertyId: string };
  if (!propertyId) {
    throw new HttpsError("invalid-argument", "propertyId is required.");
  }

  const tenantId = request.auth.uid;
  const unlockId = `${tenantId}_${propertyId}`;

  try {
    // Check unlock doc — single get, not a list query
    const unlockDoc = await db.collection("unlocks").doc(unlockId).get();
    if (unlockDoc.exists) {
      const data = unlockDoc.data();
      logger.info(`confirmPayment: unlock confirmed for ${unlockId}`);
      return {
        confirmed:     true,
        transactionId: data?.transactionId ?? "",
        unlockedAt:    data?.unlockedAt?.toMillis?.() ?? Date.now(),
      };
    }

    // Fall back to checking transactions collection
    const txSnap = await db.collection("transactions")
      .where("tenantId",   "==", tenantId)
      .where("propertyId", "==", propertyId)
      .where("status",     "==", "success")
      .limit(1)
      .get();

    if (!txSnap.empty) {
      logger.info(`confirmPayment: confirmed via transaction for ${unlockId}`);
      return {
        confirmed:     true,
        transactionId: txSnap.docs[0].id,
        unlockedAt:    Date.now(),
      };
    }

    logger.info(`confirmPayment: not yet confirmed for ${unlockId}`);
    return { confirmed: false };
  } catch (error) {
    logger.error(`confirmPayment error: ${error}`);
    throw new HttpsError("internal", "Failed to check payment confirmation.");
  }
});

// ─── 7. initiateBrokerageTopUp — Start brokerage top-up in demo or live mode ─
export const initiateBrokerageTopUp = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Must be logged in.");
  }
  const authUid = request.auth.uid;
  const amountUsd = roundMoney(Number(request.data?.amountUsd ?? 0));
  if (!Number.isFinite(amountUsd) || amountUsd <= 0) {
    throw new HttpsError("invalid-argument", "A valid top-up amount is required.");
  }

  const userSnap = await db.collection("users").doc(authUid).get();
  const userData = userSnap.data() || {};
  if (userData.providerSubtype !== "brokerage") {
    throw new HttpsError("permission-denied", "Only brokerage accounts can top up float.");
  }

  const mode = resolvePaymentMode();
  const requestRef = db.collection(BROKERAGE_TOPUP_COLLECTION).doc();
  const paymentReference = `TOPUP-${Date.now()}-${authUid.substring(0, 6)}`;
  const createdAt = Date.now();

  // ── DEMO MODE: apply top-up immediately, no payment simulator needed ────────
  // This mirrors how initiatePayment handles demo unlocks — no redirect, instant credit.
  if (mode === "demo") {
    logger.info(`Demo brokerage top-up: uid=${authUid}, amount=${amountUsd}`);
    try {
      // Write a pending request doc first (for audit trail)
      await requestRef.set({
        id:               requestRef.id,
        brokerageId:      authUid,
        amountUsd,
        currency:         "USD",
        paymentReference,
        paymentProvider:  "demo",
        status:           "pending",
        createdAt,
        performedBy:      authUid,
        message:          "Demo top-up — auto-credited.",
      });

      // Immediately apply the credit
      const result = await applyBrokerageTopUpCredit({
        requestId:        requestRef.id,
        brokerageId:      authUid,
        amountUsd,
        paymentProvider:  "demo",
        paymentReference,
        performedBy:      authUid,
      });

      // Send notification to the brokerage user
      await sendNotification({
        recipientId:  authUid,
        role:         "brokerage",
        type:         "payment_confirmed",
        title:        "💳 Float Topped Up!",
        message:      `Your brokerage insurance float has been credited by $${amountUsd.toFixed(2)}. New balance: $${result.nextBalance.toFixed(2)}.`,
      });

      logger.info(`Demo brokerage top-up applied: uid=${authUid}, amount=${amountUsd}, newBalance=${result.nextBalance}`);
      return {
        success:       true,
        requestId:     requestRef.id,
        paymentMode:   "demo",
        newBalance:    result.nextBalance,
        amountCredited: amountUsd,
        message:       `Float topped up by $${amountUsd.toFixed(2)}. New balance: $${result.nextBalance.toFixed(2)}.`,
        // No checkoutUrl — demo completes immediately
        checkoutUrl:   "",
      };
    } catch (error) {
      logger.error(`Demo brokerage top-up failed: ${error}`);
      throw new HttpsError("internal", `Top-up failed: ${error instanceof Error ? error.message : error}`);
    }
  }
  const companyName = userData.companyName || userData.name || "Brokerage";

  await requestRef.set({
    id: requestRef.id,
    brokerageId: authUid,
    amountUsd,
    currency: "USD",
    status: "pending",
    paymentProvider: "pesepay",
    paymentReference,
    paymentMode: mode,
    checkoutUrl: "",
    createdAt,
    completedAt: 0,
    ledgerEntryId: "",
    message: "Awaiting live PesePay confirmation.",
  });

  // Demo mode was already handled above (with an early return).
  // We only reach this point in live/PesePay mode.
  const pesepayIntegrationKey      = pesepayKey.value()      || process.env.PESEPAY_INTEGRATION_KEY      || "";
  const pesepayIntegrationPassword = pesepayPassword.value() || process.env.PESEPAY_INTEGRATION_PASSWORD || "";
  const payload = {
    integrationKey:      pesepayIntegrationKey,
    integrationPassword: pesepayIntegrationPassword,
    amount:              amountUsd,
    currencyCode:        "USD",
    reference:           requestRef.id,
    productDescription:  `Brokerage float top-up for ${companyName}`,
    customerEmail:       request.auth.token?.email || "",
    customerPhone:       userData.companyPhone || userData.phoneNumber || "",
    customerName:        companyName,
    returnUrl:           request.data?.successUrl || "rentout://payment-success",
    cancelUrl:           request.data?.cancelUrl || "rentout://payment-cancel",
    notificationUrl:     `https://us-central1-rentout-12239.cloudfunctions.net/verifyBrokerageTopUp`,
    paymentMethod:       "",
  };
  const response = await fetch("https://www.pesepay.com/api/payments/initialize", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const errorText = await response.text();
    await requestRef.update({ status: "failed", message: `PesePay init failed: ${errorText}` });
    throw new HttpsError("internal", "Failed to initialize PesePay top-up.");
  }
  const data: any = await response.json();
  const checkoutUrl = data.redirectUrl || data.checkoutUrl;
  if (!checkoutUrl) {
    await requestRef.update({ status: "failed", message: "Missing checkout URL from PesePay." });
    throw new HttpsError("internal", "PesePay did not return a checkout URL.");
  }
  await requestRef.update({ checkoutUrl });
  return {
    success: true,
    requestId: requestRef.id,
    checkoutUrl,
    demoMode: false,
    paymentMode: mode,
    message: "Live top-up checkout initialized.",
  };
});

export const completeBrokerageTopUp = onRequest(async (req, res) => {
  try {
    const requestId = String(req.query.requestId || req.body?.requestId || "");
    const status = String(req.query.status || req.body?.status || "SUCCESS").toUpperCase();
    if (!requestId) {
      res.status(400).json({ success: false, message: "requestId is required." });
      return;
    }
    const requestRef = db.collection(BROKERAGE_TOPUP_COLLECTION).doc(requestId);
    const requestSnap = await requestRef.get();
    if (!requestSnap.exists) {
      res.status(404).json({ success: false, message: "Top-up request not found." });
      return;
    }
    const requestData = requestSnap.data() || {};
    if (requestData.paymentMode !== "demo") {
      res.status(400).json({ success: false, message: "Only demo requests can be completed from the simulator." });
      return;
    }
    if (status !== "SUCCESS" && status !== "PAID") {
      await requestRef.set({ status: "failed", completedAt: Date.now(), message: "Demo simulator marked this payment as failed." }, { merge: true });
      res.status(200).json({ success: true, message: "Demo top-up marked as failed." });
      return;
    }
    const result = await applyBrokerageTopUpCredit({
      requestId,
      brokerageId: requestData.brokerageId,
      amountUsd: Number(requestData.amountUsd || 0),
      paymentProvider: "pesepay_demo",
      paymentReference: requestData.paymentReference || requestId,
      performedBy: "demo_simulator",
    });
    res.status(200).json({ success: true, message: "Demo top-up completed successfully.", brokerageFloatBalanceUsd: result.nextBalance });
  } catch (error: any) {
    logger.error(`completeBrokerageTopUp error: ${error}`);
    res.status(500).json({ success: false, message: error?.message || "Internal server error." });
  }
});

export const verifyBrokerageTopUp = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
  }
  try {
    const reference = String(req.body?.reference || "");
    const status = String(req.body?.status || "").toUpperCase();
    if (!reference) {
      res.status(400).json({ success: false, message: "reference is required." });
      return;
    }
    const requestRef = db.collection(BROKERAGE_TOPUP_COLLECTION).doc(reference);
    const requestSnap = await requestRef.get();
    if (!requestSnap.exists) {
      res.status(404).json({ success: false, message: "Top-up request not found." });
      return;
    }
    const requestData = requestSnap.data() || {};
    if (status !== "SUCCESS" && status !== "PAID") {
      await requestRef.set({ status: "failed", completedAt: Date.now(), message: `Live gateway marked payment as ${status || "failed"}.` }, { merge: true });
      res.status(200).json({ success: false, message: "Top-up payment not successful." });
      return;
    }
    const result = await applyBrokerageTopUpCredit({
      requestId: reference,
      brokerageId: requestData.brokerageId,
      amountUsd: Number(requestData.amountUsd || 0),
      paymentProvider: "pesepay",
      paymentReference: requestData.paymentReference || reference,
      performedBy: "pesepay_webhook",
    });
    res.status(200).json({ success: true, message: "Brokerage top-up verified.", brokerageFloatBalanceUsd: result.nextBalance });
  } catch (error: any) {
    logger.error(`verifyBrokerageTopUp error: ${error}`);
    res.status(500).json({ success: false, message: error?.message || "Internal server error." });
  }
});

// ─── 7. topUpBrokerageFloat — Brokerage callable to increase float ───────────
export const topUpBrokerageFloat = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Must be logged in.");
  }
  const authUid = request.auth.uid;

  const amountUsd = Number(request.data?.amountUsd ?? 0);
  if (!Number.isFinite(amountUsd) || amountUsd <= 0) {
    throw new HttpsError("invalid-argument", "A valid top-up amount is required.");
  }

  const userRef = db.collection("users").doc(authUid);
  const result = await db.runTransaction(async (tx) => {
    const userSnap = await tx.get(userRef);
    const userData = userSnap.data() || {};

    if (userData.providerSubtype !== "brokerage") {
      throw new HttpsError("permission-denied", "Only brokerage accounts can top up float.");
    }

    const currentBalance = roundMoney(userData.brokerageFloatBalanceUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD);
    const minimumFloat = roundMoney(userData.brokerageMinimumFloatUsd ?? BROKERAGE_MINIMUM_FLOAT_USD);
    const nextBalance = roundMoney(currentBalance + amountUsd);
    const now = Date.now();
    const ledgerRef = db.collection(BROKERAGE_LEDGER_COLLECTION).doc();

    tx.set(ledgerRef, {
      id: ledgerRef.id,
      brokerageId: authUid,
      type: "top_up",
      direction: "credit",
      amount: roundMoney(amountUsd),
      currency: "USD",
      balanceBefore: currentBalance,
      balanceAfter: nextBalance,
      createdAt: now,
      relatedTransactionId: "",
      propertyId: "",
      tenantId: "",
      title: "Brokerage float top-up",
      description: `Brokerage topped up insurance float by $${roundMoney(amountUsd).toFixed(2)}`,
      status: "success",
      performedBy: authUid,
    });

    tx.set(userRef, {
      brokerageSubscriptionFeeUsd: userData.brokerageSubscriptionFeeUsd ?? BROKERAGE_SUBSCRIPTION_FEE_USD,
      brokerageFloatBalanceUsd: nextBalance,
      brokerageMinimumFloatUsd: minimumFloat,
      brokerageCommissionRate: userData.brokerageCommissionRate ?? BROKERAGE_COMMISSION_RATE,
      brokerageIsFrozen: nextBalance < minimumFloat,
      brokerageLastTopUpAt: now,
    }, { merge: true });

    return { nextBalance, minimumFloat };
  });

  await syncBrokeragePropertyUnlockState(authUid, result.nextBalance < result.minimumFloat, result.nextBalance, result.minimumFloat);
  return {
    success: true,
    message: "Brokerage float updated successfully.",
    brokerageFloatBalanceUsd: result.nextBalance,
  };
});

// ─── 8. getAdminStats — Admin dashboard stats ─────────────────────────────────
export const getAdminStats = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Must be logged in.");
  }

  const callerDoc = await db.collection("users").doc(request.auth.uid).get();
  if (callerDoc.data()?.role !== "admin") {
    throw new HttpsError("permission-denied", "Only admins can access stats.");
  }

  const [propertiesSnap, usersSnap, transactionsSnap] = await Promise.all([
    db.collection("properties").get(),
    db.collection("users").get(),
    db.collection("transactions").where("status", "==", "success").get(),
  ]);

  const properties  = propertiesSnap.docs.map(d => d.data());
  const revenue     = transactionsSnap.docs.reduce((sum, d) => sum + (d.data().amount ?? 0), 0);

  return {
    totalProperties:    properties.length,
    pendingProperties:  properties.filter(p => p.status === "pending").length,
    approvedProperties: properties.filter(p => p.status === "approved").length,
    rejectedProperties: properties.filter(p => p.status === "rejected").length,
    totalUsers:         usersSnap.size,
    totalRevenue:       revenue,
    totalUnlocks:       transactionsSnap.size,
  };
});
