package com.example.voicedrop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VoiceDropDarkColorScheme = darkColorScheme(
    primary            = ElectricViolet,
    onPrimary          = OnSurface,
    primaryContainer   = SpaceVioletAlt,
    onPrimaryContainer = NeonViolet,
    secondary          = SoftIndigo,
    onSecondary        = OnSurface,
    secondaryContainer = SpaceVioletAlt,
    onSecondaryContainer = LightViolet,
    tertiary           = LightViolet,
    onTertiary         = DeepSpace,
    background         = DeepSpace,
    onBackground       = OnSurface,
    surface            = SpaceViolet,
    onSurface          = OnSurface,
    surfaceVariant     = SpaceVioletAlt,
    onSurfaceVariant   = OnSurfaceVar,
    outline            = Outline,
    outlineVariant     = OutlineDim,
    error              = StatusFailed,
    onError            = OnSurface,
)

@Composable
fun VoiceDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoiceDropDarkColorScheme,
        typography  = VoiceDropTypography,
        content     = content,
    )
}