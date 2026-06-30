'use strict';

const nodemailer = require('nodemailer');

// ─── Transporter Factory ──────────────────────────────────────────────────────
function createTransporter() {
  const { SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS } = process.env;

  const missing = [];
  if (!SMTP_HOST) missing.push('SMTP_HOST');
  if (!SMTP_USER) missing.push('SMTP_USER');
  if (!SMTP_PASS) missing.push('SMTP_PASS');

  if (missing.length > 0) {
    throw new Error(
      `Email service misconfigured. Missing environment variable(s): ${missing.join(', ')}. ` +
      'Please check your .env file.'
    );
  }

  const port = parseInt(SMTP_PORT, 10) || 587;
  const secure = port === 465; // TLS for port 465, STARTTLS otherwise

  return nodemailer.createTransport({
    host: SMTP_HOST,
    port,
    secure,
    auth: {
      user: SMTP_USER,
      pass: SMTP_PASS,
    },
  });
}

// ─── Timestamp Formatter ──────────────────────────────────────────────────────
function formatTimestamp(timestamp) {
  if (!timestamp) return 'Not provided';

  const millis = parseInt(timestamp, 10);
  if (isNaN(millis)) return String(timestamp);

  return new Date(millis).toLocaleString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    timeZoneName: 'short',
  });
}

// ─── HTML Email Template ──────────────────────────────────────────────────────
function buildHtmlBody(name, formattedDate, zipFileName) {
  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>New VoiceDrop Recording</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f4f4f7; margin: 0; padding: 0; }
    .container { max-width: 560px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .header { background: #4F46E5; padding: 32px 40px; }
    .header h1 { color: #ffffff; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.3px; }
    .header p { color: #C7D2FE; margin: 6px 0 0; font-size: 14px; }
    .body { padding: 32px 40px; }
    .field { margin-bottom: 20px; }
    .field label { display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.8px; color: #6B7280; margin-bottom: 4px; }
    .field span { font-size: 16px; color: #111827; font-weight: 500; }
    .attachment-note { background: #F0FDF4; border: 1px solid #BBF7D0; border-radius: 6px; padding: 14px 18px; margin-top: 8px; font-size: 14px; color: #166534; }
    .attachment-note strong { display: block; margin-bottom: 2px; }
    .footer { padding: 20px 40px; border-top: 1px solid #E5E7EB; font-size: 12px; color: #9CA3AF; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>🎙️ New VoiceDrop Recording</h1>
      <p>A new voice recording package has been delivered.</p>
    </div>
    <div class="body">
      <div class="field">
        <label>Sender Name</label>
        <span>${escapeHtml(name)}</span>
      </div>
      <div class="field">
        <label>Recorded At</label>
        <span>${escapeHtml(formattedDate)}</span>
      </div>
      <div class="attachment-note">
        <strong>📎 Attachment: ${escapeHtml(zipFileName)}</strong>
        The ZIP file containing the voice recording is attached to this email.
        Extract the archive to access the audio files inside.
      </div>
    </div>
    <div class="footer">
      Sent automatically by the VoiceDrop app. Do not reply to this email.
    </div>
  </div>
</body>
</html>
`.trim();
}

// ─── HTML Escaping ────────────────────────────────────────────────────────────
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ─── Plain-text Email Body ────────────────────────────────────────────────────
function buildTextBody(name, formattedDate, zipFileName) {
  return [
    'New VoiceDrop Recording',
    '=======================',
    '',
    `Sender Name : ${name}`,
    `Recorded At : ${formattedDate}`,
    `Attachment  : ${zipFileName}`,
    '',
    'The ZIP file containing the voice recording is attached.',
    'Extract the archive to access the audio files inside.',
    '',
    '---',
    'Sent automatically by the VoiceDrop app.',
  ].join('\n');
}

// ─── sendEmail ────────────────────────────────────────────────────────────────
async function sendEmail({ name, timestamp, zipFilePath, zipFileName }) {
  const { SMTP_USER, RECIPIENT_EMAIL } = process.env;

  if (!RECIPIENT_EMAIL) {
    throw new Error(
      'Email delivery failed: RECIPIENT_EMAIL environment variable is not set. ' +
      'Please configure it in your .env file.'
    );
  }

  const transporter = createTransporter();
  const formattedDate = formatTimestamp(timestamp);
  const subject = `New VoiceDrop Recording — ${name}`;

  const mailOptions = {
    from: `"VoiceDrop" <${SMTP_USER}>`,
    to: RECIPIENT_EMAIL,
    subject,
    text: buildTextBody(name, formattedDate, zipFileName),
    html: buildHtmlBody(name, formattedDate, zipFileName),
    attachments: [
      {
        filename: zipFileName,
        path: zipFilePath,
        contentType: 'application/zip',
      },
    ],
  };

  const info = await transporter.sendMail(mailOptions);

  console.log(
    `[emailSender] Email sent successfully. MessageId: ${info.messageId} | To: ${RECIPIENT_EMAIL} | Subject: "${subject}"`
  );

  return info;
}

module.exports = { sendEmail };
