'use strict';

/**
 * Global Express error-handling middleware (4-argument signature required).
 * Must be registered AFTER all routes.
 */
// eslint-disable-next-line no-unused-vars
function errorHandler(err, req, res, next) {
  console.error(`[errorHandler] ${err.name || 'Error'}: ${err.message}`);
  if (process.env.NODE_ENV !== 'production') {
    console.error(err.stack);
  }

  // ── Multer: file too large ─────────────────────────────────────────────────
  if (err.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({
      success: false,
      message: 'File too large. Maximum allowed size is 100 MB.',
    });
  }

  // ── Multer: unexpected field name ──────────────────────────────────────────
  if (err.code === 'LIMIT_UNEXPECTED_FILE') {
    return res.status(400).json({
      success: false,
      message: 'Unexpected file field. Use the field name "zip_file".',
    });
  }

  // ── File type rejection (from fileFilter) ──────────────────────────────────
  if (err.message === 'Only ZIP files are allowed') {
    return res.status(400).json({
      success: false,
      message: 'Only ZIP files are allowed.',
    });
  }

  // ── SMTP / email configuration errors ─────────────────────────────────────
  if (
    err.message &&
    (err.message.includes('Email service misconfigured') ||
      err.message.includes('Email delivery failed'))
  ) {
    return res.status(503).json({
      success: false,
      message:
        process.env.NODE_ENV === 'production'
          ? 'Email service is unavailable. Please try again later.'
          : err.message,
    });
  }

  // ── Default: Internal Server Error ────────────────────────────────────────
  const statusCode = err.statusCode || err.status || 500;
  const message =
    process.env.NODE_ENV === 'production'
      ? 'An unexpected error occurred. Please try again later.'
      : err.message || 'Internal Server Error';

  return res.status(statusCode).json({
    success: false,
    message,
  });
}

module.exports = errorHandler;
