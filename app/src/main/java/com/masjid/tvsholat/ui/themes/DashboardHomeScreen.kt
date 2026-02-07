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
                    .fillMaxWidth(0.25f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
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
                            text = prayer.name.uppercase(),
                            color = if (isNext) Color(0xFFFFD54F) else Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.masjid.tvsholat.ui.components.MasjidLogo(
                        config = config,
                        size = 64.dp,
                        modifier = Modifier.padding(end = 20.dp)
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = config.name.uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = config.address.uppercase(),
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormat.format(now),
                        color = Color.White,
                        fontSize = 80.sp,
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
                            text = "MENUJU ${it.name.uppercase()} : ${String.format("%02d:%02d:%02d", h, m, s)}",
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
                        size = 120
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        // Anti Burn-in
        RunningText(text = config.runningText)
    }
}
