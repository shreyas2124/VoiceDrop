package com.example.voicedrop.ui.history

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.voicedrop.data.model.RecordingEntry
import com.example.voicedrop.data.model.UploadStatus
import com.example.voicedrop.ui.theme.ElectricViolet
import com.example.voicedrop.ui.theme.SpaceViolet
import com.example.voicedrop.ui.theme.StatusFailed
import com.example.voicedrop.ui.theme.StatusPending
import com.example.voicedrop.ui.theme.StatusSent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingDetailDialog(
    entry:     RecordingEntry,
    onDismiss: () -> Unit,
    onRetry:   () -> Unit,
    onDelete:  () -> Unit,
) {
    val context        = LocalContext.current
    var showDeleteConf by remember { mutableStateOf(false) }

    val dateTimeFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }
    val formattedDate  = remember(entry.dateTimeMillis) { dateTimeFormat.format(Date(entry.dateTimeMillis)) }

    val fileSize = remember(entry.filePath) {
        val f = File(entry.filePath)
        if (f.exists()) formatFileSize(f.length()) else "File not found"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SpaceViolet,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Text(
                text  = entry.userName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                // ── Detail rows ───────────────────────────────────────────────
                DetailRow(label = "Date & Time", value = formattedDate)
                Spacer(Modifier.height(6.dp))
                DetailRow(label = "File", value = entry.fileName)
                Spacer(Modifier.height(6.dp))
                DetailRow(label = "Size", value = fileSize)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                // ── Status chip ───────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(80.dp),
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (entry.uploadStatus) {
                            UploadStatus.SENT    -> StatusSent.copy(alpha = 0.15f)
                            UploadStatus.PENDING -> StatusPending.copy(alpha = 0.15f)
                            UploadStatus.FAILED  -> StatusFailed.copy(alpha = 0.15f)
                        },
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = when (entry.uploadStatus) {
                                    UploadStatus.SENT    -> Icons.Default.CheckCircle
                                    UploadStatus.PENDING -> Icons.Default.HourglassTop
                                    UploadStatus.FAILED  -> Icons.Default.ErrorOutline
                                },
                                contentDescription = null,
                                tint = when (entry.uploadStatus) {
                                    UploadStatus.SENT    -> StatusSent
                                    UploadStatus.PENDING -> StatusPending
                                    UploadStatus.FAILED  -> StatusFailed
                                },
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text  = entry.uploadStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = when (entry.uploadStatus) {
                                    UploadStatus.SENT    -> StatusSent
                                    UploadStatus.PENDING -> StatusPending
                                    UploadStatus.FAILED  -> StatusFailed
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Action buttons ────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Play button
                    OutlinedButton(
                        onClick = {
                            val file = File(entry.filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file,
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "audio/mp4")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Play with…"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Play Recording")
                    }

                    // Retry button — only for failed uploads
                    if (entry.uploadStatus == UploadStatus.FAILED) {
                        Button(
                            onClick  = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry Upload")
                        }
                    }

                    // Delete button
                    OutlinedButton(
                        onClick  = { showDeleteConf = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusFailed),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )

    // ── Delete confirmation ────────────────────────────────────────────────────
    if (showDeleteConf) {
        AlertDialog(
            onDismissRequest = { showDeleteConf = false },
            containerColor   = SpaceViolet,
            shape            = RoundedCornerShape(24.dp),
            title = { Text("Delete Recording?") },
            text  = {
                Text(
                    text  = "This will permanently delete the audio file and its ZIP. Cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteConf = false; onDelete() },
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusFailed),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConf = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024     -> "%.1f KB".format(bytes / 1_024.0)
    else               -> "$bytes B"
}
