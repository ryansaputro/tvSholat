package com.masjid.tvsholat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.domain.model.PrayerTime
import com.masjid.tvsholat.domain.service.PrayerCalculator
import com.masjid.tvsholat.ui.components.ClockHeader
import com.masjid.tvsholat.ui.components.ScreenBackground
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun HomeScreen() {

    val latitude = -6.3210
    val longitude = 107.0220

    var now by remember { mutableStateOf(Date()) }

    // =====================
    // IQOMAH STATE
    // =====================
    var isIqomah by remember { mutableStateOf(false) }
    var iqomahSeconds by remember { mutableStateOf(5 * 60) }
    var currentPrayer by remember { mutableStateOf<PrayerTime?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = Date()
        }
    }

    val prayers = remember {
        PrayerCalculator.todayPrayerTimes(latitude, longitude)
    }

    val nextPrayer: PrayerTime? = run {
        prayers.firstOrNull { it.date.after(now) } ?: prayers.firstOrNull()
    }

    // =====================
    // TRIGGER IQOMAH
    // =====================
    LaunchedEffect(now) {
        if (!isIqomah) {
            prayers.forEach { prayer ->
                if (now.after(prayer.date) &&
                    now.time - prayer.date.time < 1000
                ) {
                    isIqomah = true
                    iqomahSeconds = 5 * 60
                    currentPrayer = prayer
                }
            }
        }
    }

    // =====================
    // COUNTDOWN IQOMAH
    // =====================
    LaunchedEffect(isIqomah) {
        if (isIqomah) {
            while (iqomahSeconds > 0) {
                delay(1000)
                iqomahSeconds--
            }
            isIqomah = false
            currentPrayer = null
        }
    }

    ScreenBackground {

        // =====================
        // IQOMAH MODE
        // =====================
        if (isIqomah && currentPrayer != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IQOMAH",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentPrayer!!.name,
                    fontSize = 36.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                val minutes = iqomahSeconds / 60
                val seconds = iqomahSeconds % 60

                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            return@ScreenBackground
        }

        // =====================
        // NORMAL SCREEN
        // =====================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp), // 🔥 DIPERKECIL
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ===== HEADER =====
            Column {
                ClockHeader(now)

                nextPrayer?.let {
                    Text(
                        text = "Menuju ${it.name}",
                        fontSize = 22.sp,
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ===== LIST JADWAL =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 🔥 KUNCI BIAR ISYA TIDAK KE POTONG
            ) {
                prayers.forEach {
                    val isNext = it == nextPrayer

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isNext) Color(0xFF1E2A25) else Color.Transparent
                            )
                            .padding(vertical = 6.dp, horizontal = 12.dp), // 🔥 LEBIH RAPAT
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = it.name,
                            fontSize = 22.sp,
                            color = if (isNext) Color(0xFFFFD54F) else Color.White,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                        )

                        Text(
                            text = it.time,
                            fontSize = 24.sp,
                            color = if (isNext) Color(0xFFFFD54F) else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ===== FOOTER =====
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Masjid Al-Kautsar",
                    fontSize = 18.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Perum Arcadia Residence",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
