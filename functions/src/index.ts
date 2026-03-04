import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { onRequest } from "firebase-functions/v2/https";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";

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

// ─── 3. verifyPesePay — Webhook from PesePay after $10 payment ───────────────
export const verifyPesePay = onRequest(async (req, res) => {
  // Only accept POST requests from PesePay
  if (req.method !== "POST") {
    res.status(405).send("Method Not Allowed");
    return;
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
      // Write failed transaction
      await db.collection("transactions").add({
        tenantId,
        propertyId,
        amount,
        currency,
        status:           "failed",
        paymentProvider:  "pesepay",
        paymentReference: reference,
        createdAt:        admin.firestore.FieldValue.serverTimestamp(),
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

    // Write transaction record
    const transactionRef = await db.collection("transactions").add({
      tenantId,
      propertyId,
      amount,
      currency,
      status:           "success",
      paymentProvider:  "pesepay",
      paymentReference: reference,
      createdAt:        admin.firestore.FieldValue.serverTimestamp(),
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
