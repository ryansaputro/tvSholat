package com.masjid.tvsholat.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.domain.model.PrayerTime
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

import com.masjid.tvsholat.ui.components.RunningText

@Composable
fun ElegantHomeScreen(
    now: Date,
    config: MasjidConfig,
    appVersion: String,
    deviceIp: String,
    prayers: List<PrayerTime>,
    nextPrayer: PrayerTime?
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val secondFormat = SimpleDateFormat(":ss", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id"))
    
    // Hijri Logic
    val localDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val hijri = HijrahDate.from(localDate)
    val hijriFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    val hijriFormatted = hijri.format(hijriFormatter) + " H"

    Box(modifier = Modifier.fillMaxSize()) {
        // Dark Overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP SECTION: Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = config.name.uppercase(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = config.address,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // IP & Version (Small & Discreet)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "v$appVersion",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Admin: $deviceIp:9090",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp)) // Pushed to absolute top

            // CENTER SECTION: Large Clock
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeFormat.format(now),
                        color = Color.White,
                        fontSize = 110.sp, // Reduced for 32" TV safety
                        fontWeight = FontWeight.Black,
                        lineHeight = 110.sp
                    )
                    Text(
                        text = secondFormat.format(now),
                        color = Color(0xFFFFD54F),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 32.dp, start = 8.dp)
                    )
                }
                
                // Date Section (Stacked but Tight)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateFormat.format(now).uppercase(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = hijriFormatted,
                        color = Color(0xFFFFD54F),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }
                
                // Countdown to Next Prayer (Floating style)
                nextPrayer?.let {
                    val diff = it.date.time - now.time
                    val h = (diff / (1000 * 60 * 60)) % 24
                    val m = (diff / (1000 * 60)) % 60
                    val s = (diff / 1000) % 60
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Menuju ${it.name}: ${String.format("%02d:%02d:%02d", h, m, s)}",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.0f))

            // BOTTOM SECTION: Prayer Cards (Glassmorphism)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                prayers.forEach { prayer ->
                    val isNext = prayer == nextPrayer
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isNext) Color(0xFF2E7D32).copy(alpha = 0.8f) 
                                else Color.White.copy(alpha = 0.2f)
                            )
                            .then(
                                if (isNext) Modifier.border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp))
                                else Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = prayer.name.uppercase(),
                                color = if (isNext) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = prayer.time,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            
        }

        // Running Text at the very bottom
        RunningText(
            text = config.runningText,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
