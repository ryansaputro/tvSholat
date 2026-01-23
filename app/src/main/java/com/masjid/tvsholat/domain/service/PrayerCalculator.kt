package com.masjid.tvsholat.domain.service

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.masjid.tvsholat.domain.model.PrayerTime
import java.text.SimpleDateFormat
import java.util.*

object PrayerCalculator {

    fun todayPrayerTimes(
        latitude: Double,
        longitude: Double
    ): List<PrayerTime> {

        val coordinates = Coordinates(latitude, longitude)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        val date = DateComponents.from(Date())

        val times = PrayerTimes(coordinates, date, params)
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

//        // =====================
//// TESTING ONLY
//// =====================
//        val now = Date()
//
//
//        return listOf(
//            PrayerTime(
//                name = "Subuh",
//                time = formatter.format(Date(now.time + 60_000)),
//                date = Date(now.time + 60_000)
//            ),
//            PrayerTime(
//                name = "Dzuhur",
//                time = formatter.format(Date(now.time + 120_000)),
//                date = Date(now.time + 120_000)
//            ),
//            PrayerTime(
//                name = "Ashar",
//                time = formatter.format(Date(now.time + 180_000)),
//                date = Date(now.time + 180_000)
//            ),
//            PrayerTime(
//                name = "Maghrib",
//                time = formatter.format(Date(now.time + 240_000)),
//                date = Date(now.time + 240_000)
//            ),
//            PrayerTime(
//                name = "Isya",
//                time = formatter.format(Date(now.time + 300_000)),
//                date = Date(now.time + 300_000)
//            )
//        )

        val fajrTime = times.fajr

        val cal = Calendar.getInstance().apply {
            time = fajrTime
            add(Calendar.MINUTE, -10) // 🔥 IMSAK = Subuh - 10 menit
        }

        val imsakTime = cal.time

        return listOf(
            PrayerTime("Imsak", formatter.format(imsakTime), imsakTime),
            PrayerTime("Subuh", formatter.format(times.fajr), times.fajr),
            PrayerTime("Terbit", formatter.format(times.sunrise), times.sunrise),
            PrayerTime("Dzuhur", formatter.format(times.dhuhr), times.dhuhr),
            PrayerTime("Ashar", formatter.format(times.asr), times.asr),
            PrayerTime("Maghrib", formatter.format(times.maghrib), times.maghrib),
            PrayerTime("Isya", formatter.format(times.isha), times.isha)
        )
    }
}
