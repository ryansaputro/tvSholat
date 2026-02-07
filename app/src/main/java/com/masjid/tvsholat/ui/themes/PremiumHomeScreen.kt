package com.masjid.tvsholat.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.domain.model.PrayerTime
import com.masjid.tvsholat.ui.components.RunningText
import com.masjid.tvsholat.utils.HijriCalendar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PremiumHomeScreen(
    now: Date,
    config: MasjidConfig,
    appVersion: String,
    deviceIp: String,
    prayers: List<PrayerTime>,
    nextPrayer: PrayerTime?
) {
    val hijri = remember(now) { HijriCalendar.toHijri(now) }
    
    // STRUCTURED COLUMN LAYOUT (No Overlaps)
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. TOP BAR: Header & Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Logo & Masjid Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.masjid.tvsholat.ui.components.MasjidLogo(
                    config = config,
                    size = 48.dp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = config.name.uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = config.address,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            // Right: Dual Date Glass Panel
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        1.dp, 
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)), 
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id")).format(now),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${hijri.day} ${hijri.getMonthName()} ${hijri.year} H",
                    color = Color(0xFFFFCC33), // Golden Amber
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 2. CENTER SECTION: Big Clock (Flexible)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Digital Clock
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 60.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm:ss").format(now),
                    color = Color.White,
                    fontSize = 110.sp, // Large but controlled
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // Countdown Badge
            nextPrayer?.let {
                val diff = it.date.time - now.time
                val h = (diff / (1000 * 60 * 60)) % 24
                val m = (diff / (1000 * 60)) % 60
                val s = (diff / 1000) % 60
                val countdown = String.format("%02d:%02d:%02d", h, m, s)
                
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0D47A1).copy(alpha = 0.3f))
                        .border(1.dp, Color(0xFF42A5F5).copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MENUJU ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = it.name.uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = countdown,
                        color = Color(0xFFFFD54F),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        // 3. BOTTOM SECTION: Prayer Times (Always Visible)
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                prayers.forEach { prayer ->
                    val isNext = prayer == nextPrayer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isNext) Color(0xFFFFD54F).copy(alpha = 0.12f) 
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                width = if (isNext) 2.dp else 1.dp,
                                color = if (isNext) Color(0xFFFFD54F).copy(alpha = 0.6f) 
                                        else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = prayer.name,
                                color = if (isNext) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = if (isNext) FontWeight.Black else FontWeight.Bold
                            )
                            Text(
                                text = prayer.time,
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Bottom Info Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 32.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "http://$deviceIp:9090",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(24.dp))
                Box(modifier = Modifier.weight(1f)) {
                    RunningText(text = config.runningText)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "v$appVersion",
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
