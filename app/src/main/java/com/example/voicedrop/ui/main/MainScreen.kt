package com.example.voicedrop.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicedrop.ui.theme.ElectricViolet
import com.example.voicedrop.ui.theme.LightViolet
import com.example.voicedrop.ui.theme.RecordRed
import com.example.voicedrop.ui.theme.RecordRedDim
import com.example.voicedrop.ui.theme.SpaceViolet
import com.example.voicedrop.ui.theme.SpaceVioletAlt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val state           by viewModel.recordingState.collectAsStateWithLifecycle()
    val elapsedSeconds  by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val isSaving        by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Show snackbar messages
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
                        text = "VoiceDrop",
                        style = MaterialTheme.typography.titleLarge,
                        color = LightViolet,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = LightViolet,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceViolet,
                ),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SpaceVioletAlt,
                    contentColor   = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── Status label ─────────────────────────────────────────────────
            StatusLabel(state = state)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Timer display ─────────────────────────────────────────────────
            TimerDisplay(seconds = elapsedSeconds, isActive = state == RecordingState.RECORDING)

            Spacer(modifier = Modifier.height(48.dp))

            // ── Record button ─────────────────────────────────────────────────
            RecordButton(
                state = state,
                onRecord  = viewModel::startRecording,
                onPause   = viewModel::pauseRecording,
                onResume  = viewModel::resumeRecording,
                onStop    = viewModel::stopRecording,
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Save button (visible only after stop) ─────────────────────────
            AnimatedVisibility(
                visible = state == RecordingState.STOPPED,
                enter   = scaleIn() + fadeIn(),
                exit    = scaleOut() + fadeOut(),
            ) {
                Button(
                    onClick  = { showSaveDialog = true },
                    enabled  = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(52.dp),
                    shape  = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = if (isSaving) "Saving…" else "Save Recording",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    // ── Save dialog ───────────────────────────────────────────────────────────
    if (showSaveDialog) {
        SaveRecordingDialog(
            onConfirm = { name ->
                showSaveDialog = false
                viewModel.saveRecording(name)
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

// ─── Timer Display ─────────────────────────────────────────────────────────────

@Composable
private fun TimerDisplay(seconds: Long, isActive: Boolean) {
    val hours   = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs    = seconds % 60
    val timeText = "%02d:%02d:%02d".format(hours, minutes, secs)

    // Subtle pulse on the colon separators when recording is active
    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val colonAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isActive) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "colon_alpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SpaceVioletAlt)
            .border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
        // Render hours:minutes separately from seconds so we can pulse the colons
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "%02d".format(hours),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = ":",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isActive) colonAlpha else 1f),
            )
            Text(
                text  = "%02d".format(minutes),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = ":",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isActive) colonAlpha else 1f),
            )
            Text(
                text  = "%02d".format(secs),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ─── Status Label ──────────────────────────────────────────────────────────────

@Composable
private fun StatusLabel(state: RecordingState) {
    val (label, color) = when (state) {
        RecordingState.IDLE      -> Pair("Ready to Record", MaterialTheme.colorScheme.onSurfaceVariant)
        RecordingState.RECORDING -> Pair("● Recording", RecordRed)
        RecordingState.PAUSED    -> Pair("⏸ Paused", MaterialTheme.colorScheme.tertiary)
        RecordingState.STOPPED   -> Pair("Recording Complete", MaterialTheme.colorScheme.primary)
    }

    Text(
        text      = label,
        style     = MaterialTheme.typography.titleMedium,
        color     = color,
        textAlign = TextAlign.Center,
    )
}

// ─── Record Button ─────────────────────────────────────────────────────────────

@Composable
private fun RecordButton(
    state: RecordingState,
    onRecord: () -> Unit,
    onPause:  () -> Unit,
    onResume: () -> Unit,
    onStop:   () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (state == RecordingState.RECORDING) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Main record / stop button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = when (state) {
                            RecordingState.RECORDING -> listOf(RecordRed, RecordRedDim)
                            RecordingState.STOPPED   -> listOf(ElectricViolet.copy(0.3f), SpaceVioletAlt)
                            else                     -> listOf(RecordRed, RecordRedDim)
                        },
                    ),
                )
                .border(2.dp, RecordRed.copy(alpha = 0.6f), CircleShape),
        ) {
            IconButton(
                onClick = {
                    when (state) {
                        RecordingState.IDLE, RecordingState.STOPPED -> onRecord()
                        RecordingState.RECORDING, RecordingState.PAUSED -> onStop()
                    }
                },
                modifier = Modifier.size(100.dp),
            ) {
                Icon(
                    imageVector = when (state) {
                        RecordingState.RECORDING, RecordingState.PAUSED -> Icons.Default.Stop
                        else -> Icons.Default.Mic
                    },
                    contentDescription = when (state) {
                        RecordingState.RECORDING, RecordingState.PAUSED -> "Stop"
                        else -> "Record"
                    },
                    tint     = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        // Pause / Resume button — only visible while actively recording or paused
        AnimatedVisibility(
            visible = state == RecordingState.RECORDING || state == RecordingState.PAUSED,
            enter   = fadeIn() + scaleIn(),
            exit    = fadeOut() + scaleOut(),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(
                onClick = { if (state == RecordingState.RECORDING) onPause() else onResume() },
                modifier = Modifier.padding(top = 20.dp),
                shape    = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = if (state == RecordingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state == RecordingState.RECORDING) "Pause" else "Resume",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text  = if (state == RecordingState.RECORDING) "Pause" else "Resume",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ─── Save Dialog ───────────────────────────────────────────────────────────────

@Composable
private fun SaveRecordingDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SpaceViolet,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Text(
                text  = "Name your Recording",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                Text(
                    text  = "Enter your name to label this recording.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value           = nameInput,
                    onValueChange   = { nameInput = it; nameError = false },
                    label           = { Text("Your Name") },
                    isError         = nameError,
                    supportingText  = if (nameError) {
                        { Text("Name cannot be empty", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (nameInput.isBlank()) nameError = true
                            else onConfirm(nameInput)
                        },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = ElectricViolet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor    = ElectricViolet,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isBlank()) { nameError = true; return@Button }
                    onConfirm(nameInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
