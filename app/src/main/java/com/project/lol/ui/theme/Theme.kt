package com.project.lol.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun SpotifyTheme(
    useDynamicColor: Boolean = false,
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            androidx.compose.material3.dynamicDarkColorScheme(context)
        }
        else -> androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFE0E0E0),
            onPrimary = Color(0xFF121212),
            primaryContainer = Color(0xFF2E2E2E),
            onPrimaryContainer = Color(0xFFF5F5F5),
            inversePrimary = Color(0xFF121212),
            secondary = Color(0xFFCCCCCC),
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = Color(0xFF262626),
            onSecondaryContainer = Color(0xFFE0E0E0),
            tertiary = Color(0xFFB0B0B0),
            onTertiary = Color(0xFF181818),
            tertiaryContainer = Color(0xFF202020),
            onTertiaryContainer = Color(0xFFD6D6D6),
            outline = Color(0xFF767676),
            outlineVariant = Color(0xFF444444)
        )
    }

    val colorScheme = if (amoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF0F0F0F),
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerLowest = Color.Black,
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
