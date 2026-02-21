package com.masjid.tvsholat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RamadanDecoration(
    modifier: Modifier = Modifier,
    lanternSize: Dp = 100.dp,
    color: Color = Color(0xFFFFD54F) // Gold
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        // Different variations for variety
        RamadanLantern(size = lanternSize, color = color, animationDelay = 0)
        CrescentMoon(size = lanternSize * 0.8f, color = color)
        RamadanLantern(size = lanternSize * 0.9f, color = color, animationDelay = 1000)
    }
}

@Composable
fun RamadanLantern(
    size: Dp = 100.dp,
    color: Color = Color(0xFFFFD54F),
    animationDelay: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LanternAnimation")
    
    // Swinging Animation with delay
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, delayMillis = animationDelay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Rotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = animationDelay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Canvas(modifier = Modifier.size(size.coerceAtLeast(60.dp) * 1.5f)) {
        val width = this.size.width
        val lanternWidth = size.toPx()
        val lanternHeight = size.toPx() * 1.3f
        val centerX = width / 2
        val topY = 0f

        withTransform({
            rotate(rotation, pivot = Offset(centerX, topY))
        }) {
            // String
            drawLine(
                brush = Brush.verticalGradient(listOf(Color.Gray, Color.DarkGray)),
                start = Offset(centerX, topY),
                end = Offset(centerX, lanternHeight * 0.2f),
                strokeWidth = 2.dp.toPx()
            )

            val bodyTop = lanternHeight * 0.2f
            val bodyHeight = lanternHeight * 0.7f
            val bodyWidth = lanternWidth * 0.8f

            // Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.6f * glowAlpha), Color.Transparent),
                    center = Offset(centerX, bodyTop + bodyHeight / 2),
                    radius = lanternWidth
                )
            )

            // Top Cap (Ornate)
            val capHeight = bodyHeight * 0.25f
            val capPath = Path().apply {
                moveTo(centerX - bodyWidth * 0.45f, bodyTop + capHeight)
                quadraticTo(centerX, bodyTop - 10f, centerX + bodyWidth * 0.45f, bodyTop + capHeight)
                close()
            }
            drawPath(capPath, color.darker(0.2f))

            // Body
            val mainBodyPath = Path().apply {
                moveTo(centerX - bodyWidth * 0.45f, bodyTop + capHeight)
                lineTo(centerX + bodyWidth * 0.45f, bodyTop + capHeight)
                lineTo(centerX + bodyWidth * 0.5f, bodyTop + bodyHeight - capHeight)
                lineTo(centerX - bodyWidth * 0.5f, bodyTop + bodyHeight - capHeight)
                close()
            }
            
            drawPath(
                path = mainBodyPath,
                brush = Brush.verticalGradient(
                    listOf(color.lighter(0.2f), color, color.darker(0.2f))
                )
            )

            // Ornate Windows/Patterns
            val windowWidth = bodyWidth * 0.15f
            for (i in -1..1) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(centerX + (i * bodyWidth * 0.25f) - windowWidth/2, bodyTop + capHeight + 10f),
                    size = Size(windowWidth, bodyHeight - capHeight * 2 - 20f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }

            // Bottom Cap
            val bottomCapPath = Path().apply {
                moveTo(centerX - bodyWidth * 0.5f, bodyTop + bodyHeight - capHeight)
                lineTo(centerX + bodyWidth * 0.5f, bodyTop + bodyHeight - capHeight)
                quadraticTo(centerX, bodyTop + bodyHeight + 10f, centerX - bodyWidth * 0.5f, bodyTop + bodyHeight - capHeight)
                close()
            }
            drawPath(bottomCapPath, color.darker(0.3f))

            // Tassel
            drawLine(
                color = color.darker(0.1f),
                start = Offset(centerX, bodyTop + bodyHeight - 5f),
                end = Offset(centerX, bodyTop + bodyHeight + 20f),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

@Composable
fun CrescentMoon(
    size: Dp = 80.dp,
    color: Color = Color(0xFFFFD54F)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MoonAnimation")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MoonGlow"
    )

    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 2
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2

        // Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.3f * glow), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = r * 1.5f
            )
        )

        val moonPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(centerX - r, centerY - r, centerX + r, centerY + r))
        }
        val cutPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(centerX - r * 0.4f, centerY - r, centerX + r * 1.6f, centerY + r))
        }
        
        val resultPath = Path.combine(PathOperation.Difference, moonPath, cutPath)
        
        drawPath(
            path = resultPath,
            brush = Brush.linearGradient(listOf(color.lighter(0.3f), color, color.darker(0.2f)))
        )
        
        // Small Star next to moon
        drawStar(
            center = Offset(centerX + r * 0.4f, centerY - r * 0.4f),
            radius = r * 0.2f * glow,
            color = Color.White
        )
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        for (i in 0..4) {
            val angle = (i * 144f - 18f) * (PI / 180f).toFloat()
            val x = center.x + radius * kotlin.math.cos(angle)
            val y = center.y + radius * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color)
}

fun Color.lighter(factor: Float = 0.3f): Color = Color(
    red = (red + factor).coerceAtMost(1f),
    green = (green + factor).coerceAtMost(1f),
    blue = (blue + factor).coerceAtMost(1f),
    alpha = alpha
)

fun Color.darker(factor: Float = 0.3f): Color = Color(
    red = (red * (1f - factor)).coerceAtLeast(0f),
    green = (green * (1f - factor)).coerceAtLeast(0f),
    blue = (blue * (1f - factor)).coerceAtLeast(0f),
    alpha = alpha
)
