/**
 * IndSphinx Cloud Functions
 *
 * Trigger: onOccupantCreated
 *   Fires when a new document is added to the "Occupants" Firestore collection.
 *
 *   Actions:
 *     1. Generate a password  →  first4(Name) + EMPID + 3 random digits (all lowercase, alphanumeric)
 *     2. Create a Firebase Auth account for the occupant
 *     3. Create a Users/{uid} document  →  { Name, Email, Role: "OCCUPANT", Enabled: true }
 *     4. Stamp authUid back onto the Occupants/{id} document (idempotency guard)
 *     5. Send a welcome e-mail with the login credentials via Gmail SMTP
 *
 * Secrets (set via `firebase functions:secrets:set <KEY>`):
 *   SMTP_USER  —  Gmail address used to send emails
 *   SMTP_PASS  —  Gmail App Password
 *
 * Deploy:
 *   cd IndSphinx  (project root with firebase.json)
 *   firebase deploy --only functions
 */

"use strict";

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

// Explicit ADC helps Cloud Functions / Cloud Run resolve OAuth for FCM HTTP v1
// (avoids intermittent "missing required authentication credential" on messaging.send).
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
  });
}

// ─── Secrets (stored in Google Secret Manager) ────────────────────────────────
const SMTP_USER = defineSecret("SMTP_USER");
const SMTP_PASS = defineSecret("SMTP_PASS");

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Derives an auto-generated password:
 *   first 4 alphanumeric characters of Name (lowercase)
 *   + EMPID stripped of non-alphanumeric (lowercase)
 *   + 3 random digits (100–999)
 *
 * Example: Name="John Doe", EMPID="EMP-001"  →  "johnemp001473"
 */
function generatePassword(name, empId) {
  const namePart = String(name)
    .replace(/[^a-zA-Z0-9]/g, "")
    .toLowerCase()
    .slice(0, 4)
    .padEnd(4, "x"); // ensure at least 4 chars even for short names

  const empPart = String(empId)
    .replace(/[^a-zA-Z0-9]/g, "")
    .toLowerCase();

  const digits = String(Math.floor(Math.random() * 900) + 100); // 100–999

  return `${namePart}${empPart}${digits}`;
}

/** Builds the HTML body of the welcome email. */
function buildWelcomeEmail({ name, email, password, appUrl }) {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Welcome to IndSphinx</title>
</head>
<body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f6fb;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
          style="background:#ffffff;border-radius:16px;overflow:hidden;
                 box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:560px;width:100%;">

          <!-- Header -->
          <tr>
            <td style="background:#1e2a6e;padding:32px 40px 28px;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;
                         letter-spacing:-0.3px;">
                IndSphinx
              </h1>
              <p style="margin:4px 0 0;color:#a8b4e8;font-size:13px;">
                Accommodation Management System
              </p>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="padding:36px 40px 28px;">
              <p style="margin:0 0 8px;font-size:18px;font-weight:700;color:#1a1f36;">
                Welcome, ${escapeHtml(name)}!
              </p>
              <p style="margin:0 0 24px;font-size:14px;color:#6b7280;line-height:1.6;">
                Your occupant account has been created. You can now sign in to the
                IndSphinx portal using the credentials below.
              </p>

              <!-- Credentials box -->
              <table width="100%" cellpadding="0" cellspacing="0"
                style="background:#f8f9ff;border:1px solid #e0e4f8;border-radius:12px;
                       margin-bottom:24px;">
                <tr>
                  <td style="padding:24px 28px;">
                    <p style="margin:0 0 16px;font-size:11px;font-weight:700;
                               color:#6b7280;letter-spacing:0.08em;text-transform:uppercase;">
                      Your login credentials
                    </p>

                    <table width="100%" cellpadding="0" cellspacing="0">
                      <tr>
                        <td style="padding-bottom:12px;">
                          <p style="margin:0;font-size:12px;color:#9ca3af;font-weight:600;">
                            EMAIL
                          </p>
                          <p style="margin:3px 0 0;font-size:15px;color:#1a1f36;font-weight:600;">
                            ${escapeHtml(email)}
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding-top:4px;">
                          <p style="margin:0;font-size:12px;color:#9ca3af;font-weight:600;">
                            PASSWORD
                          </p>
                          <p style="margin:3px 0 0;font-size:15px;color:#1a1f36;font-weight:700;
                                     font-family:monospace;letter-spacing:0.05em;
                                     background:#eef0fc;display:inline-block;
                                     padding:4px 10px;border-radius:6px;">
                            ${escapeHtml(password)}
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>

              <!-- CTA -->
              <table cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                <tr>
                  <td style="border-radius:10px;overflow:hidden;">
                    <a href="${appUrl}"
                      style="display:inline-block;background:#1e2a6e;color:#ffffff;
                             text-decoration:none;font-size:14px;font-weight:700;
                             padding:12px 28px;border-radius:10px;">
                      Sign In to IndSphinx →
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.6;">
                For security, please change your password after your first sign-in.
                If you did not expect this email, please contact your administrator.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background:#f8f9ff;border-top:1px solid #e9edf7;
                       padding:20px 40px;text-align:center;">
              <p style="margin:0;font-size:12px;color:#9ca3af;">
                This is an automated message from IndSphinx Accommodation Management.
                Please do not reply to this email.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

/** Minimal HTML escaping to prevent XSS in the email template. */
function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/** Builds an HTML email notifying a worker that a complaint has been assigned to them. */
function buildWorkerAssignedEmail({ workerName, problem, category, flatNumber, occupantName, appUrl }) {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>New Complaint Assigned</title>
</head>
<body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f6fb;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
          style="background:#ffffff;border-radius:16px;overflow:hidden;
                 box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:560px;width:100%;">
          <tr>
            <td style="background:#1e2a6e;padding:32px 40px 28px;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.3px;">
                IndSphinx
              </h1>
              <p style="margin:4px 0 0;color:#a8b4e8;font-size:13px;">
                New Complaint Assignment
              </p>
            </td>
          </tr>

          <tr>
            <td style="padding:32px 40px 10px;">
              <p style="margin:0 0 10px;font-size:16px;font-weight:700;color:#1a1f36;">
                Hi ${escapeHtml(workerName || "there")},
              </p>
              <p style="margin:0 0 18px;font-size:14px;color:#6b7280;line-height:1.6;">
                A new complaint has been assigned to you. Please review the details below
                and attend to it at the earliest.
              </p>

              <table width="100%" cellpadding="0" cellspacing="0"
                style="background:#f8f9ff;border:1px solid #e0e4f8;border-radius:12px;margin-bottom:18px;">
                <tr>
                  <td style="padding:18px 20px;">
                    <p style="margin:0 0 10px;font-size:11px;font-weight:700;color:#6b7280;
                               letter-spacing:0.08em;text-transform:uppercase;">
                      Complaint details
                    </p>
                    <p style="margin:0 0 6px;font-size:14px;color:#1a1f36;font-weight:700;">
                      ${escapeHtml(problem || "Complaint")}
                    </p>
                    <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.6;">
                      ${category    ? `Category: ${escapeHtml(category)}<br/>` : ""}
                      ${flatNumber  ? `Flat: ${escapeHtml(flatNumber)}<br/>`   : ""}
                      ${occupantName ? `Resident: ${escapeHtml(occupantName)}` : ""}
                    </p>
                  </td>
                </tr>
              </table>

              <table cellpadding="0" cellspacing="0" style="margin-bottom:22px;">
                <tr>
                  <td style="border-radius:10px;overflow:hidden;">
                    <a href="${appUrl}"
                      style="display:inline-block;background:#1e2a6e;color:#ffffff;
                             text-decoration:none;font-size:14px;font-weight:700;
                             padding:12px 22px;border-radius:10px;">
                      Open IndSphinx →
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                This is an automated message. Please do not reply.
              </p>
            </td>
          </tr>

          <tr>
            <td style="background:#f8f9ff;border-top:1px solid #e9edf7;
                       padding:18px 40px;text-align:center;">
              <p style="margin:0;font-size:12px;color:#9ca3af;">
                IndSphinx Accommodation Management
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

/** Builds HTML email body for a complaint closed notification. */
function buildComplaintClosedEmail({ name, complaintTitle, category, flatNumber, closedDate, appUrl }) {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Complaint Closed</title>
</head>
<body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f6fb;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
          style="background:#ffffff;border-radius:16px;overflow:hidden;
                 box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:560px;width:100%;">
          <tr>
            <td style="background:#1e2a6e;padding:32px 40px 28px;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.3px;">
                IndSphinx
              </h1>
              <p style="margin:4px 0 0;color:#a8b4e8;font-size:13px;">
                Complaint Update
              </p>
            </td>
          </tr>

          <tr>
            <td style="padding:32px 40px 10px;">
              <p style="margin:0 0 10px;font-size:16px;font-weight:700;color:#1a1f36;">
                Hi ${escapeHtml(name || "there")},
              </p>
              <p style="margin:0 0 18px;font-size:14px;color:#6b7280;line-height:1.6;">
                Your complaint has been marked as <strong>Closed</strong>.
              </p>

              <table width="100%" cellpadding="0" cellspacing="0"
                style="background:#f8f9ff;border:1px solid #e0e4f8;border-radius:12px;margin-bottom:18px;">
                <tr>
                  <td style="padding:18px 20px;">
                    <p style="margin:0 0 10px;font-size:11px;font-weight:700;color:#6b7280;letter-spacing:0.08em;text-transform:uppercase;">
                      Complaint details
                    </p>
                    <p style="margin:0 0 6px;font-size:14px;color:#1a1f36;font-weight:700;">
                      ${escapeHtml(complaintTitle || "Complaint")}
                    </p>
                    <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.6;">
                      ${category ? `Category: ${escapeHtml(category)}<br/>` : ""}
                      ${flatNumber ? `Flat: ${escapeHtml(flatNumber)}<br/>` : ""}
                      Closed on: ${escapeHtml(closedDate || "")}
                    </p>
                  </td>
                </tr>
              </table>

              <table cellpadding="0" cellspacing="0" style="margin-bottom:22px;">
                <tr>
                  <td style="border-radius:10px;overflow:hidden;">
                    <a href="${appUrl}"
                      style="display:inline-block;background:#1e2a6e;color:#ffffff;text-decoration:none;
                             font-size:14px;font-weight:700;padding:12px 22px;border-radius:10px;">
                      Open IndSphinx →
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                This is an automated message. Please do not reply.
              </p>
            </td>
          </tr>
          <tr>
            <td style="background:#f8f9ff;border-top:1px solid #e9edf7;padding:18px 40px;text-align:center;">
              <p style="margin:0;font-size:12px;color:#9ca3af;">
                IndSphinx Accommodation Management
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

async function getFcmTokenForEmail(email) {
  const e = String(email ?? "").trim().toLowerCase();
  if (!e) return null;
  const snap = await admin
    .firestore()
    .collection("Users")
    .where("Email", "==", e)
    .limit(1)
    .get();
  if (snap.empty) return null;
  const data = snap.docs[0].data();
  return sanitizeFcmToken(data.fcm_token);
}

/** Strip whitespace / invisible chars — iOS paste or Firestore edits can break FCM otherwise. */
function sanitizeFcmToken(raw) {
  if (typeof raw !== "string") return null;
  const s = raw.trim().replace(/[\r\n\u200b\u00a0]/g, "").replace(/\s+/g, "");
  return s.length ? s : null;
}

function isCredentialLikeMessagingError(err) {
  const msg = String(err?.message || err || "");
  const code = String(err?.code || err?.errorInfo?.code || "");
  return (
    code === "UNAUTHENTICATED" ||
    msg.includes("authentication credential") ||
    msg.includes("OAuth 2 access token") ||
    msg.includes("invalid authentication credentials")
  );
}

async function sendFcmToToken(rawToken, { title, body, data }) {
  const token = sanitizeFcmToken(rawToken);
  if (!token) return;

  const message = {
    token,
    notification: { title, body },
    data: Object.fromEntries(
      Object.entries(data || {}).map(([k, v]) => [k, String(v)]),
    ),
    android: { priority: "high" },
    apns: { headers: { "apns-priority": "10" } },
  };

  const maxAttempts = 3;
  let lastErr;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await admin.messaging().send(message);
    } catch (e) {
      lastErr = e;
      const details = {
        attempt,
        code: e?.code,
        errorInfo: e?.errorInfo,
        message: e?.message,
      };
      console.error("[sendFcmToToken] messaging.send failed:", JSON.stringify(details));
      if (attempt < maxAttempts && isCredentialLikeMessagingError(e)) {
        await new Promise((r) => setTimeout(r, 400 * attempt));
        continue;
      }
      throw e;
    }
  }
  throw lastErr;
}

// ─── Cloud Function ───────────────────────────────────────────────────────────

exports.createOccupantAccount = onDocumentCreated(
  {
    document: "Occupants/{occupantId}",
    secrets: [SMTP_USER, SMTP_PASS],
    // Change region to match your Firebase project's preferred region,
    // e.g. "asia-south1" for Mumbai, "us-central1" for Iowa, etc.
    region: "asia-south1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) {
      console.error("No snapshot data. Exiting.");
      return;
    }

    const data = snap.data();
    if (!data) {
      console.error("Empty document data. Exiting.");
      return;
    }

    // ── Idempotency guard ──────────────────────────────────────────────────────
    // If authUid is already set, this function already ran successfully.
    if (data.authUid) {
      console.log(`Occupant ${snap.id} already has authUid (${data.authUid}). Skipping.`);
      return;
    }

    // ── Validate required fields ───────────────────────────────────────────────
    const name  = String(data.Name  ?? "").trim();
    const email = String(data.Email ?? "").trim().toLowerCase();
    const empId = String(data.EMPID ?? "").trim();

    if (!name || !email || !empId) {
      console.error(
        `Occupant ${snap.id} is missing required fields (Name="${name}", Email="${email}", EMPID="${empId}"). Skipping.`,
      );
      return;
    }

    const password = generatePassword(name, empId);
    console.log(`Processing new occupant: ${name} <${email}> (EMPID: ${empId})`);

    // ── 1. Create Firebase Auth account ───────────────────────────────────────
    let userRecord;
    try {
      userRecord = await admin.auth().createUser({
        email,
        password,
        displayName: name,
        emailVerified: false,
      });
      console.log(`✓ Auth user created: uid=${userRecord.uid}`);
    } catch (authErr) {
      if (authErr.code === "auth/email-already-exists") {
        // Occupant's email is already registered (e.g. they were previously added).
        // Fetch the existing user so we can still create the Users document.
        console.warn(`Auth user already exists for ${email}. Linking existing account.`);
        userRecord = await admin.auth().getUserByEmail(email);
      } else {
        console.error("Failed to create Auth user:", authErr);
        throw authErr; // Let the function retry
      }
    }

    const uid = userRecord.uid;

    // ── 2. Create / merge Users collection document ────────────────────────────
    await admin.firestore().collection("Users").doc(uid).set(
      {
        Name:    name,
        Email:   email,
        Role:    "OCCUPANT",
        Enabled: true,
      },
      { merge: true }, // safe to run multiple times
    );
    console.log(`✓ Users/${uid} document created/updated`);

    // ── 3. Stamp authUid onto the Occupants document (marks function as done) ──
    await snap.ref.update({ authUid: uid });
    console.log(`✓ Occupants/${snap.id}.authUid set to ${uid}`);

    // ── 4. Send welcome e-mail ─────────────────────────────────────────────────
    const appUrl = process.env.APP_URL || "https://ind-sphinx.web.app";

    try {
      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {
          user: SMTP_USER.value(),
          pass: SMTP_PASS.value(),
        },
      });

      await transporter.sendMail({
        from:    `"IndSphinx Accommodation" <${SMTP_USER.value()}>`,
        to:      email,
        subject: "Welcome to IndSphinx : Your Login Credentials",
        html:    buildWelcomeEmail({ name, email, password, appUrl }),
      });

      console.log(`✓ Welcome email sent to ${email}`);
    } catch (mailErr) {
      // Email failure is non-fatal — account is already created.
      // Admin should manually inform the occupant of their credentials.
      console.error(`✗ Failed to send welcome email to ${email}:`, mailErr.message);
    }

    console.log(`✓ onOccupantCreated complete for ${snap.id} (uid: ${uid})`);
  },
);

// ─── Worker Account Creation ──────────────────────────────────────────────────

/**
 * Fires when a new document is added to the `Workers` collection.
 *
 * Actions (mirror of createOccupantAccount):
 *   1. Generate password → first4(Name) + WorkerUID + 3 random digits
 *   2. Create Firebase Auth account
 *   3. Create Users/{uid} → { Name, Email, Role: "WORKER", Enabled: true }
 *   4. Stamp authUid onto Workers/{id}
 *   5. Send welcome email (best-effort)
 */
exports.createWorkerAccount = onDocumentCreated(
  {
    document: "Workers/{workerId}",
    secrets: [SMTP_USER, SMTP_PASS],
    region: "asia-south1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) { console.error("No snapshot data. Exiting."); return; }

    const data = snap.data();
    if (!data) { console.error("Empty document data. Exiting."); return; }

    // Idempotency guard
    if (data.authUid) {
      console.log(`Worker ${snap.id} already has authUid (${data.authUid}). Skipping.`);
      return;
    }

    const name      = String(data.Name      ?? "").trim();
    const email     = String(data.Email     ?? "").trim().toLowerCase();
    const workerUid = String(data.WorkerUID ?? "").trim();

    if (!name || !email || !workerUid) {
      console.error(
        `Worker ${snap.id} missing required fields (Name="${name}", Email="${email}", WorkerUID="${workerUid}"). Skipping.`,
      );
      return;
    }

    // Password uses WorkerUID in place of EMPID — same format, same helper
    const password = generatePassword(name, workerUid);
    console.log(`Processing new worker: ${name} <${email}> (WorkerUID: ${workerUid})`);

    // 1. Create Firebase Auth account
    let userRecord;
    try {
      userRecord = await admin.auth().createUser({
        email,
        password,
        displayName: name,
        emailVerified: false,
      });
      console.log(`✓ Auth user created: uid=${userRecord.uid}`);
    } catch (authErr) {
      if (authErr.code === "auth/email-already-exists") {
        console.warn(`Auth user already exists for ${email}. Linking existing account.`);
        userRecord = await admin.auth().getUserByEmail(email);
      } else {
        console.error("Failed to create Auth user:", authErr);
        throw authErr;
      }
    }

    const uid = userRecord.uid;

    // 2. Create / merge Users document with Role: "WORKER"
    await admin.firestore().collection("Users").doc(uid).set(
      { Name: name, Email: email, Role: "WORKER", Enabled: true },
      { merge: true },
    );
    console.log(`✓ Users/${uid} document created/updated (Role: WORKER)`);

    // 3. Stamp authUid onto the Workers document
    await snap.ref.update({ authUid: uid });
    console.log(`✓ Workers/${snap.id}.authUid set to ${uid}`);

    // 4. Send welcome email (best-effort)
    const appUrl = process.env.APP_URL || "https://ind-sphinx.web.app";
    try {
      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: { user: SMTP_USER.value(), pass: SMTP_PASS.value() },
      });

      await transporter.sendMail({
        from:    `"IndSphinx Accommodation" <${SMTP_USER.value()}>`,
        to:      email,
        subject: "Welcome to IndSphinx : Your Login Credentials",
        html:    buildWelcomeEmail({ name, email, password, appUrl }),
      });

      console.log(`✓ Welcome email sent to ${email}`);
    } catch (mailErr) {
      console.error(`✗ Failed to send welcome email to ${email}:`, mailErr.message);
    }

    console.log(`✓ createWorkerAccount complete for ${snap.id} (uid: ${uid})`);
  },
);

// ─── Complaint status notifications ───────────────────────────────────────────
/**
 * Fires when a complaint document is updated and the Status changes.
 *
 * Status transitions we handle:
 * - OPEN      -> ASSIGNED   : FCM only (worker assigned)
 * - ASSIGNED  -> COMPLETED  : FCM only (worker marked completed)
 * - *         -> CLOSED     : FCM + email (complaint closed)
 *
 * FCM token is stored in Users collection under field `fcm_token`.
 * We resolve the user by complaint.OccupantEmail matching Users.Email.
 */
exports.notifyOnComplaintStatusChange = onDocumentUpdated(
  {
    document: "Complaints/{complaintId}",
    secrets: [SMTP_USER, SMTP_PASS],
    region: "asia-south1",
  },
  async (event) => {
    const complaintId = event.params?.complaintId;
    console.log(`[notifyOnComplaintStatusChange] triggered complaintId=${complaintId}`);

    // In firebase-functions v2, event.data is a Change<DocumentSnapshot>
    const beforeSnap = event.data?.before;
    const afterSnap  = event.data?.after;
    if (!beforeSnap || !afterSnap) {
      console.warn("[notifyOnComplaintStatusChange] missing before/after snapshots. Exiting.");
      return;
    }

    const before = beforeSnap.data();
    const after  = afterSnap.data();
    if (!before || !after) {
      console.warn("[notifyOnComplaintStatusChange] empty before/after data. Exiting.");
      return;
    }

    const beforeStatus = String(before.Status ?? "").trim().toUpperCase();
    const afterStatus  = String(after.Status  ?? "").trim().toUpperCase();
    if (!beforeStatus || !afterStatus) {
      console.warn("[notifyOnComplaintStatusChange] missing Status field(s).", { beforeStatus, afterStatus });
      return;
    }
    if (beforeStatus === afterStatus) {
      console.log("[notifyOnComplaintStatusChange] Status unchanged. Skipping.", { status: afterStatus });
      return;
    }

    console.log("[notifyOnComplaintStatusChange] Status changed.", { beforeStatus, afterStatus });

    const occupantEmail = String(after.OccupantEmail ?? "").trim().toLowerCase();
    const occupantName  = String(after.OccupantName  ?? "").trim();
    const workerName    = String(after.WorkerName    ?? "").trim();
    const workerDocId   = String(after.WorkerUid     ?? "").trim();
    const problem       = String(after.Problem       ?? "").trim();
    const category      = String(after.Category      ?? "").trim();
    const flatNumber    = String(after.FlatNumber    ?? "").trim();
    console.log("[notifyOnComplaintStatusChange] complaint meta", {
      occupantEmail,
      occupantName,
      workerName,
      workerDocId,
      problem,
      category,
      flatNumber,
    });

    // Resolve occupant FCM token
    let token = null;
    try {
      token = await getFcmTokenForEmail(occupantEmail);
      console.log("[notifyOnComplaintStatusChange] occupant token lookup", {
        found: Boolean(token),
        last6: token ? token.slice(-6) : null,
      });
    } catch (e) {
      console.error("Failed to resolve occupant FCM token:", e?.message || e);
    }

    // OPEN -> ASSIGNED
    if (beforeStatus === "OPEN" && afterStatus === "ASSIGNED") {
      const appUrl = process.env.APP_URL || "https://ind-sphinx.web.app";

      // 1. Notify occupant via FCM
      const occupantTitle = "Worker assigned";
      const occupantBody  = workerName
        ? `${workerName} has been assigned to your complaint.`
        : "A worker has been assigned to your complaint.";
      if (token) {
        try {
          const msgId = await sendFcmToToken(token, {
            title: occupantTitle,
            body:  occupantBody,
            data:  { complaintId, status: afterStatus },
          });
          console.log("[notifyOnComplaintStatusChange] FCM sent to occupant (OPEN->ASSIGNED).", { msgId });
        } catch (e) {
          console.error("FCM send failed to occupant (OPEN->ASSIGNED):", e?.message || e);
        }
      } else {
        console.warn("[notifyOnComplaintStatusChange] no occupant fcm_token; skipping occupant FCM.");
      }

      // 2. Notify worker via FCM + email
      if (workerDocId) {
        let workerAuthUid = "";
        let workerEmail   = "";

        try {
          const workerSnap = await admin.firestore().collection("Workers").doc(workerDocId).get();
          if (workerSnap.exists) {
            workerAuthUid = String(workerSnap.data()?.authUid ?? "").trim();
            workerEmail   = String(workerSnap.data()?.Email   ?? "").trim().toLowerCase();
          }
        } catch (e) {
          console.error("[notifyOnComplaintStatusChange] Failed to fetch worker doc:", e?.message || e);
        }

        // FCM
        if (workerAuthUid) {
          try {
            const userSnap = await admin.firestore().collection("Users").doc(workerAuthUid).get();
            const workerFcmToken = userSnap.exists ? sanitizeFcmToken(userSnap.data()?.fcm_token) : null;

            if (workerFcmToken) {
              const wMsgId = await sendFcmToToken(workerFcmToken, {
                title: "New complaint assigned",
                body:  problem
                  ? `You have been assigned: "${problem}"${flatNumber ? ` (Flat ${flatNumber})` : ""}.`
                  : "A new complaint has been assigned to you.",
                data: { complaintId, status: afterStatus },
              });
              console.log("[notifyOnComplaintStatusChange] FCM sent to worker (OPEN->ASSIGNED).", { wMsgId });
            } else {
              console.warn("[notifyOnComplaintStatusChange] no worker fcm_token; skipping worker FCM.");
            }
          } catch (e) {
            console.error("FCM send failed to worker (OPEN->ASSIGNED):", e?.message || e);
          }
        }

        // Email
        if (workerEmail) {
          try {
            const transporter = nodemailer.createTransport({
              service: "gmail",
              auth: { user: SMTP_USER.value(), pass: SMTP_PASS.value() },
            });
            await transporter.sendMail({
              from:    `"IndSphinx Accommodation" <${SMTP_USER.value()}>`,
              to:      workerEmail,
              subject: "IndSphinx - New complaint assigned to you",
              html:    buildWorkerAssignedEmail({
                workerName,
                problem,
                category,
                flatNumber,
                occupantName,
                appUrl,
              }),
            });
            console.log("[notifyOnComplaintStatusChange] assignment email sent to worker.", { to: workerEmail });
          } catch (e) {
            console.error("Failed to send assignment email to worker:", e?.message || e);
          }
        }
      }

      return;
    }

    // ASSIGNED -> COMPLETED
    if (beforeStatus === "ASSIGNED" && afterStatus === "COMPLETED") {
      const title = "Work completed";
      const body  = "The worker marked your complaint as completed. Please review and mark it as closed.";
      try {
        if (!token) {
          console.warn("[notifyOnComplaintStatusChange] no fcm_token found; skipping FCM (ASSIGNED->COMPLETED).");
          return;
        }
        const msgId = await sendFcmToToken(token, {
          title,
          body,
          data: { complaintId, status: afterStatus },
        });
        console.log("[notifyOnComplaintStatusChange] FCM sent (ASSIGNED->COMPLETED).", { msgId });
      } catch (e) {
        console.error("FCM send failed (ASSIGNED->COMPLETED):", e?.message || e);
      }
      return;
    }

    // Any -> CLOSED (we intentionally allow ASSIGNED->CLOSED too)
    if (afterStatus === "CLOSED" && beforeStatus !== "CLOSED") {
      const closedDate = String(after.ResolveDate ?? "").trim() || new Date().toISOString().slice(0, 10);

      // 1) FCM
      try {
        if (!token) {
          console.warn("[notifyOnComplaintStatusChange] no fcm_token found; skipping FCM (*->CLOSED).");
        } else {
          const msgId = await sendFcmToToken(token, {
            title: "Complaint closed",
            body: "Your complaint has been marked as closed.",
            data: { complaintId, status: afterStatus },
          });
          console.log("[notifyOnComplaintStatusChange] FCM sent (*->CLOSED).", { msgId });
        }
      } catch (e) {
        console.error("FCM send failed (*->CLOSED):", e?.message || e);
      }

      // 2) Email (best-effort)
      if (!occupantEmail) {
        console.warn("[notifyOnComplaintStatusChange] missing OccupantEmail; skipping email.");
        return;
      }

      const appUrl = process.env.APP_URL || "https://ind-sphinx.web.app";
      try {
        const transporter = nodemailer.createTransport({
          service: "gmail",
          auth: { user: SMTP_USER.value(), pass: SMTP_PASS.value() },
        });

        await transporter.sendMail({
          from: `"IndSphinx Accommodation" <${SMTP_USER.value()}>`,
          to: occupantEmail,
          subject: "IndSphinx - Your complaint was closed",
          html: buildComplaintClosedEmail({
            name: occupantName,
            complaintTitle: problem,
            category,
            flatNumber,
            closedDate,
            appUrl,
          }),
        });
        console.log("[notifyOnComplaintStatusChange] closed email sent.", { to: occupantEmail });
      } catch (e) {
        console.error("Failed to send closed email:", e?.message || e);
      }

      return;
    }

    // Otherwise: ignore transitions we don't care about
    console.log("[notifyOnComplaintStatusChange] transition ignored.", { beforeStatus, afterStatus });
  },
);

// ─── Notice Board FCM Broadcast ───────────────────────────────────────────────

/**
 * Fires when a new document is created in the `NoticeBoard` collection.
 * Sends an FCM push notification to all Users where Enabled=true and fcm_token is set.
 */
exports.onNoticeBoardCreated = onDocumentCreated(
  {
    document: "NoticeBoard/{noticeId}",
    region: "asia-south1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) {
      console.warn("[onNoticeBoardCreated] no snapshot data.");
      return;
    }

    const data = snap.data();
    const title       = String(data?.title       ?? "").trim() || "New Notice";
    const description = String(data?.description ?? "").trim();

    console.log(`[onNoticeBoardCreated] Broadcasting notice "${title}" (${snap.id})`);

    // Fetch enabled occupants (and coordinators) only — workers are excluded
    let usersSnap;
    try {
      usersSnap = await admin
        .firestore()
        .collection("Users")
        .where("Enabled", "==", true)
        .where("Role", "in", ["OCCUPANT", "COORDINATOR"])
        .get();
    } catch (e) {
      console.error("[onNoticeBoardCreated] Failed to fetch users:", e.message);
      return;
    }

    const tokens = [];
    usersSnap.forEach((doc) => {
      const token = sanitizeFcmToken(doc.data().fcm_token);
      if (token) tokens.push(token);
    });

    if (tokens.length === 0) {
      console.log("[onNoticeBoardCreated] No eligible FCM tokens. Skipping broadcast.");
      return;
    }

    console.log(`[onNoticeBoardCreated] Sending to ${tokens.length} token(s).`);

    try {
      const result = await admin.messaging().sendEachForMulticast({
        tokens,
        notification: { title, body: description || title },
        android: { priority: "high" },
        apns: { headers: { "apns-priority": "10" } },
        data: { type: "NOTICE_BOARD", noticeId: snap.id },
      });
      console.log(
        `[onNoticeBoardCreated] FCM done — success: ${result.successCount}, failed: ${result.failureCount}`,
      );
    } catch (e) {
      console.error("[onNoticeBoardCreated] FCM multicast failed:", e.message);
    }
  },
);

// ─── Targeted Notification (Callable) ────────────────────────────────────────

/** Builds an HTML email body for a targeted admin notification. */
function buildAdminNotificationEmail({ name, title, description, appUrl }) {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${escapeHtml(title)}</title>
</head>
<body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f6fb;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
          style="background:#ffffff;border-radius:16px;overflow:hidden;
                 box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:560px;width:100%;">

          <tr>
            <td style="background:#1e2a6e;padding:32px 40px 28px;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.3px;">
                IndSphinx
              </h1>
              <p style="margin:4px 0 0;color:#a8b4e8;font-size:13px;">
                Notification from Admin
              </p>
            </td>
          </tr>

          <tr>
            <td style="padding:32px 40px 10px;">
              <p style="margin:0 0 10px;font-size:16px;font-weight:700;color:#1a1f36;">
                Hi ${escapeHtml(name || "there")},
              </p>
              <p style="margin:0 0 16px;font-size:15px;font-weight:700;color:#2b3a8c;">
                ${escapeHtml(title)}
              </p>
              <p style="margin:0 0 24px;font-size:14px;color:#6b7280;line-height:1.7;
                         white-space:pre-wrap;">
                ${escapeHtml(description)}
              </p>

              <table cellpadding="0" cellspacing="0" style="margin-bottom:22px;">
                <tr>
                  <td style="border-radius:10px;overflow:hidden;">
                    <a href="${appUrl}"
                      style="display:inline-block;background:#1e2a6e;color:#ffffff;
                             text-decoration:none;font-size:14px;font-weight:700;
                             padding:12px 22px;border-radius:10px;">
                      Open IndSphinx →
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                This is an automated message. Please do not reply.
              </p>
            </td>
          </tr>

          <tr>
            <td style="background:#f8f9ff;border-top:1px solid #e9edf7;
                       padding:18px 40px;text-align:center;">
              <p style="margin:0;font-size:12px;color:#9ca3af;">
                IndSphinx Accommodation Management
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

/**
 * Callable function: sends a targeted notification (FCM + optional email) to a single user.
 *
 * Expected request.data:
 *   targetUserId {string}  — Firebase Auth UID (= Users document ID)
 *   title        {string}  — notification title (max 80 chars)
 *   description  {string}  — notification body  (max 500 chars)
 *   sendEmail    {boolean} — also send an email to the user
 */
exports.sendTargetedNotification = onCall(
  {
    secrets: [SMTP_USER, SMTP_PASS],
    region: "asia-south1",
  },
  async (request) => {
    const { targetUserId, title, description, sendEmail } = request.data ?? {};

    if (!targetUserId || !title) {
      throw new Error("targetUserId and title are required.");
    }

    const userDoc = await admin.firestore().collection("Users").doc(targetUserId).get();
    if (!userDoc.exists) {
      throw new Error("Target user not found.");
    }

    const userData    = userDoc.data();
    const userName    = String(userData.Name  ?? "").trim();
    const userEmail   = String(userData.Email ?? "").trim().toLowerCase();
    const userEnabled = userData.Enabled === true;
    const fcmToken = sanitizeFcmToken(userData.fcm_token);

    const appUrl = process.env.APP_URL || "https://ind-sphinx.web.app";
    let fcmSent   = false;
    let emailSent = false;

    // 1. FCM
    if (fcmToken && userEnabled) {
      try {
        await sendFcmToToken(fcmToken, {
          title,
          body: description || title,
          data: { type: "TARGETED_NOTIFICATION" },
        });
        fcmSent = true;
        console.log(`[sendTargetedNotification] FCM sent to uid=${targetUserId}`);
      } catch (e) {
        console.error(
          `[sendTargetedNotification] FCM failed for uid=${targetUserId}:`,
          e?.message || e,
          e?.code ? `code=${e.code}` : "",
          e?.errorInfo ? JSON.stringify(e.errorInfo) : "",
        );
      }
    } else {
      console.log(
        `[sendTargetedNotification] FCM skipped — token: ${Boolean(fcmToken)}, enabled: ${userEnabled}`,
      );
    }

    // 2. Email (best-effort)
    if (sendEmail && userEmail) {
      try {
        const transporter = nodemailer.createTransport({
          service: "gmail",
          auth: { user: SMTP_USER.value(), pass: SMTP_PASS.value() },
        });
        await transporter.sendMail({
          from:    `"IndSphinx Accommodation" <${SMTP_USER.value()}>`,
          to:      userEmail,
          subject: `[IndSphinx] ${title}`,
          html:    buildAdminNotificationEmail({ name: userName, title, description: description || "", appUrl }),
        });
        emailSent = true;
        console.log(`[sendTargetedNotification] Email sent to ${userEmail}`);
      } catch (e) {
        console.error(`[sendTargetedNotification] Email failed for ${userEmail}:`, e.message);
      }
    }

    return { success: true, fcmSent, emailSent };
  },
);

// ─── Visitor Pass Status Notifications ───────────────────────────────────────

/** Builds HTML email for a visitor pass acceptance or rejection. */
function buildVisitorPassEmail({ occupantName, visitorName, status, visitDate, flatNumber, purposeOfVisit, appUrl }) {
  const isAccepted = status === "ACCEPTED";
  const accentColor = isAccepted ? "#16a34a" : "#dc2626";
  const accentLight = isAccepted ? "#f0fdf4" : "#fef2f2";
  const accentBorder = isAccepted ? "#bbf7d0" : "#fecaca";
  const statusLabel = isAccepted ? "Accepted" : "Rejected";

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Visitor Pass ${escapeHtml(statusLabel)}</title>
</head>
<body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f6fb;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
          style="background:#ffffff;border-radius:16px;overflow:hidden;
                 box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:560px;width:100%;">

          <tr>
            <td style="background:#1e2a6e;padding:32px 40px 28px;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.3px;">
                IndSphinx
              </h1>
              <p style="margin:4px 0 0;color:#a8b4e8;font-size:13px;">
                Visitor Pass Update
              </p>
            </td>
          </tr>

          <tr>
            <td style="padding:32px 40px 10px;">
              <p style="margin:0 0 10px;font-size:16px;font-weight:700;color:#1a1f36;">
                Hi ${escapeHtml(occupantName || "there")},
              </p>
              <p style="margin:0 0 18px;font-size:14px;color:#6b7280;line-height:1.6;">
                The visitor pass request for your flat has been
                <strong style="color:${accentColor};">${escapeHtml(statusLabel)}</strong>.
              </p>

              <!-- Status banner -->
              <table width="100%" cellpadding="0" cellspacing="0"
                style="background:${accentLight};border:1px solid ${accentBorder};
                       border-radius:12px;margin-bottom:18px;">
                <tr>
                  <td style="padding:16px 20px;text-align:center;">
                    <p style="margin:0;font-size:16px;font-weight:700;color:${accentColor};">
                      Visitor Pass ${escapeHtml(statusLabel)}
                    </p>
                  </td>
                </tr>
              </table>

              <!-- Visitor details -->
              <table width="100%" cellpadding="0" cellspacing="0"
                style="background:#f8f9ff;border:1px solid #e0e4f8;border-radius:12px;margin-bottom:18px;">
                <tr>
                  <td style="padding:18px 20px;">
                    <p style="margin:0 0 10px;font-size:11px;font-weight:700;color:#6b7280;
                               letter-spacing:0.08em;text-transform:uppercase;">
                      Visitor details
                    </p>
                    <p style="margin:0 0 6px;font-size:14px;color:#1a1f36;font-weight:700;">
                      ${escapeHtml(visitorName || "—")}
                    </p>
                    <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.6;">
                      ${flatNumber     ? `Flat: ${escapeHtml(flatNumber)}<br/>`           : ""}
                      ${visitDate      ? `Visit date: ${escapeHtml(visitDate)}<br/>`      : ""}
                      ${purposeOfVisit ? `Purpose: ${escapeHtml(purposeOfVisit)}`         : ""}
                    </p>
                  </td>
                </tr>
              </table>

              <table cellpadding="0" cellspacing="0" style="margin-bottom:22px;">
                <tr>
                  <td style="border-radius:10px;overflow:hidden;">
                    <a href="${appUrl}"
                      style="display:inline-block;background:#1e2a6e;color:#ffffff;
                             text-decoration:none;font-size:14px;font-weight:700;
                             padding:12px 22px;border-radius:10px;">
                      Open IndSphinx →
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:0;font-size:12px;color:#9ca3af;line-height:1.6;">
                This is an automated message. Please do not reply.
              </p>
            </td>
          </tr>

          <tr>
            <td style="background:#f8f9ff;border-top:1px solid #e9edf7;
                       padding:18px 40px;text-align:center;">
              <p style="margin:0;font-size:12px;color:#9ca3af;">
                IndSphinx Accommodation Management
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

/**
 * Fires when a VisitorPass document is updated.
 * When Status changes from PENDING to ACCEPTED or REJECTED,
 * sends FCM + email to the occupant.
 */
exports.onVisitorPassStatusChange = onDocumentUpdated(
  {
    document: "VisitorPass/{passId}",
    secrets: [SMTP_USER, SMTP_PASS],
    region: "asia-south1",
  },
  async (event) => {
    const passId = event.params?.passId;
    console.log(`[onVisitorPassStatusChange] triggered passId=${passId}`);

    const beforeSnap = event.data?.before;
    const afterSnap  = event.data?.after;
    if (!beforeSnap || !afterSnap) {
      console.warn("[onVisitorPassStatusChange] missing before/after snapshots. Exiting.");
      return;
    }

    const before = beforeSnap.data();
    const after  = afterSnap.data();
    if (!before || !after) {
      console.warn("[onVisitorPassStatusChange] empty before/after data. Exiting.");
      return;
    }

    const beforeStatus = String(before.Status ?? "").trim().toUpperCase();
    const afterStatus  = String(after.Status  ?? "").trim().toUpperCase();

    if (beforeStatus === afterStatus) {
      console.log("[onVisitorPassStatusChange] Status unchanged. Skipping.");
      return;
    }

    // Only act on PENDING → ACCEPTED or PENDING → REJECTED
    if (beforeStatus !== "PENDING" || (afterStatus !== "ACCEPTED" && afterStatus !== "REJECTED")) {
      console.log("[onVisitorPassStatusChange] Transition not actionable.", { beforeStatus, afterStatus });
      return;
    }

    console.log("[onVisitorPassStatusChange] Actionable transition.", { beforeStatus, afterStatus });

    const occupantId   = String(after.OccupantId   ?? "").trim();
    const occupantName = String(after.OccupantName ?? "").trim();
    const visitorName  = String(after.VisitorName  ?? "").trim();
    const flatNumber   = String(after.FlatNumber   ?? "").trim();
    const purpose      = String(after.PurposeOfVisit ?? "").trim();

    // Format visit date for email
    let visitDateStr = "";
    try {
      const vd = after.VisitDate;
      const dateObj = (vd && typeof vd.toDate === "function") ? vd.toDate() : (vd ? new Date(vd) : null);
      if (dateObj) {
        visitDateStr = new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" }).format(dateObj);
      }
    } catch (_) { /* ignore */ }

    // Resolve occupant's authUid and email via Occupants collection
    let occupantEmail   = "";
    let occupantAuthUid = "";

    if (occupantId) {
      try {
        const occSnap = await admin.firestore().collection("Occupants").doc(occupantId).get();
        if (occSnap.exists) {
          occupantEmail   = String(occSnap.data()?.Email   ?? "").trim().toLowerCase();
          occupantAuthUid = String(occSnap.data()?.authUid ?? "").trim();
        }
      } catch (e) {
        console.error("[onVisitorPassStatusChange] Failed to fetch occupant:", e.message);
      }
    }

    // Fallback: resolve email from Users if we still don't have it
    if (!occupantEmail && occupantAuthUid) {
      try {
        const userSnap = await admin.firestore().collection("Users").doc(occupantAuthUid).get();
        if (userSnap.exists) {
          occupantEmail = String(userSnap.data()?.Email ?? "").trim().toLowerCase();
        }
      } catch (e) {
        console.error("[onVisitorPassStatusChange] Failed to fetch user:", e.message);
      }
    }

    const appUrl = process.env.APP_URL || "https://ind-sphinx.web.app";

    const fcmTitle = afterStatus === "ACCEPTED"
      ? "Visitor pass accepted"
      : "Visitor pass rejected";
    const fcmBody = afterStatus === "ACCEPTED"
      ? `${visitorName || "Your visitor"}'s pass has been accepted.`
      : `${visitorName || "Your visitor"}'s pass has been rejected.`;

    // 1. FCM
    if (occupantAuthUid) {
      try {
        const userSnap = await admin.firestore().collection("Users").doc(occupantAuthUid).get();
        const fcmToken = userSnap.exists ? sanitizeFcmToken(userSnap.data()?.fcm_token) : null;

        if (fcmToken) {
          const msgId = await sendFcmToToken(fcmToken, {
            title: fcmTitle,
            body:  fcmBody,
            data:  { passId, status: afterStatus, type: "VISITOR_PASS" },
          });
          console.log(`[onVisitorPassStatusChange] FCM sent to occupant.`, { msgId });
        } else {
          console.warn("[onVisitorPassStatusChange] No FCM token for occupant; skipping FCM.");
        }
      } catch (e) {
        console.error("[onVisitorPassStatusChange] FCM failed:", e.message);
      }
    }

    // 2. Email
    if (occupantEmail) {
      try {
        const transporter = nodemailer.createTransport({
          service: "gmail",
          auth: { user: SMTP_USER.value(), pass: SMTP_PASS.value() },
        });
        await transporter.sendMail({
          from:    `"IndSphinx Accommodation" <${SMTP_USER.value()}>`,
          to:      occupantEmail,
          subject: `IndSphinx — Visitor Pass ${afterStatus === "ACCEPTED" ? "Accepted" : "Rejected"}`,
          html:    buildVisitorPassEmail({
            occupantName,
            visitorName,
            status: afterStatus,
            visitDate: visitDateStr,
            flatNumber,
            purposeOfVisit: purpose,
            appUrl,
          }),
        });
        console.log(`[onVisitorPassStatusChange] Email sent to occupant.`, { to: occupantEmail });
      } catch (e) {
        console.error("[onVisitorPassStatusChange] Email failed:", e.message);
      }
    } else {
      console.warn("[onVisitorPassStatusChange] No occupant email found; skipping email.");
    }
  },
);
