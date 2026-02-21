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
import com.masjid.tvsholat.ui.components.TarhimScreen
import com.masjid.tvsholat.ui.themes.*
import kotlinx.coroutines.delay
import java.util.*
import com.masjid.tvsholat.utils.HijriCalendar
import com.masjid.tvsholat.utils.IslamicEvent

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun HomeScreen(repo: MasjidConfigRepository, deviceIp: String, appVersion: String) {

    val config by repo.configFlow.collectAsState()
    
    // Function to get "now" with manual offset applied
    fun getAdjustedNow(offsetMinutes: Int): Date {
        return com.masjid.tvsholat.data.TimeRepository.getCurrentTime(offsetMinutes)
    }

    var now by remember { mutableStateOf(getAdjustedNow(config.timeOffsetMinutes)) }

    LaunchedEffect(config.timeOffsetMinutes) {
        while (true) {
            delay(100) // Update faster to reflect smooth seconds if needed, or stick to 1s
            now = getAdjustedNow(config.timeOffsetMinutes)
        }
    }

    // Recalculate todayKey correctly at LOCAL midnight
    val todayKey = remember(Calendar.getInstance().apply { time = now }.get(Calendar.DAY_OF_YEAR), config.dateOffsetDays) {
        val cal = Calendar.getInstance()
        cal.time = now
        cal.add(Calendar.DAY_OF_YEAR, config.dateOffsetDays)
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH)
        val y = cal.get(Calendar.YEAR)
        "$d-$m-$y"
    }

    val prayers = remember(config.latitude, config.longitude, todayKey) {
        val cal = Calendar.getInstance()
        cal.time = now
        cal.add(Calendar.DAY_OF_YEAR, config.dateOffsetDays)
        
        PrayerCalculator.calculateForDate(
            config.latitude,
            config.longitude,
            cal.time
        )
    }

    val nextPrayer = prayers.firstOrNull { it.date.after(now) } ?: remember(todayKey) {
        // Jika jadwal hari ini sudah lewat semua (Isya sudah lewat), 
        // ancang-ancang ambil Subuh/Imsak BESOK buat highlight.
        val tomorrow = Calendar.getInstance()
        tomorrow.time = now
        tomorrow.add(Calendar.DAY_OF_YEAR, config.dateOffsetDays + 1)
        
        val tomorrowPrayers = PrayerCalculator.calculateForDate(
            config.latitude,
            config.longitude,
            tomorrow.time
        )
        tomorrowPrayers.firstOrNull()
    }
    
    // 🔥 TIMING CONFIG
    val adzanPeriodMillis = 30 * 1000L // 30 Detik Adzan
    val blankPeriodMillis = 60 * 1000L // 1 Menit Layar Hitam (Blank)
    val totalDelayMillis = adzanPeriodMillis + blankPeriodMillis

    // 🔥 LOGIC ADZAN: Tampil 30 detik pertama
    val currentPrayerInAdzan = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val adzanEnd = prayerStart + adzanPeriodMillis
        now.time in prayerStart until adzanEnd && 
        prayer.name != "IMSAK" && prayer.name != "TERBIT"
    }

    // 🔥 LOGIC BLANK: Tampil 1 menit setelah Adzan (30s - 90s)
    val currentPrayerInBlank = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val blankStart = prayerStart + adzanPeriodMillis
        val blankEnd = prayerStart + totalDelayMillis
        now.time in blankStart until blankEnd && 
        prayer.name != "IMSAK" && prayer.name != "TERBIT"
    }

    // 🔥 HELPER: Get Iqomah Minutes for specific prayer
    fun getIqomahMinutes(prayerName: String): Int {
        return when (prayerName) {
            "SUBUH" -> config.iqomahSubuh
            "DZUHUR" -> config.iqomahDzuhur
            "ASHAR" -> config.iqomahAshar
            "MAGHRIB" -> config.iqomahMaghrib
            "ISYA" -> config.iqomahIsya
            "JUM'AT" -> config.iqomahJumat
            else -> config.iqomahMinutes
        }
    }

    // 🔥 LOGIC IQOMAH: Jalan seteleh Adzan + Blank beres (setelah 1 menit 30 detik)
    val currentPrayerInIqomah = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val iqomahStart = prayerStart + totalDelayMillis
        val mins = getIqomahMinutes(prayer.name)
        val iqomahEnd = iqomahStart + (mins * 60 * 1000)
        
        now.time in iqomahStart until iqomahEnd && 
        prayer.name != "IMSAK" && prayer.name != "TERBIT" &&
        !(prayer.name == "JUM'AT" && mins <= 0) // Jika Jumat diset 0, lompati iqomah
    }
    
    // 🔥 LOGIC SHOLAT: Tampil setelah Iqomah selesai (Layar Hitam Standby)
    val currentPrayerInSholat = prayers.firstOrNull { prayer ->
        val prayerStart = prayer.date.time
        val iqomahStart = prayerStart + totalDelayMillis
        val mins = getIqomahMinutes(prayer.name)
        val iqomahEnd = iqomahStart + (mins * 60 * 1000)
        val sholatEnd = iqomahEnd + (config.sholatDurationMinutes * 60 * 1000)
        
        now.time in iqomahEnd until sholatEnd &&
        prayer.name != "IMSAK" && prayer.name != "TERBIT"
    }

    // 🔥 HELPER: Apakah sedang ada event sholat (Adzan/Iqomah/Sholat)?
    val isPrayerEventActive = currentPrayerInAdzan != null || 
                             currentPrayerInBlank != null || 
                             currentPrayerInIqomah != null || 
                             currentPrayerInSholat != null

    // 🔥 LOGIC NEAR ADZAN: Suppressed 5 menit sebelum Adzan
    val isNearAdzan = prayers.any { prayer ->
        val diff = prayer.date.time - now.time
        diff in 0L until (5 * 60 * 1000L) && 
        prayer.name != "IMSAK" && prayer.name != "TERBIT"
    }

    // 🔥 LOGIC IMSAK/TARHIM: Tampil saat masa Imsak sampai Subuh
    val isImsakPeriod = prayers.any { prayer ->
        if (prayer.name == "IMSAK") {
            val subuh = prayers.find { it.name == "SUBUH" }
            if (subuh != null) {
                // Tepat dari waktu Imsak sampai sebelum waktu Subuh
                now.time in prayer.date.time until subuh.date.time
            } else false
        } else false
    }

    // Flag untuk menentukan apakah boleh menampilkan konten rotasi (Hadits/Kas/Info)
    // Blokir rotasi jika: sedang Adzan/Iqomah/Sholat, sedang dekat Adzan, atau sedang masa Imsak
    val canShowRotation = !isPrayerEventActive && !isNearAdzan && !isImsakPeriod

    // 🔥 LOGIC TREASURY: Tampil setiap interval (misal tiap 5 menit)
    val isTreasuryPeriod = canShowRotation &&
                          config.treasuryDisplayInterval > 0 && 
                          (now.time / 1000 / 60) % config.treasuryDisplayInterval == 0L && 
                          (now.time / 1000 % 60) in 0L until config.treasuryDisplayDuration.toLong()

    // 🔥 LOGIC HADITH: Tampil setiap interval
    val isHadithPeriod = canShowRotation &&
                        config.hadithDisplayInterval > 0 && 
                        (now.time / 1000 / 60) % config.hadithDisplayInterval == 0L && 
                        (now.time / 1000 % 60) in 30L until (30L + config.hadithDisplayDuration)

    // 🔥 LOGIC INFO BOARD: Tampil setiap interval
    val currentSecond = (now.time / 1000 % 60)
    val infoItemCount = if (config.infoItems.isNotEmpty()) config.infoItems.size else 1
    // 🔥 FIX: Duration is TOTAL time for all items combined, not per item
    // User wants: "kalo diset 15 detik tampilnya ini dibagi 3 aja bro"
    val totalInfoDuration = config.infoDisplayDuration.toLong()
    val infoEndSecond = 45L + totalInfoDuration
    
    val isInfoPeriod = if (canShowRotation && config.infoDisplayInterval > 0) {
        val minuteNum = now.time / 1000 / 60
        val isInfoMinute = (minuteNum % config.infoDisplayInterval == 0L)
        val isPrevInfoMinute = ((minuteNum - 1) % config.infoDisplayInterval == 0L)
        
        if (infoEndSecond <= 60) {
            isInfoMinute && currentSecond in 45L until infoEndSecond
        } else {
            (isInfoMinute && currentSecond >= 45L) || (isPrevInfoMinute && currentSecond < (infoEndSecond % 60))
        }
    } else {
        false
    }
    // 🔥 LOGIC ISLAMIC EVENT (H-1 & Hari H)
    // Cek apakah hari ini atau besok ada event penting
    val todayEvent = remember(todayKey) { HijriCalendar.getEvent(now) }
    val upcomingEvent = remember(todayKey) { HijriCalendar.getUpcomingEvent(now) }
    
    // Tampilkan event jika ada. Durasi: 15 detik per putaran
    // Prioritas: Hari H > H-1
    val activeEvent = todayEvent ?: upcomingEvent
    val isEventTomorrow = (todayEvent == null && upcomingEvent != null)
    

    
    // REVISED SCHEDULE if Event exists:

    // Treasury: 0-10s
    // Event: 10-25s
    // Hadith: 25-40s
    // Info: 40s+
    
    // Let's use a dedicated slot if event exists.
    val showEventScreen = canShowRotation && activeEvent != null && 
                          (now.time / 1000 % 60) in 0L until 20L // Dedicated 20s slot for Islamic Events

    // --- MODE HEMAT DAYA (STANDBY) ---
    val isDisplayActive = remember(now, prayers, config.enablePowerSaving, config.powerSavingPreMinutes, config.powerSavingPostMinutes, config.enableTarhim, config.keepAwakeOnInternet) {
        // 🔥 WAKE ON ADMIN ACTIVITY (Stay awake for 5 minutes after last admin request)
        val adminActiveDuration = 5 * 60 * 1000L
        if (System.currentTimeMillis() - com.masjid.tvsholat.data.TimeRepository.lastAdminActivityTime < adminActiveDuration) {
            return@remember true
        }

        // 🔥 WAKE ON INTERNET (Always-on if connected & enabled)
        if (config.keepAwakeOnInternet) {
            val currentIp = com.masjid.tvsholat.utils.NetworkUtils.getLocalIpAddress()
            if (currentIp != "Unknown IP" && currentIp.isNotEmpty()) {
                return@remember true
            }
        }

        if (!config.enablePowerSaving) return@remember true
        
        prayers.any { prayer ->
            val prayerTime = prayer.date.time
            val preDuration = config.powerSavingPreMinutes * 60 * 1000L
            val postDuration = config.powerSavingPostMinutes * 60 * 1000L
            
            when (prayer.name) {
                "MAGHRIB" -> {
                    // Maghrib starts [preDuration] before and stays on until Isya + [postDuration]
                    val isya = prayers.find { it.name == "ISYA" }
                    val displayStart = prayerTime - preDuration
                    val displayEnd = (isya?.date?.time ?: prayerTime) + postDuration
                    now.time in displayStart until displayEnd
                }
                "ISYA" -> {
                    // Already handled by Maghrib block for continuity, but keep for safety
                    val displayStart = prayerTime - preDuration
                    val displayEnd = prayerTime + postDuration
                    now.time in displayStart until displayEnd
                }
                "IMSAK" -> {
                    // Selalu nyalakan layar pas masa Imsak (menuju Subuh)
                    val subuh = prayers.find { it.name == "SUBUH" }
                    now.time in prayerTime until (subuh?.date?.time ?: prayerTime)
                }
                "TERBIT" -> false // Sunrise doesn't trigger wake
                else -> {
                    // SUBUH, DZUHUR, ASHAR
                    now.time in (prayerTime - preDuration) until (prayerTime + postDuration)
                }
            }
        }
    }
    
    // --- BRIGHTNESS CONTROL ---
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(isDisplayActive, currentPrayerInBlank, currentPrayerInSholat) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            val isOffMode = (!isDisplayActive && config.enablePowerSaving) || 
                            currentPrayerInBlank != null || 
                            currentPrayerInSholat != null
            
            val params = window.attributes
            params.screenBrightness = if (isOffMode) 0.01f else -1.0f 
            window.attributes = params
        }
    }

    if (config.enablePowerSaving && !isDisplayActive) {
        // 🔥 TV STANDBY (Layar Hitam Total)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    } else {
        ScreenBackground(
            backgroundUrl = config.backgroundUrl,
            backgroundType = config.backgroundType,
            backgroundLocalPath = config.backgroundLocalPath
        ) {
                // 🔥 GABUNG RUNNING TEXT DENGAN PERINGATAN JAM (Jika Ngaco)
                val baseRunningText = config.runningText
                val finalRunningText = if (com.masjid.tvsholat.data.TimeRepository.isTimeUnreliable) {
                    "⚠️ PERINGATAN: Jam TV mungkin tidak akurat karena mati listrik. Mohon periksa atau sinkronkan ulang! | $baseRunningText"
                } else {
                    baseRunningText
                }

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
                    val mins = getIqomahMinutes(currentPrayerInIqomah.name)
                    val iqomahEnd = iqomahStart + (mins * 60 * 1000)
                    val remainingMillis = iqomahEnd - now.time

                    IqomahScreen(
                        prayerName = currentPrayerInIqomah.name,
                        timeLeftMillis = remainingMillis
                    )
                } else if (currentPrayerInSholat != null) {
                    // 🔥 LAYAR HITAM SAAT SHOLAT
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                } else if (isImsakPeriod) {
                    // 🔥 VISUAL IMSAK (Countdown ke Subuh + Sholawat jika aktif)
                    val subuh = prayers.find { it.name == "SUBUH" }
                    TarhimScreen(config = config, subuhTime = subuh?.date, now = now, runningText = finalRunningText)
                } else if (showEventScreen) {
                    // 🔥 PERINGATAN HARI BESAR ISLAM (Prioritas Tinggi)
                    com.masjid.tvsholat.ui.components.IslamicEventScreen(
                        config = config,
                        event = activeEvent,
                        isTomorrow = isEventTomorrow,
                        now = now,
                        runningText = finalRunningText
                    )
                } else if (isTreasuryPeriod) {
                    // 🔥 LAPORAN KAS
                    TreasuryScreen(config = config, runningText = finalRunningText)
                } else if (isHadithPeriod) {
                    // 🔥 HADITS HARIAN
                    HadithScreen(config = config, now = now, runningText = finalRunningText)
                } else if (isInfoPeriod && config.infoItems.isNotEmpty()) {
                    // 🔥 PAPAN INFORMASI
                    com.masjid.tvsholat.ui.components.InfoScreen(config = config, now = now, runningText = finalRunningText)
                } else {
                    // 🔥 PILIH TEMA DISINI
                    when (config.themeName) {
                        "focus" -> FocusHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                        "modern" -> ModernHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                        "elegant" -> ElegantHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                        "classic" -> ClassicHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                        "dashboard" -> DashboardHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                        "grand" -> GrandHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                        "premium" -> PremiumHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                        else -> SimpleHomeScreen(now, config, appVersion, deviceIp, prayers, nextPrayer, finalRunningText)
                    }
                }
            }
        }
    }
