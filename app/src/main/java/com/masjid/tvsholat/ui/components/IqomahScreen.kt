package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.*
import android.media.AudioManager
import android.media.ToneGenerator

@Composable
fun IqomahScreen(prayerName: String, timeLeftMillis: Long) {
    val minutes = (timeLeftMillis / 1000) / 60
    val seconds = (timeLeftMillis / 1000) % 60
    
    // Play beep every second during the last 10 seconds
    LaunchedEffect(seconds) {
        if (minutes == 0L && seconds in 1L..10L) {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                // Release after a short delay to avoid memory leaks
                delay(200)
                tg.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val timeFormatted = String.format("%02d:%02d", minutes.coerceAtLeast(0), seconds.coerceAtLeast(0))

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "IQOMAH",
                fontSize = 80.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD54F) // Gold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = prayerName.uppercase(),
                fontSize = 48.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = timeFormatted,
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            if (minutes == 0L && seconds in 0L..10L) {
                Text(
                    text = "LURUSKAN DAN RAPATKAN SHAF",
                    fontSize = 28.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}
