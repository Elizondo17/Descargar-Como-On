const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-remixcom-1bf500c5-f4d0-45d9-bd3c-8e21955d8e02";

exports.dispatchFcmFromQueue = onDocumentCreated(
  {
    document: "fcm_dispatch_queue/{dispatchId}",
    database: DATABASE_ID,
    region: "us-central1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const data = snap.data();
    if (!data || data.status !== "QUEUED") return;

    const androidChannelId =
      data.category === "CATALOG_UPDATE"
        ? "psa_catalog_updates"
        : data.category === "ASSIGNED_CONTACT"
          ? "psa_assigned_contacts"
          : "como_on_team_announcements";

    const payload = {
      notification: {
        title: String(data.title || "Aviso Como On"),
        body: String(data.body || ""),
      },
      data: {
        id: String(data.id || event.params.dispatchId),
        category: String(data.category || "SUPERUSER_GLOBAL"),
        priority: String(data.priority || "IMPORTANT"),
        title: String(data.title || ""),
        body: String(data.body || ""),
        sender: String(data.senderName || "Superusuario"),
        actionUrl: String(data.actionUrl || ""),
      },
      android: {
        priority: "high",
        notification: {
          channelId: androidChannelId,
          clickAction: "OPEN_MAIN_BANNER",
          sticky: true,
        },
      },
    };

    try {
      if (data.targetMode === "TOKEN" && data.targetToken) {
        await admin.messaging().send({ ...payload, token: data.targetToken });
      } else if (data.targetMode === "TOPIC" && data.targetTopic) {
        await admin.messaging().send({ ...payload, topic: data.targetTopic });
      } else {
        throw new Error("Destino FCM inválido en documento de cola.");
      }

      await snap.ref.update({
        status: "SENT",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (err) {
      console.error("Error enviando notificación FCM:", err);
      await snap.ref.update({
        status: "FAILED",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
  }
);
