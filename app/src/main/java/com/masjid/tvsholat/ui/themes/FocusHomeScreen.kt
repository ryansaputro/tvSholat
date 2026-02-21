package com.masjid.tvsholat.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.domain.model.PrayerTime
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import java.text.SimpleDateFormat
import java.util.*
import com.masjid.tvsholat.utils.HijriCalendar
import com.masjid.tvsholat.ui.components.RunningText
import com.masjid.tvsholat.ui.components.QrPanel
import com.masjid.tvsholat.ui.components.MosqueSilhouette
import com.masjid.tvsholat.ui.components.RamadanDecoration

@Composable
fun FocusHomeScreen(
    now: Date,
    config: MasjidConfig,
    appVersion: String,
    deviceIp: String,
    prayers: List<PrayerTime>,
    nextPrayer: PrayerTime?,
    runningText: String
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id"))
    
    // Hijri Logic using local utility (API 24 compatible)
    val hijri = HijriCalendar.toHijri(now)
    val hijriFormatted = "${hijri.day} ${hijri.getMonthName()} ${hijri.year} H"

    com.masjid.tvsholat.ui.components.ScreenBackground(
        backgroundUrl = config.backgroundUrl,
        backgroundType = config.backgroundType,
        backgroundLocalPath = config.backgroundLocalPath
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dark overlay for focus
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )


            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // TOP SECTION (Balanced Weight)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.25f)
                        .padding(top = 10.dp, start = 20.dp, end = 20.dp)
                ) {
                    // Ramadan Decoration (Enhanced)
                    if (com.masjid.tvsholat.utils.HijriCalendar.toHijri(now).month == 9) {
                        RamadanDecoration(
                            modifier = Modifier.align(Alignment.TopStart),
                            lanternSize = 60.dp // Slightly smaller
                        )
                        RamadanDecoration(
                            modifier = Modifier.align(Alignment.TopEnd),
                            lanternSize = 60.dp
                        )
                    }

                    // HEADER (Masjid Name & Logo)
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        com.masjid.tvsholat.ui.components.MasjidLogo(
                            config = config,
                            size = 50.dp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Text(
                            text = config.name.uppercase(Locale.getDefault()),
                            color = Color.White,
                            fontSize = 26.sp, // Dikecilin pas permintaan
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = config.address,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // CENTER CLOCK (Balanced Weight)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeFormat.format(now),
                        color = Color.White,
                        fontSize = 110.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color(0xFFFFD54F).copy(alpha = 0.3f),
                                offset = Offset(0f, 0f),
                                blurRadius = 25f
                            )
                        ),
                        letterSpacing = (-2).sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = dateFormat.format(now).uppercase(Locale.getDefault()),
                            color = Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "  |  ",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 18.sp
                        )
                        Text(
                            text = hijriFormatted,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // PRAYER GRID (Optimized Height)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .weight(0.32f)
                        .heightIn(max = 130.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    prayers.forEach { prayer ->
                        // Highlight logic: check by name for next rollover
                        val isNext = prayer.name == nextPrayer?.name
                        val bgColor = if (isNext) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.15f)
                        val textColor = Color.White
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .padding(paddingValues = PaddingValues(top = 8.dp, bottom = 8.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = prayer.name.uppercase(),
                                color = if (isNext) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = prayer.time,
                                color = textColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                
                // Add some space at the very bottom to avoid running text
                Spacer(modifier = Modifier.height(40.dp))
            }

            // QR & IP (Fixed positioning to avoid overlap)
            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.TopEnd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "ADMIN", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text(text = deviceIp, color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    QrPanel(content = "http://$deviceIp:9090", size = 60)
                }
            }

            // Running Text
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                RunningText(text = runningText)
            }
        }
    }
}
