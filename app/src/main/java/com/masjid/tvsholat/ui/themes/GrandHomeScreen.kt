package com.masjid.tvsholat.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.domain.model.PrayerTime
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import com.masjid.tvsholat.ui.components.RunningText
import com.masjid.tvsholat.ui.components.QrPanel

@Composable
fun GrandHomeScreen(
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

    // Use ScreenBackground for dynamic user-defined backgrounds
    com.masjid.tvsholat.ui.components.ScreenBackground(
        backgroundUrl = config.backgroundUrl,
        backgroundType = config.backgroundType,
        backgroundLocalPath = config.backgroundLocalPath
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Overlay for Grand Theme (Radial Gradient for focus, but transparent enough for BG)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1A237E).copy(alpha = 0.4f), // Deep Blue Center, semi-transparent
                                Color.Black.copy(alpha = 0.85f)       // Darker Edges for contrast
                            ),
                            radius = 1200f
                        )
                    )
            )

            // 2. Main Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp), // Space for RunningText
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // --- HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Masjid Identity
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = config.name.uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = config.address,
                            color = Color.LightGray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // QR & IP Code (Compact)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Admin Panel",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = deviceIp,
                                color = Color.Yellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        QrPanel(content = "http://$deviceIp:9090", size = 80)
                    }
                }

                // --- CENTER CLOCK ---
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormat.format(now),
                            color = Color.White,
                            fontSize = 130.sp, // Reduced to prevent cutting
                            lineHeight = 130.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-4).sp,
                            modifier = Modifier.alignByBaseline()
                        )
                        Text(
                            text = secondFormat.format(now),
                            color = Color(0xFFFFD54F), // Gold
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alignByBaseline().padding(start = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dateFormat.format(now).uppercase(),
                            color = Color.Cyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "  |  ",
                            color = Color.DarkGray,
                            fontSize = 24.sp
                        )
                        Text(
                            text = hijriFormatted,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light
                        )
                    }

                    // Countdown Box
                    nextPrayer?.let {
                        Spacer(modifier = Modifier.height(32.dp))
                        val diff = it.date.time - now.time
                        val h = (diff / (1000 * 60 * 60)) % 24
                        val m = (diff / (1000 * 60)) % 60
                        val s = (diff / 1000) % 60
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF57F17).copy(alpha = 0.9f))
                                .padding(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "MENUJU ${it.name.uppercase()} - ${String.format("%02d:%02d:%02d", h, m, s)}",
                                color = Color.Black,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // --- PRAYER TIMES ROW ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .height(140.dp), // Fixed height for uniformity
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    prayers.forEach { prayer ->
                        val isNext = prayer == nextPrayer
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isNext) Color.White else Color.White.copy(alpha = 0.1f)
                                )
                                .border(
                                    width = if (isNext) 0.dp else 1.dp, 
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = prayer.name.uppercase(),
                                    color = if (isNext) Color.Black else Color.LightGray,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prayer.time,
                                    color = if (isNext) Color(0xFFD84315) else Color.White, // Deep Orange if selected
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // 3. Running Text (Flush Bottom)
            RunningText(
                text = config.runningText,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
