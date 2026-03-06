import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { onRequest } from "firebase-functions/v2/https";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineString } from "firebase-functions/params";

// Environment variables for PesePay
const pesepayKey      = defineString("PESEPAY_INTEGRATION_KEY",      { default: "" });
const pesepayPassword = defineString("PESEPAY_INTEGRATION_PASSWORD", { default: "" });

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();

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
    }, { merge: true });
    logger.info(`User defaults set for ${event.params.uid}`);
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

  // Idempotency — already unlocked
  const existingUnlock = await db.collection("unlocks").doc(unlockId).get();
  if (existingUnlock.exists) {
    return { success: true, alreadyUnlocked: true, unlockId };
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
  });

  // Resolve credentials — prefer env params, fall back to process.env
  const pesepayIntegrationKey      = pesepayKey.value()      || process.env.PESEPAY_INTEGRATION_KEY      || "demo";
  const pesepayIntegrationPassword = pesepayPassword.value() || process.env.PESEPAY_INTEGRATION_PASSWORD || "demo";

  const isDemoMode =
    pesepayIntegrationKey      === "demo" ||
    pesepayIntegrationKey      === "demo_key_for_testing" ||
    pesepayIntegrationPassword === "demo_password_for_testing";

  try {
    let checkoutUrl: string;

    if (isDemoMode) {
      // ── Demo / test mode ────────────────────────────────────────────────────
      // Auto-create the unlock doc immediately so the app can confirm it
      // without any external redirect or webhook.
      logger.info(`Demo payment initiated: transaction=${transactionRef.id}`);

      await db.collection("unlocks").doc(unlockId).set({
        id:            unlockId,
        tenantId,
        propertyId,
        transactionId: transactionRef.id,
        unlockedAt:    admin.firestore.FieldValue.serverTimestamp(),
        isDemo:        true,
      });

      await transactionRef.update({ status: "success" });

      await db.collection("notifications").add({
        userId:    tenantId,
        title:     "🔑 Contact Unlocked! (Demo)",
        body:      "You can now view the landlord's contact details. This is a test unlock.",
        propertyId,
        type:      "unlock_success",
        read:      false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      checkoutUrl = `https://rentout-12239.web.app/payment-simulator?transactionId=${transactionRef.id}&tenantId=${tenantId}&propertyId=${propertyId}`;
      logger.info(`Demo unlock auto-created: ${unlockId}`);
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
      status:           "success",
      paymentProvider:  "pesepay_mock",
      paymentReference: reference,
      createdAt:        Date.now(),
    });

    // Write unlock record — this is what the app checks
    await db.collection("unlocks").doc(unlockId).set({
      id:            unlockId,
      tenantId,
      propertyId,
      transactionId: transactionRef.id,
      unlockedAt:    admin.firestore.FieldValue.serverTimestamp(),
    });

    // Send notification to tenant
    await db.collection("notifications").add({
      userId:    tenantId,
      title:     "🔑 Contact Unlocked! (TEST)",
      body:      "You can now view the landlord's contact details. (Mock Payment)",
      propertyId,
      type:      "unlock_success",
      read:      false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

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
      status:           "success",
      paymentProvider:  "pesepay",
      paymentReference: reference,
      createdAt:        Date.now(),
    });

    // Write unlock record — this is what the app checks
    await db.collection("unlocks").doc(unlockId).set({
      id:            unlockId,
      tenantId,
      propertyId,
      transactionId: transactionRef.id,
      unlockedAt:    admin.firestore.FieldValue.serverTimestamp(),
    });

    // Send notification to tenant
    await db.collection("notifications").add({
      userId:    tenantId,
      title:     "🔑 Contact Unlocked!",
      body:      "You can now view the landlord's contact details.",
      propertyId,
      type:      "unlock_success",
      read:      false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    logger.info(`Unlock written: ${unlockId}`);
    res.status(200).json({ success: true, unlockId, transactionId: transactionRef.id });

  } catch (error) {
    logger.error(`PesePay webhook error: ${error}`);
    res.status(500).json({ error: "Internal server error." });
  }
});

// ─── 4. onPropertyStatusChange — Notify landlord when admin approves/rejects ──
export const onPropertyStatusChange = onDocumentUpdated(
  "properties/{propertyId}",
  async (event) => {
    const before = event.data?.before.data();
    const after  = event.data?.after.data();
    if (!before || !after) return;

    // Only fire when status actually changed
    if (before.status === after.status) return;

    // Only notify on status change to approved or rejected
    if (!["approved", "rejected"].includes(after.status)) return;

    try {
      const message = after.status === "approved"
        ? `✅ Your property "${after.title}" has been approved and is now live!`
        : `❌ Your property "${after.title}" was rejected. Please review and resubmit.`;

      await db.collection("notifications").add({
        userId:     after.landlordId,
        title:      after.status === "approved" ? "Property Approved! 🎉" : "Property Rejected",
        body:       message,
        propertyId: event.params.propertyId,
        type:       `property_${after.status}`,
        read:       false,
        createdAt:  admin.firestore.FieldValue.serverTimestamp(),
      });

      logger.info(`Notification sent to landlord ${after.landlordId} for property ${event.params.propertyId}`);
    } catch (error) {
      logger.error(`Error sending property notification: ${error}`);
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

  logger.info(`User ${userId} ${action}ed by admin ${request.auth.uid}`);
  return { success: true, userId, status: newStatus };
});

// ─── 6. getAdminStats — Admin dashboard stats ─────────────────────────────────
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
