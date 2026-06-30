package com.example.voicedrop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.voicedrop.ui.main.MainScreen
import com.example.voicedrop.ui.settings.SettingsScreen
import com.example.voicedrop.ui.theme.ElectricViolet
import com.example.voicedrop.ui.theme.VoiceDropTheme

private object Routes {
    const val MAIN     = "main"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceDropTheme {
                VoiceDropApp()
            }
        }
    }
}

@Composable
private fun VoiceDropApp() {
    val context = LocalContext.current

    // ── Permission state ───────────────────────────────────────────────────────
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    // Build the list of permissions to request
    val permissionsToRequest = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] == true
    }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        if (!hasAudioPermission) {
            // ── Permission denied UI ───────────────────────────────────────────
            PermissionDeniedScreen(
                onRequestPermission = { permissionLauncher.launch(permissionsToRequest) }
            )
        } else {
            // ── Main navigation graph ──────────────────────────────────────────
            val navController = rememberNavController()

            NavHost(
                navController    = navController,
                startDestination = Routes.MAIN,
            ) {
                composable(Routes.MAIN) {
                    MainScreen(
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// ─── Permission denied screen ──────────────────────────────────────────────────

@Composable
private fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.MicOff,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text      = "Microphone Permission Required",
                style     = MaterialTheme.typography.headlineMedium,
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text      = "VoiceDrop needs access to your microphone to record audio. Please grant the permission to continue.",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                colors  = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Grant Permission")
            }
        }
    }
}