'use strict';

const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const { sendEmail } = require('../services/emailSender');

const router = express.Router();

// ─── Uploads directory ────────────────────────────────────────────────────────
const UPLOADS_DIR = path.join(__dirname, '..', '..', 'uploads');

if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

// ─── Multer Storage ───────────────────────────────────────────────────────────
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, UPLOADS_DIR);
  },
  filename: (req, file, cb) => {
    const safeName = file.originalname.replace(/[^a-zA-Z0-9._-]/g, '_');
    cb(null, `${Date.now()}-${safeName}`);
  },
});

// ─── File Filter ──────────────────────────────────────────────────────────────
const fileFilter = (req, file, cb) => {
  const ext = path.extname(file.originalname).toLowerCase();
  const isZipMime =
    file.mimetype === 'application/zip' ||
    file.mimetype === 'application/x-zip-compressed' ||
    file.mimetype === 'application/x-zip' ||
    file.mimetype === 'application/octet-stream';
  const isZipExt = ext === '.zip';

  if (isZipMime && isZipExt) {
    cb(null, true);
  } else {
    cb(new Error('Only ZIP files are allowed'), false);
  }
};

// ─── Multer Instance ──────────────────────────────────────────────────────────
const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 100 * 1024 * 1024, // 100 MB
  },
});

// ─── Helper: delete file silently ─────────────────────────────────────────────
function cleanupFile(filePath) {
  if (!filePath) return;
  fs.unlink(filePath, (err) => {
    if (err && err.code !== 'ENOENT') {
      console.error(`[upload] Failed to delete temp file: ${filePath}`, err.message);
    }
  });
}

// ─── POST /upload ─────────────────────────────────────────────────────────────
router.post('/upload', upload.single('zip_file'), async (req, res, next) => {
  // Validation: zip_file must be present
  if (!req.file) {
    return res.status(400).json({
      success: false,
      message: 'Missing required field: zip_file (must be a .zip file).',
    });
  }

  // Validation: name must be present
  const { name, timestamp } = req.body;

  if (!name || typeof name !== 'string' || name.trim() === '') {
    cleanupFile(req.file.path);
    return res.status(400).json({
      success: false,
      message: 'Missing required field: name.',
    });
  }

  const zipFilePath = req.file.path;
  const zipFileName = req.file.originalname;

  try {
    await sendEmail({
      name: name.trim(),
      timestamp: timestamp || null,
      zipFilePath,
      zipFileName,
    });

    // Async cleanup — don't block the response
    cleanupFile(zipFilePath);

    return res.status(200).json({ success: true });
  } catch (err) {
    // Cleanup on error, then propagate
    cleanupFile(zipFilePath);
    return next(err);
  }
});

module.exports = router;
