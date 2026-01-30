package com.masjid.tvsholat.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AdzanScreen(prayerName: String) {
    // 🔊 BEEP SOUND LOGIC
    LaunchedEffect(Unit) {
        val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        repeat(7) {
            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 200)
            delay(600)
        }
        toneGen.release()
    }
    val infiniteTransition = rememberInfiniteTransition(label = "Label Adzan Blinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha Adzan Blinking"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "SAATNYA ADZAN",
                color = Color.White.copy(alpha = alpha),
                fontSize = 80.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 90.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = prayerName.uppercase(),
                color = Color(0xFFFFD54F),
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}
