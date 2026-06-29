/**
 * Flujo donación por transferencia:
 * 1. App escribe donation_requests/{id} (donante avisa).
 * 2. onDonationRequest → email al admin con enlace firmado.
 * 3. Admin hace clic → adminEnableDonation habilita donation_grants + users.
 *
 * Config (una vez):
 *   firebase functions:config:set \
 *     enable.secret="CAMBIAR_POR_SECRETO_LARGO" \
 *     admin.email="tu@gmail.com" \
 *     gmail.user="tu@gmail.com" \
 *     gmail.pass="CONTRASEÑA_DE_APLICACION_GMAIL"
 *
 * Deploy: firebase deploy --only functions
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");
const nodemailer = require("nodemailer");

admin.initializeApp();
const db = admin.firestore();

function cfg() {
  const c = functions.config();
  return {
    secret: c.enable?.secret || process.env.ENABLE_SECRET || "",
    adminEmail: c.admin?.email || process.env.ADMIN_EMAIL || "",
    gmailUser: c.gmail?.user || process.env.GMAIL_USER || "",
    gmailPass: c.gmail?.pass || process.env.GMAIL_PASS || "",
  };
}

function normalizeEmailDocId(email) {
  return email.trim().toLowerCase().replace("@", "_at_").replace(/\./g, "_dot_");
}

function amountFromTierCode(code) {
  const digits = code.toUpperCase().replace("CAFE", "").replace(/\D/g, "");
  const n = parseInt(digits, 10);
  return Number.isFinite(n) && n > 0 ? n : 0;
}

function createEnableToken(donorEmail, tierCode, requestId) {
  const { secret } = cfg();
  if (!secret) throw new Error("Falta enable.secret en functions config");
  const exp = Date.now() + 7 * 24 * 60 * 60 * 1000;
  const payload = JSON.stringify({
    donorEmail: donorEmail.trim().toLowerCase(),
    tierCode: tierCode.trim().toUpperCase(),
    requestId,
    exp,
  });
  const payloadB64 = Buffer.from(payload).toString("base64url");
  const sig = crypto.createHmac("sha256", secret).update(payloadB64).digest("base64url");
  return `${payloadB64}.${sig}`;
}

function verifyEnableToken(token) {
  const { secret } = cfg();
  if (!secret) throw new Error("Falta enable.secret");
  const parts = String(token || "").split(".");
  if (parts.length !== 2) throw new Error("Token mal formado");
  const [payloadB64, sig] = parts;
  const expected = crypto.createHmac("sha256", secret).update(payloadB64).digest("base64url");
  if (sig !== expected) throw new Error("Token inválido o alterado");
  const payload = JSON.parse(Buffer.from(payloadB64, "base64url").toString("utf8"));
  if (Date.now() > payload.exp) throw new Error("Enlace expirado (máx. 7 días)");
  return payload;
}

async function resolveAdminEmails() {
  const emails = new Set();
  const { adminEmail } = cfg();
  if (adminEmail && adminEmail.includes("@")) emails.add(adminEmail.trim().toLowerCase());
  try {
    const snap = await db.collection("app_config").doc("donation_admins").get();
    const list = snap.get("emails") || [];
    for (const e of list) {
      if (typeof e === "string" && e.includes("@")) emails.add(e.trim().toLowerCase());
    }
  } catch (_) {
    /* ignore */
  }
  return [...emails];
}

async function enableDonorInFirestore(donorEmail, tierCode, enabledBy) {
  const code = tierCode.toUpperCase();
  const tierSnap = await db.collection("donation_codes").doc(code).get();
  if (!tierSnap.exists) throw new Error(`No existe donation_codes/${code}`);
  if (tierSnap.get("active") === false) throw new Error(`${code} no está activo`);
  const amount =
    tierSnap.get("amountPesos") || amountFromTierCode(code);
  if (!amount) throw new Error(`No se pudo leer monto de ${code}`);

  const now = Date.now();
  const grantId = normalizeEmailDocId(donorEmail);
  await db
    .collection("donation_grants")
    .doc(grantId)
    .set(
      {
        email: donorEmail,
        enabled: true,
        tierCode: code,
        amountPesos: amount,
        enabledBy: enabledBy || "email_link",
        enabledAt: now,
        redeemedAt: admin.firestore.FieldValue.delete(),
      },
      { merge: true },
    );

  const users = await db
    .collection("users")
    .where("email", "==", donorEmail)
    .limit(1)
    .get();
  if (!users.empty) {
    await users.docs[0].ref.update({
      transferDonationEnabled: true,
      transferTierCode: code,
      transferDonationRedeemedAt: admin.firestore.FieldValue.delete(),
    });
  }
  return { amount, code };
}

function htmlPage(title, body) {
  return `<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title}</title></head><body style="font-family:sans-serif;padding:24px;max-width:480px;margin:auto"><h1>${title}</h1><p>${body}</p></body></html>`;
}

async function sendAdminEnableEmail(request, enableUrl) {
  const { gmailUser, gmailPass } = cfg();
  const admins = await resolveAdminEmails();
  if (!admins.length) throw new Error("Sin admin.email ni app_config/donation_admins");
  if (!gmailUser || !gmailPass) {
    throw new Error("Configurá gmail.user y gmail.pass (contraseña de aplicación Gmail)");
  }

  const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: { user: gmailUser, pass: gmailPass },
  });

  const donor = request.donorEmail || "?";
  const tier = request.tierCode || "?";
  const amount = request.amountPesos || amountFromTierCode(tier);

  const subject = `☕ Donación AntiGastos: ${donor} — ${tier}`;
  const html = `
    <p>El usuario <strong>${donor}</strong> avisó transferencia por <strong>${tier}</strong> (~$${amount} ARS).</p>
    <p>Si ya ves el pago en Mercado Pago, habilitá sin publicidad con un clic:</p>
    <p style="margin:24px 0">
      <a href="${enableUrl}" style="background:#1b5e20;color:#fff;padding:14px 22px;text-decoration:none;border-radius:8px;font-weight:bold;display:inline-block">
        ✓ Habilitar sin publicidad
      </a>
    </p>
    <p style="color:#666;font-size:13px">El enlace expira en 7 días y solo funciona una vez. Si no fuiste vos, ignorá este mail.</p>
  `;

  await transporter.sendMail({
    from: `"AntiGastos Donaciones" <${gmailUser}>`,
    to: admins.join(","),
    subject,
    html,
  });
}

/** App: donante crea solicitud → mail al admin con botón. */
exports.onDonationRequest = functions.firestore
  .document("donation_requests/{requestId}")
  .onCreate(async (snap, context) => {
    const data = snap.data() || {};
    if (data.status && data.status !== "pending") return null;

    const donorEmail = (data.donorEmail || "").trim().toLowerCase();
    const tierCode = (data.tierCode || "").trim().toUpperCase();
    if (!donorEmail.includes("@") || !tierCode.startsWith("CAFE")) {
      console.warn("donation_requests inválido", data);
      return null;
    }

    const token = createEnableToken(donorEmail, tierCode, context.params.requestId);
    const projectId = process.env.GCLOUD_PROJECT || admin.app().options.projectId;
    const region = process.env.FUNCTION_REGION || "us-central1";
    const enableUrl = `https://${region}-${projectId}.cloudfunctions.net/adminEnableDonation?token=${encodeURIComponent(token)}`;

    try {
      await sendAdminEnableEmail(data, enableUrl);
      await snap.ref.update({
        notifyEmailSent: true,
        enableUrlPreview: enableUrl,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (err) {
      console.error("onDonationRequest email failed", err);
      await snap.ref.update({
        notifyEmailError: String(err.message || err),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
    return null;
  });

/** Un clic en el mail → habilita al donante en Firestore. */
exports.adminEnableDonation = functions.https.onRequest(async (req, res) => {
  if (req.method !== "GET") {
    res.status(405).send("Usá GET desde el enlace del correo.");
    return;
  }
  try {
    const token = req.query.token;
    const payload = verifyEnableToken(token);
    const tokenHash = crypto.createHash("sha256").update(String(token)).digest("hex");
    const usedRef = db.collection("used_enable_tokens").doc(tokenHash);
    if ((await usedRef.get()).exists) {
      res.status(200).send(htmlPage("Ya procesado", "Este enlace ya se usó. El donante puede verificar en la app."));
      return;
    }

    const { amount, code } = await enableDonorInFirestore(
      payload.donorEmail,
      payload.tierCode,
      "email_link",
    );

    await usedRef.set({
      usedAt: Date.now(),
      donorEmail: payload.donorEmail,
      tierCode: code,
    });

    if (payload.requestId) {
      await db.collection("donation_requests").doc(payload.requestId).update({
        status: "enabled",
        enabledAt: Date.now(),
        enabledBy: "email_link",
      });
    }

    res
      .status(200)
      .send(
        htmlPage(
          "Donación habilitada",
          `Listo: <strong>${payload.donorEmail}</strong> con <strong>${code}</strong> ($${amount} ARS).<br><br>Que abra la app con ese Gmail y toque «Verificar mi transferencia» en Donar.`,
        ),
      );
  } catch (err) {
    console.error("adminEnableDonation", err);
    res.status(400).send(htmlPage("No se pudo habilitar", String(err.message || err)));
  }
});
