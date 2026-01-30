package com.masjid.tvsholat.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
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
import java.util.*

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.masjid.tvsholat.ui.components.RunningText

@Composable
fun ClassicHomeScreen(
    now: Date,
    config: MasjidConfig,
    appVersion: String,
    deviceIp: String,
    prayers: List<PrayerTime>,
    nextPrayer: PrayerTime?
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id"))
    
    // Hijri Logic
    val localDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val hijri = HijrahDate.from(localDate)
    val hijriFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    val hijriFormatted = hijri.format(hijriFormatter) + " H"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B5E20), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = config.name, 
                        color = Color.White, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = config.address, 
                        color = Color(0xFFA5D6A7), 
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(timeFormat.format(now), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text(dateFormat.format(now), color = Color.White, fontSize = 14.sp)
                    Text(hijriFormatted, color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(24.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    prayers.forEachIndexed { index, prayer ->
                        val isNext = prayer == nextPrayer
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .background(
                                    if (isNext) Color(0xFFC8E6C9) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    if (isNext) 2.dp else 0.dp,
                                    if (isNext) Color(0xFF2E7D32) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = prayer.name,
                                color = if (isNext) Color(0xFF1B5E20) else Color.DarkGray,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = prayer.time,
                                color = if (isNext) Color(0xFF1B5E20) else Color.Black,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            )
                            
                            if (isNext) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val diff = prayer.date.time - now.time
                                val m = (diff / (1000 * 60)) % 60
                                val s = (diff / 1000) % 60
                                Text(
                                    text = "- ${String.format("%02dm %02ds", m, s)}",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (index < prayers.size - 1) {
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.LightGray))
                        }
                    }
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B5E20), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Panel Admin: http://$deviceIp:9090  |  v$appVersion" + 
                           if (config.lastUpdated.isNotEmpty()) "  |  Update: ${config.lastUpdated}" else "",
                    color = Color(0xFFA5D6A7),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                
                com.masjid.tvsholat.ui.components.QrPanel(
                    content = "http://$deviceIp:9090",
                    size = 32
                )
            }
        }
        // Anti Burn-in
        RunningText(text = config.runningText)
    }
}
