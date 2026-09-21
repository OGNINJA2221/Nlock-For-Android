package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.AppThemeMode

@Composable
fun BackgroundWrapper(
    themeMode: AppThemeMode = AppThemeMode.DEFAULT_GLASS_BLUE,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071B33),
                        Color(0xFF0B2D59),
                        Color(0xFF041224)
                    )
                )
            )
    ) {
        // Base Wallpaper: attached blue gradient
        Image(
            painter = painterResource(id = R.drawable.nlock_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark Overlay: 40% opacity (within 35%-45% range) to subdue high brightness
        // while preserving the vivid blue appearance and providing high contrast for UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030A18).copy(alpha = 0.40f))
        )

        // Overlay depending on theme
        val overlayBrush = when (themeMode) {
            AppThemeMode.DEFAULT_GLASS_BLUE -> Brush.verticalGradient(
                colors = listOf(
                    Color(0x1A000000),
                    Color(0x44001A33),
                    Color(0x66000D1A)
                )
            )
            AppThemeMode.DARK_GLASS -> Brush.verticalGradient(
                colors = listOf(
                    Color(0x88050914),
                    Color(0xBB030710),
                    Color(0xDD020408)
                )
            )
            AppThemeMode.PURE_WHITE_GLASS -> Brush.verticalGradient(
                colors = listOf(
                    Color(0x66FFFFFF),
                    Color(0x88F0F4F8),
                    Color(0xAAFFFFFF)
                )
            )
            AppThemeMode.AUTO -> Brush.verticalGradient(
                colors = listOf(
                    Color(0x33000000),
                    Color(0x66001020)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )

        content()
    }
}
