package com.example.voicedrop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicedrop.data.model.RecordingEntry
import com.example.voicedrop.data.model.UploadStatus
import com.example.voicedrop.ui.history.RecordingDetailDialog
import com.example.voicedrop.ui.theme.ElectricViolet
import com.example.voicedrop.ui.theme.LightViolet
import com.example.voicedrop.ui.theme.SpaceViolet
import com.example.voicedrop.ui.theme.SpaceVioletAlt
import com.example.voicedrop.ui.theme.StatusFailed
import com.example.voicedrop.ui.theme.StatusPending
import com.example.voicedrop.ui.theme.StatusSent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val recordings      by viewModel.recordings.collectAsStateWithLifecycle(emptyList())
    val pendingCount    by viewModel.pendingCount.collectAsStateWithLifecycle(0)
    val failedCount     by viewModel.failedCount.collectAsStateWithLifecycle(0)
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isLoading       by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedEntry by remember { mutableStateOf<RecordingEntry?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = LightViolet,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = LightViolet,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceViolet),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = SpaceVioletAlt,
                    contentColor   = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            // ── Section: Upload Status ──────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader(title = "Upload Status")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = SpaceViolet),
                ) {
                    Column {
                        InfoRow(
                            icon  = Icons.Default.HourglassTop,
                            tint  = StatusPending,
                            label = "Pending Uploads",
                            value = pendingCount.toString(),
                        )
                        HorizontalDivider(color = SpaceVioletAlt, thickness = 1.dp)
                        SettingsActionRow(
                            icon    = Icons.Default.Refresh,
                            tint    = if (failedCount > 0) StatusFailed else MaterialTheme.colorScheme.onSurfaceVariant,
                            label   = "Retry Failed Uploads",
                            badge   = if (failedCount > 0) failedCount.toString() else null,
                            enabled = failedCount > 0 && !isLoading,
                            onClick = { viewModel.retryAllFailed() },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Section: Recording History ──────────────────────────────────
            item {
                SectionHeader(title = "Recording History")
            }

            if (recordings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = SpaceViolet),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector        = Icons.Default.History,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier           = Modifier.size(40.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text  = "No recordings yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = SpaceViolet),
                    ) {
                        Column {
                            recordings.forEachIndexed { index, entry ->
                                RecordingHistoryRow(
                                    entry   = entry,
                                    onClick = { selectedEntry = entry },
                                )
                                if (index < recordings.lastIndex) {
                                    HorizontalDivider(color = SpaceVioletAlt, thickness = 1.dp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Section: Manage ─────────────────────────────────────────────
            item {
                SectionHeader(title = "Manage")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = SpaceViolet),
                ) {
                    Column {
                        SettingsActionRow(
                            icon    = Icons.Default.Delete,
                            tint    = StatusFailed,
                            label   = "Clear History",
                            enabled = recordings.isNotEmpty() && !isLoading,
                            onClick = { showClearDialog = true },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Section: About ──────────────────────────────────────────────
            item {
                SectionHeader(title = "About")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = SpaceViolet),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Default.Info,
                                contentDescription = null,
                                tint               = ElectricViolet,
                                modifier           = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text  = "VoiceDrop",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text  = "Version 1.0 — Secure voice recording and delivery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ElectricViolet)
            }
        }
    }

    // ── Detail dialog ─────────────────────────────────────────────────────────
    selectedEntry?.let { entry ->
        RecordingDetailDialog(
            entry       = entry,
            onDismiss   = { selectedEntry = null },
            onRetry     = { viewModel.retryUpload(entry.id); selectedEntry = null },
            onDelete    = { viewModel.deleteRecording(entry); selectedEntry = null },
        )
    }

    // ── Clear history confirmation ─────────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor   = SpaceViolet,
            shape            = RoundedCornerShape(24.dp),
            title            = { Text("Clear History?", style = MaterialTheme.typography.titleLarge) },
            text             = {
                Text(
                    text  = "This will permanently delete all recordings and their files. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { showClearDialog = false; viewModel.clearHistory() },
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusFailed),
                ) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

// ─── Composable helpers ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun InfoRow(
    icon:  ImageVector,
    tint:  androidx.compose.ui.graphics.Color,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text  = value,
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon:    ImageVector,
    tint:    androidx.compose.ui.graphics.Color,
    label:   String,
    badge:   String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint     = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(0.4f),
            )
        }
        if (badge != null) {
            Surface(
                shape = CircleShape,
                color = StatusFailed,
            ) {
                Text(
                    text     = badge,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordingHistoryRow(
    entry:   RecordingEntry,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val date = remember(entry.dateTimeMillis) { Date(entry.dateTimeMillis) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    when (entry.uploadStatus) {
                        UploadStatus.SENT    -> StatusSent
                        UploadStatus.PENDING -> StatusPending
                        UploadStatus.FAILED  -> StatusFailed
                    }
                ),
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = entry.userName,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = "${dateFormat.format(date)} · ${timeFormat.format(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Status badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = when (entry.uploadStatus) {
                UploadStatus.SENT    -> StatusSent.copy(alpha = 0.15f)
                UploadStatus.PENDING -> StatusPending.copy(alpha = 0.15f)
                UploadStatus.FAILED  -> StatusFailed.copy(alpha = 0.15f)
            },
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = when (entry.uploadStatus) {
                        UploadStatus.SENT    -> Icons.Default.CheckCircle
                        UploadStatus.PENDING -> Icons.Default.HourglassTop
                        UploadStatus.FAILED  -> Icons.Default.ErrorOutline
                    },
                    contentDescription = null,
                    tint     = when (entry.uploadStatus) {
                        UploadStatus.SENT    -> StatusSent
                        UploadStatus.PENDING -> StatusPending
                        UploadStatus.FAILED  -> StatusFailed
                    },
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text  = entry.uploadStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (entry.uploadStatus) {
                        UploadStatus.SENT    -> StatusSent
                        UploadStatus.PENDING -> StatusPending
                        UploadStatus.FAILED  -> StatusFailed
                    },
                )
            }
        }
    }
}
