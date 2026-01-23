package com.masjid.tvsholat.domain.service

import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.masjid.tvsholat.domain.model.PrayerTime
import java.text.SimpleDateFormat
import java.util.*

object PrayerCalculator {

    fun calculateForDate(lat: Double, lng: Double, targetDate: Date): List<PrayerTime> {
        val coordinates = Coordinates(lat, lng)
        val date = DateComponents.from(targetDate)
        
        // Standar Kemenag: Subuh 20 deg, Isya 18 deg
        val params = CalculationMethod.SINGAPORE.parameters.apply {
            fajrAngle = 20.0
            ishaAngle = 18.0
        }

        val prayerTimes = PrayerTimes(coordinates, date, params)
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Tambahan Ihtiyati (Pengaman) 2 menit sesuai standar Kemenag
        fun Date.withIhtiyati(): Date = Date(this.time + 2 * 60 * 1000)
        fun Date.format(): String = formatter.format(this)

        val fajr = prayerTimes.fajr.withIhtiyati()
        val imsak = Date(fajr.time - 12 * 60 * 1000) // 10 menit sebelum Subuh asli (sebelum ihtiyati)
        val syuruq = prayerTimes.sunrise.withIhtiyati()
        val dhuhr = prayerTimes.dhuhr.withIhtiyati()
        val asr = prayerTimes.asr.withIhtiyati()
        val maghrib = prayerTimes.maghrib.withIhtiyati()
        val isha = prayerTimes.isha.withIhtiyati()

        return listOf(
            PrayerTime("Imsak", imsak.format(), imsak),
            PrayerTime("Subuh", fajr.format(), fajr),
            PrayerTime("Syuruq", syuruq.format(), syuruq),
            PrayerTime("Dzuhur", dhuhr.format(), dhuhr),
            PrayerTime("Ashar", asr.format(), asr),
            PrayerTime("Maghrib", maghrib.format(), maghrib),
            PrayerTime("Isya", isha.format(), isha)
        )
    }
}
