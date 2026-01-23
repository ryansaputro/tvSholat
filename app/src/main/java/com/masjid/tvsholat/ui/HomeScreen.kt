package com.masjid.tvsholat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.data.MasjidConfigRepository
import com.masjid.tvsholat.domain.model.PrayerTime
import com.masjid.tvsholat.domain.service.PrayerCalculator
import com.masjid.tvsholat.ui.components.ClockHeader
import com.masjid.tvsholat.ui.components.IqomahScreen
import com.masjid.tvsholat.ui.components.ScreenBackground
import kotlinx.coroutines.delay
import java.util.*

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun HomeScreen(repo: MasjidConfigRepository, deviceIp: String) {

    val config by repo.configFlow.collectAsState()
    
    // Function to get "now" with manual offset applied
    fun getAdjustedNow(offsetMinutes: Int): Date {
        val actualNow = Date()
        return if (offsetMinutes == 0) actualNow 
        else Date(actualNow.time + (offsetMinutes * 60 * 1000L))
    }

    var now by remember { mutableStateOf(getAdjustedNow(config.timeOffsetMinutes)) }

    LaunchedEffect(config.timeOffsetMinutes) {
        while (true) {
            delay(1000)
            now = getAdjustedNow(config.timeOffsetMinutes)
        }
    }

    // Recalculate todayKey only when the actual date changes (not every second)
    val todayKey = remember(now.year, now.month, now.date, config.dateOffsetDays) {
        val cal = Calendar.getInstance()
        cal.time = now
        cal.add(Calendar.DAY_OF_YEAR, config.dateOffsetDays)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        "$day-$month-$year"
    }

    val prayers = remember(config.latitude, config.longitude, todayKey) {
        val cal = Calendar.getInstance()
        // Use a clean date for calculation to avoid millisecond sliding in tests
        cal.time = now
        cal.add(Calendar.DAY_OF_YEAR, config.dateOffsetDays)
        
        PrayerCalculator.calculateForDate(
            config.latitude,
            config.longitude,
            cal.time
        )
    }

    val nextPrayer = prayers.firstOrNull { it.date.after(now) }
    
    // Logic Iqomah: Cek apakah lagi dalam masa tunggu sholat
    val currentPrayerInIqomah = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val iqomahEnd = prayerStart + (config.iqomahMinutes * 60 * 1000)
        now.time in prayerStart until iqomahEnd && 
        prayer.name != "Imsak" && prayer.name != "Syuruq"
    }

    ScreenBackground(backgroundUrl = config.backgroundUrl) {
        if (currentPrayerInIqomah != null) {
            val prayerStart = currentPrayerInIqomah.date.time
            val iqomahEnd = prayerStart + (config.iqomahMinutes * 60 * 1000)
            val remainingMillis = iqomahEnd - now.time

            IqomahScreen(
                prayerName = currentPrayerInIqomah.name,
                timeLeftMillis = remainingMillis
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            text = "Menuju ${it.name}   - $countdown",
                            color = Color(0xFFFFD54F),
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
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prayer.name, 
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

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(config.name, color = Color.LightGray, fontSize = 16.sp)
                    Text(config.address, color = Color.Gray, fontSize = 12.sp)
                    Text(
                        text = "Panel Admin: http://$deviceIp:9090",
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
