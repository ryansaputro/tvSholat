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
import com.masjid.tvsholat.ui.components.ClockHeader
import com.masjid.tvsholat.ui.components.RunningText
import java.util.*

@Composable
fun SimpleHomeScreen(
    now: Date,
    config: MasjidConfig,
    appVersion: String,
    deviceIp: String,
    prayers: List<PrayerTime>,
    nextPrayer: PrayerTime?
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                ClockHeader(now)
                nextPrayer?.let {
                    val diff = it.date.time - now.time
                    val h = (diff / (1000 * 60 * 60)) % 24
                    val m = (diff / (1000 * 60)) % 60
                    val s = (diff / 1000) % 60
                    val countdown = String.format("%02d:%02d:%02d", h, m, s)
                    
                    Text(
                        text = "MENUJU ${it.name}   - $countdown",
                        color = Color(0xFF03A9F4), // Light Blue/Cyan distinct from Gold
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                prayers.forEach { prayer ->
                    val isNext = prayer == nextPrayer
                    val bgColor = if (isNext) Color.White.copy(alpha = 0.15f) else Color.Transparent
                    val textColor = if (isNext) Color(0xFFFFD54F) else Color.White
                    val fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.Normal

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prayer.name.uppercase(), 
                            fontSize = if (isNext) 24.sp else 18.sp, 
                            color = textColor,
                            fontWeight = fontWeight
                        )
                        Text(
                            text = prayer.time, 
                            fontSize = if (isNext) 28.sp else 22.sp, 
                            color = textColor,
                            fontWeight = fontWeight
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${config.name.uppercase()} (v$appVersion)", 
                        color = Color.LightGray, 
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${config.address} | Admin: http://$deviceIp:9090", 
                        color = Color.Gray, 
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (config.lastUpdated.isNotEmpty()) {
                        Text(
                            text = "Update: ${config.lastUpdated}",
                            color = Color.DarkGray,
                            fontSize = 10.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                com.masjid.tvsholat.ui.components.QrPanel(
                    content = "http://$deviceIp:9090",
                    size = 120
                )
            }
        }
        
        // Anti Burn-in
        RunningText(text = config.runningText)
    }
}
