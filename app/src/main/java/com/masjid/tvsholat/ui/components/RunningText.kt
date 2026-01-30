package com.masjid.tvsholat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RunningText(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    
    // Kecepatan diatur berdasarkan panjang teks (makin panjang makin lama biar speed stabil)
    val duration = (text.length * 150).coerceAtLeast(10000)
    
    val offsetFraction by infiniteTransition.animateFloat(
        initialValue = 1.2f, 
        targetValue = -1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        // Karena TV layar lebar, kita pake offset yang dikali angka gede biar teksnya lewat
        val xOffset = 1500 * offsetFraction
        
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = xOffset.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}
