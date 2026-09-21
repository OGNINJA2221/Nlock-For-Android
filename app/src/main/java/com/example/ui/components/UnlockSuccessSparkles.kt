package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.GlassAccentCyan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class SparkleParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun UnlockSuccessSparkles(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            Color.White,
            GlassAccentCyan,
            Color(0xFF80D8FF),
            Color(0xFFFFD700)
        )
        List(42) {
            SparkleParticle(
                angle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                speed = Random.nextFloat() * 320f + 120f,
                size = Random.nextFloat() * 6f + 3f,
                color = colors.random()
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(750))
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val p = progress.value
        val alpha = (1f - p).coerceIn(0f, 1f)

        for (particle in particles) {
            val dist = particle.speed * p
            val x = center.x + cos(particle.angle) * dist
            val y = center.y + sin(particle.angle) * dist
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size * (1f - p * 0.4f),
                center = Offset(x, y)
            )
        }
    }
}
