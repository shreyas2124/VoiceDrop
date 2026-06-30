# VoiceDrop Backend

A lightweight Node.js/Express server that receives voice-recording ZIP files from the VoiceDrop Android app and delivers them to a configurable recipient via email.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Prerequisites](#prerequisites)
3. [Local Setup](#local-setup)
4. [Deploy to Render](#deploy-to-render)
5. [Deploy to Railway](#deploy-to-railway)
6. [API Reference](#api-reference)
7. [Android Integration](#android-integration)
8. [Security Notes](#security-notes)

---

## Project Overview

VoiceDrop Backend is a stateless REST API with a single purpose: accept a `.zip` file upload from the Android app (multipart `POST /api/upload`), send it as an email attachment to the configured recipient, and delete the temporary file from disk. It uses:

| Package | Role |
|---|---|
| **Express** | HTTP server and routing |
| **Multer** | Multipart file upload handling |
| **Nodemailer** | SMTP email delivery |
| **Morgan** | HTTP request logging |
| **dotenv** | Environment variable management |

---

## Prerequisites

- **Node.js 18+** — [nodejs.org](https://nodejs.org)
- **npm 9+** (bundled with Node.js)
- A **Gmail account** (or any SMTP provider) for sending emails
- *(Deployment only)* A [Render](https://render.com) or [Railway](https://railway.app) account

---

## Local Setup

### 1. Navigate to the backend directory

```bash
cd path/to/VoiceDrop/backend
```

### 2. Install dependencies

```bash
npm install
```

### 3. Configure environment variables

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

Open `.env` in any text editor and set the following:

```env
PORT=3000
NODE_ENV=development

RECIPIENT_EMAIL=recipient@example.com

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-sender@gmail.com
SMTP_PASS=your-16-char-app-password
```

### 4. Gmail App Password Setup

> **Important:** You must use a **Gmail App Password**, not your regular Gmail password. Regular passwords are blocked by Google for SMTP access.

**Step-by-step:**

1. Go to your Google Account: [myaccount.google.com](https://myaccount.google.com)
2. Click **Security** in the left sidebar
3. Under *"How you sign in to Google"*, ensure **2-Step Verification** is **On**
   *(App Passwords are only available when 2FA is enabled)*
4. In the search bar at the top type **"App Passwords"** and click the result
5. Under *"Select app"* choose **Mail** (or type a custom name like `VoiceDrop`)
6. Under *"Select device"* choose **Other** and type `VoiceDrop Backend`
7. Click **Generate**
8. Copy the 16-character password shown (e.g., `abcd efgh ijkl mnop`)
9. Paste it into your `.env` as `SMTP_PASS` — **remove the spaces**:
   ```
   SMTP_PASS=abcdefghijklmnop
   ```

### 5. Start the development server

```bash
npm run dev
```

You should see:

```
[VoiceDrop] Server running on port 3000 in development mode
[VoiceDrop] Health check:    http://localhost:3000/health
[VoiceDrop] Upload endpoint: http://localhost:3000/api/upload
```

### 6. Test with curl

**Health check:**

```bash
curl http://localhost:3000/health
```

Expected response:

```json
{ "status": "ok", "timestamp": "2026-06-30T05:00:00.000Z" }
```

**Upload a ZIP file:**

```bash
curl -X POST http://localhost:3000/api/upload \
  -F "name=Test User" \
  -F "timestamp=1751262000000" \
  -F "zip_file=@test.zip"
```

Expected response:

```json
{ "success": true }
```

---

## Deploy to Render

[Render](https://render.com) offers a free tier that runs Node.js web services.

### 1. Push your code to GitHub

Make sure `backend/` (or the whole repo) is pushed to a GitHub repository. Confirm `.env` is in `.gitignore` and **not committed**.

### 2. Create a new Web Service on Render

1. Log in at [render.com](https://render.com) and click **New > Web Service**
2. Connect your GitHub repository
3. Set the **Root Directory** to `backend` (if the repo contains more than just the backend)
4. Configure the service:

| Setting | Value |
|---|---|
| **Environment** | `Node` |
| **Build Command** | `npm install` |
| **Start Command** | `node server.js` |
| **Instance Type** | Free (or paid for always-on) |

### 3. Add Environment Variables

In the Render dashboard > **Environment** tab, add:

| Key | Value |
|---|---|
| `NODE_ENV` | `production` |
| `RECIPIENT_EMAIL` | `your-recipient@example.com` |
| `SMTP_HOST` | `smtp.gmail.com` |
| `SMTP_PORT` | `587` |
| `SMTP_USER` | `your-sender@gmail.com` |
| `SMTP_PASS` | `your-16-char-app-password` |

> **Note:** Do **not** set `PORT` — Render injects it automatically.

### 4. Deploy

Click **Create Web Service**. Render will build and deploy. Your service URL will look like:

```
https://voicedrop-backend.onrender.com
```

> **Warning:** Free-tier Render services **spin down after 15 minutes of inactivity** and take ~30 seconds to cold-start. Use a paid instance or Railway for always-on availability.

### 5. Update Android

Copy your Render URL and paste it into the Android app's `BASE_URL` (see [Android Integration](#android-integration)).

---

## Deploy to Railway

[Railway](https://railway.app) is an alternative PaaS with a more generous free tier and no cold starts on the hobby plan.

### 1. Install the Railway CLI

```bash
npm install -g @railway/cli
```

### 2. Log in and initialize

```bash
railway login
```

Navigate to your `backend/` directory, then:

```bash
railway init
```

Follow the prompts to create a new project.

### 3. Deploy

```bash
railway up
```

Railway will detect Node.js automatically, run `npm install`, and start `node server.js`.

### 4. Set environment variables

In the Railway dashboard:

1. Open your project > **Variables** tab
2. Click **+ New Variable** for each of the following:

| Key | Value |
|---|---|
| `NODE_ENV` | `production` |
| `RECIPIENT_EMAIL` | `your-recipient@example.com` |
| `SMTP_HOST` | `smtp.gmail.com` |
| `SMTP_PORT` | `587` |
| `SMTP_USER` | `your-sender@gmail.com` |
| `SMTP_PASS` | `your-16-char-app-password` |

> **Note:** Railway auto-injects `PORT`. Do not set it manually.

### 5. Get your public URL

In the Railway dashboard > **Settings > Domains**, generate a public URL:

```
https://voicedrop-backend-production.up.railway.app
```

---

## API Reference

### `GET /health`

Returns server health status.

**Response `200 OK`:**

```json
{
  "status": "ok",
  "timestamp": "2026-06-30T05:39:00.000Z"
}
```

---

### `POST /api/upload`

Accepts a voice-recording ZIP archive and delivers it by email.

**Content-Type:** `multipart/form-data`

**Request Fields:**

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | Yes | Sender's name (used in email subject and body) |
| `timestamp` | string | No | Recording time in Unix milliseconds |
| `zip_file` | file (.zip) | Yes | The ZIP archive to deliver. Max 100 MB. |

**Success Response `200 OK`:**

```json
{ "success": true }
```

**Error Responses:**

| HTTP Status | Cause | Example Message |
|---|---|---|
| `400` | `zip_file` field missing | `"Missing required field: zip_file..."` |
| `400` | `name` field missing | `"Missing required field: name."` |
| `400` | Non-ZIP file uploaded | `"Only ZIP files are allowed."` |
| `413` | File exceeds 100 MB | `"File too large. Maximum allowed size is 100 MB."` |
| `500` | Unexpected server error | `"An unexpected error occurred."` |
| `503` | SMTP misconfigured | `"Email service is unavailable."` |

---

## Android Integration

Once your backend is deployed, update the base URL in your Android project.

Open `app/build.gradle.kts` and set the `BASE_URL` build config field to your deployed service URL:

```kotlin
android {
    defaultConfig {
        // Replace with your actual deployed backend URL
        buildConfigField("String", "BASE_URL", "\"https://voicedrop-backend.onrender.com\"")
    }
}
```

Then rebuild the Android project (**Build > Rebuild Project**) so the new URL is picked up at compile time.

> **Tip:** During local development, point the app at your machine using your LAN IP:
> ```kotlin
> buildConfigField("String", "BASE_URL", "\"http://192.168.1.x:3000\"")
> ```
> Find your LAN IP with `ipconfig` (Windows) or `ifconfig` (macOS/Linux).

---

## Security Notes

- **Never commit `.env`** — it is listed in `.gitignore`. Use your hosting provider's environment variables dashboard instead.
- **Use App Passwords** — never put your real Gmail password in `SMTP_PASS`.
- **HTTPS in production** — both Render and Railway terminate TLS for you, so all traffic between the Android app and the server is encrypted in transit.
- **File cleanup** — uploaded ZIPs are deleted from disk immediately after the email is sent (or on any error), so no recordings are stored server-side.
- **File type enforcement** — the upload route rejects non-ZIP files by checking both the MIME type and the file extension.
- **100 MB cap** — enforced by Multer before the file is fully written to disk; large payloads are rejected early.
- **Production error masking** — when `NODE_ENV=production`, internal error messages are replaced with generic responses so stack traces are never exposed to clients.
