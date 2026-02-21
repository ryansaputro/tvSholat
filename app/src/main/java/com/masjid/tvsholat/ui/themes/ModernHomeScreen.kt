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
import com.masjid.tvsholat.utils.HijriCalendar
import java.util.*

import com.masjid.tvsholat.ui.components.RunningText

@Composable
fun ModernHomeScreen(
    now: Date,
    config: MasjidConfig,
    appVersion: String,
    deviceIp: String,
    prayers: List<PrayerTime>,
    nextPrayer: PrayerTime?,
    runningText: String
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id"))
    
    // Hijri Logic using local utility (API 24 compatible)
    val hijri = HijriCalendar.toHijri(now)
    val hijriFormatted = "${hijri.day} ${hijri.getMonthName()} ${hijri.year} H"

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            ) {
                // LEFT SIDE: Clock and Info
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeFormat.format(now),
                        color = Color.White,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = dateFormat.format(now),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = hijriFormatted,
                        color = Color(0xFFFFD54F),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    nextPrayer?.let {
                        val diff = it.date.time - now.time
                        val h = (diff / (1000 * 60 * 60)) % 24
                        val m = (diff / (1000 * 60)) % 60
                        val s = (diff / 1000) % 60
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1976D2).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFF42A5F5), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${it.name.uppercase()}  -  ${String.format("%02d:%02d:%02d", h, m, s)}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.masjid.tvsholat.ui.components.MasjidLogo(
                            config = config,
                            size = 50.dp,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = config.name.uppercase(), 
                                color = Color.White, 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = config.address, 
                                color = Color.LightGray, 
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (config.lastUpdated.isNotEmpty()) {
                                Text(
                                    text = "Admin Updated: ${config.lastUpdated}",
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                               )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        com.masjid.tvsholat.ui.components.QrPanel(
                            content = "http://$deviceIp:9090",
                            size = 120
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // RIGHT SIDE: Prayer Times
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .padding(start = 24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    prayers.forEach { prayer ->
                        val isNext = prayer.name == nextPrayer?.name
                        val glowColor = if (isNext) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.7f)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isNext) Color.White.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .then(
                                    if (isNext) Modifier.border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prayer.name.uppercase(),
                                color = glowColor,
                                fontSize = if (isNext) 18.sp else 15.sp, // Slightly smaller
                                fontWeight = if (isNext) FontWeight.Black else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = prayer.time,
                                color = glowColor,
                                fontSize = if (isNext) 22.sp else 17.sp, // Slightly smaller
                                fontWeight = if (isNext) FontWeight.Black else FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
            // Anti Burn-in
            RunningText(text = runningText)
        }
    }
}
