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
import java.text.SimpleDateFormat
import java.util.*

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.masjid.tvsholat.ui.components.RunningText

@Composable
fun DashboardHomeScreen(
    now: Date,
    config: MasjidConfig,
    appVersion: String,
    deviceIp: String,
    prayers: List<PrayerTime>,
    nextPrayer: PrayerTime?
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id"))

    // Hijri Logic
    val localDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val hijri = HijrahDate.from(localDate)
    val hijriFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    val hijriFormatted = hijri.format(hijriFormatter) + " H"

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            // Sidebar - Prayer Times
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                prayers.forEach { prayer ->
                    val isNext = prayer == nextPrayer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNext) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = prayer.name,
                            color = if (isNext) Color(0xFFFFD54F) else Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.Normal
                        )
                        Text(
                            text = prayer.time,
                            color = if (isNext) Color.White else Color.Gray,
                            fontSize = 20.sp,
                            fontWeight = if (isNext) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }

            // Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = config.name,
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = config.address,
                        color = Color.LightGray,
                        fontSize = 18.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormat.format(now),
                        color = Color.White,
                        fontSize = 100.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = dateFormat.format(now),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = hijriFormatted,
                        color = Color(0xFFFFD54F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                nextPrayer?.let {
                    val diff = it.date.time - now.time
                    val h = (diff / (1000 * 60 * 60)) % 24
                    val m = (diff / (1000 * 60)) % 60
                    val s = (diff / 1000) % 60
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "Menuju ${it.name} : ${String.format("%02d:%02d:%02d", h, m, s)}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Panel Admin: http://$deviceIp:9090" + 
                               if (config.lastUpdated.isNotEmpty()) " (Update: ${config.lastUpdated})" else "",
                        color = Color.DarkGray,
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    com.masjid.tvsholat.ui.components.QrPanel(
                        content = "http://$deviceIp:9090",
                        size = 35
                    )
                }
            }
        }
        // Anti Burn-in
        RunningText(text = config.runningText)
    }
}
