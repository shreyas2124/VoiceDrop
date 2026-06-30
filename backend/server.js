'use strict';

require('dotenv').config();

const express = require('express');
const morgan = require('morgan');

const uploadRouter = require('./src/routes/upload');
const errorHandler = require('./src/middleware/errorHandler');

const app = express();
const PORT = process.env.PORT || 3000;

// ─── Middleware ───────────────────────────────────────────────────────────────
app.use(morgan('combined'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ─── Health Check ─────────────────────────────────────────────────────────────
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
  });
});

// ─── Routes ───────────────────────────────────────────────────────────────────
app.use('/api', uploadRouter);

// ─── 404 Handler ──────────────────────────────────────────────────────────────
app.use((req, res) => {
  res.status(404).json({ success: false, message: 'Route not found.' });
});

// ─── Error Handler ────────────────────────────────────────────────────────────
app.use(errorHandler);

// ─── Start Server ─────────────────────────────────────────────────────────────
app.listen(PORT, () => {
  console.log(`[VoiceDrop] Server running on port ${PORT} in ${process.env.NODE_ENV || 'development'} mode`);
  console.log(`[VoiceDrop] Health check: http://localhost:${PORT}/health`);
  console.log(`[VoiceDrop] Upload endpoint: http://localhost:${PORT}/api/upload`);
});

module.exports = app;
