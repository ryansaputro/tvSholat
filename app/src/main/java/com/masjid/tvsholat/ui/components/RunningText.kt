package com.masjid.tvsholat.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * RunningText component with high-compatibility manual marquee animation.
 * Uses explicit types and state management to ensure smooth compilation and runtime performance.
 */
@Composable
fun RunningText(text: String, modifier: Modifier = Modifier) {
    if (text.isEmpty()) return

    // Using explicit MutableState to bypass 'by' delegate inference issues
    val containerWidth: MutableState<Float> = remember { mutableStateOf(0f) }
    val textWidth: MutableState<Float> = remember { mutableStateOf(0f) }
    
    // Correct factory function call
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(containerWidth.value, textWidth.value, text) {
        if (containerWidth.value > 0f && textWidth.value > 0f) {
            // Speed calculation based on text length and container width
            val durationMillis = (text.length * 200 + containerWidth.value.toInt() / 2).coerceAtLeast(12000)
            
            while (true) {
                offsetX.snapTo(containerWidth.value)
                offsetX.animateTo(
                    targetValue = -textWidth.value,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.Black.copy(alpha = 0.45f))
            .onSizeChanged { size: IntSize -> 
                containerWidth.value = size.width.toFloat() 
            }
            .clipToBounds(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .onSizeChanged { size: IntSize -> 
                    textWidth.value = size.width.toFloat() 
                }
                .graphicsLayer(translationX = offsetX.value),
            maxLines = 1,
            softWrap = false
        )
    }
}
