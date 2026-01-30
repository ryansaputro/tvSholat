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
import com.masjid.tvsholat.ui.components.TreasuryScreen
import com.masjid.tvsholat.ui.components.HadithScreen
import com.masjid.tvsholat.ui.themes.*
import kotlinx.coroutines.delay
import java.util.*

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun HomeScreen(repo: MasjidConfigRepository, deviceIp: String, appVersion: String) {

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
    
    // 🔥 TIMING CONFIG
    val adzanPeriodMillis = 30 * 1000L // 30 Detik Adzan
    val blankPeriodMillis = 60 * 1000L // 1 Menit Layar Hitam (Blank)
    val totalDelayMillis = adzanPeriodMillis + blankPeriodMillis

    // 🔥 LOGIC ADZAN: Tampil 30 detik pertama
    val currentPrayerInAdzan = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val adzanEnd = prayerStart + adzanPeriodMillis
        now.time in prayerStart until adzanEnd && 
        prayer.name != "Imsak" && prayer.name != "Syuruq"
    }

    // 🔥 LOGIC BLANK: Tampil 1 menit setelah Adzan (30s - 90s)
    val currentPrayerInBlank = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val blankStart = prayerStart + adzanPeriodMillis
        val blankEnd = prayerStart + totalDelayMillis
        now.time in blankStart until blankEnd && 
        prayer.name != "Imsak" && prayer.name != "Syuruq"
    }

    // 🔥 LOGIC IQOMAH: Jalan seteleh Adzan + Blank beres (setelah 1 menit 30 detik)
    val currentPrayerInIqomah = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val iqomahStart = prayerStart + totalDelayMillis
        val iqomahEnd = iqomahStart + (config.iqomahMinutes * 60 * 1000)
        now.time in iqomahStart until iqomahEnd && 
        prayer.name != "Imsak" && prayer.name != "Syuruq"
    }

    // 🔥 LOGIC TREASURY: Tampil setiap interval (misal tiap 5 menit)
    // Syarat: Interval > 0, Menit habis dibagi interval, dan detik antara 0-15
    val isTreasuryPeriod = config.treasuryDisplayInterval > 0 && 
                          (now.time / 1000 / 60) % config.treasuryDisplayInterval == 0L && 
                          (now.time / 1000 % 60) in 0L until config.treasuryDisplayDuration.toLong()

    // 🔥 LOGIC HADITH: Tampil setiap interval (misal tiap 3 menit)
    // Syarat: Interval > 0, Menit habis dibagi interval, dan detik antara 20-40 (biar ga tabrakan sama Kas)
    val isHadithPeriod = config.hadithDisplayInterval > 0 && 
                        (now.time / 1000 / 60) % config.hadithDisplayInterval == 0L && 
                        (now.time / 1000 % 60) in 30L until (30L + config.hadithDisplayDuration)

    ScreenBackground(backgroundUrl = config.backgroundUrl) {
        if (currentPrayerInAdzan != null) {
            com.masjid.tvsholat.ui.components.AdzanScreen(
                prayerName = currentPrayerInAdzan.name
            )
        } else if (currentPrayerInBlank != null) {
            // 🔥 LAYAR HITAM (BLANK)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        } else if (currentPrayerInIqomah != null) {
            val prayerStart = currentPrayerInIqomah.date.time
            val iqomahStart = prayerStart + totalDelayMillis
            val iqomahEnd = iqomahStart + (config.iqomahMinutes * 60 * 1000)
            val remainingMillis = iqomahEnd - now.time

            IqomahScreen(
                prayerName = currentPrayerInIqomah.name,
                timeLeftMillis = remainingMillis
            )
        } else if (isTreasuryPeriod) {
            // 🔥 LAPORAN KAS
            TreasuryScreen(config = config)
        } else if (isHadithPeriod) {
            // 🔥 HADITS HARIAN
            HadithScreen(config = config, now = now)
        } else {
            // 🔥 PILIH TEMA DISINI
            when (config.themeName) {
                "modern" -> ModernHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer)
                "classic" -> ClassicHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer)
                "dashboard" -> DashboardHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer)
                else -> SimpleHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer)
            }
        }
    }
}
