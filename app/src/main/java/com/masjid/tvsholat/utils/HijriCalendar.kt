package com.masjid.tvsholat.utils

import java.util.Calendar
import java.util.Date

data class HijriDate(val day: Int, val month: Int, val year: Int) {
    fun getMonthName(): String {
        return when (month) {
            1 -> "Muharram"
            2 -> "Safar"
            3 -> "Rabi'ul Awal"
            4 -> "Rabi'ul Akhir"
            5 -> "Jumadil Awal"
            6 -> "Jumadil Akhir"
            7 -> "Rajab"
            8 -> "Sya'ban"
            9 -> "Ramadhan"
            10 -> "Syawal"
            11 -> "Dzulqa'dah"
            12 -> "Dzulhijjah"
            else -> ""
        }
    }
}

data class IslamicEvent(
    val name: String,
    val description: String,
    val hijriDay: Int,
    val hijriMonth: Int
)

object HijriCalendar {

    // Major Islamic Events
    private val events = listOf(
        IslamicEvent("Tahun Baru Islam", "Selamat Tahun Baru Hijriyah. Semoga tahun ini membawa keberkahan bagi kita semua.", 1, 1),
        IslamicEvent("Hari Asyura", "Disunnahkan berpuasa pada hari ini (Puasa Asyura) untuk menghapus dosa setahun lalu.", 10, 1),
        IslamicEvent("Maulid Nabi Muhammad SAW", "Memperbanyak sholawat dan meneladani akhlak Rasulullah SAW.", 12, 3),
        IslamicEvent("Isra Mi'raj", "Perjalanan agung Nabi Muhammad SAW menerima perintah Sholat 5 Waktu.", 27, 7),
        IslamicEvent("Nisfu Sya'ban", "Malam pengampunan dosa. Mari perbanyak istighfar dan amal shaleh.", 15, 8),
        IslamicEvent("Awal Ramadhan", "Marhaban Ya Ramadhan. Selamat menunaikan Ibadah Puasa.", 1, 9),
        IslamicEvent("Nuzulul Qur'an", "Malam turunnya Al-Qur'an sebagai petunjuk bagi umat manusia.", 17, 9),
        IslamicEvent("Idul Fitri", "Taqabbalallahu Minna Wa Minkum. Mohon Maaf Lahir dan Batin.", 1, 10),
        IslamicEvent("Hari Arafah", "Puncak Ibadah Haji. Disunnahkan puasa Arafah bagi yang tidak berhaji.", 9, 12),
        IslamicEvent("Idul Adha", "Hari Raya Kurban. Mari berbagi kebahagiaan dengan berkurban.", 10, 12)
    )

    fun toHijri(date: Date): HijriDate {
        val cal = Calendar.getInstance()
        cal.time = date
        return gregorianToHijri(cal)
    }

    // Standard Tabular Islamic Calendar Algorithm (Kuwaiti Algo)
    private fun gregorianToHijri(gcal: Calendar): HijriDate {
        var day = gcal.get(Calendar.DAY_OF_MONTH)
        var month = gcal.get(Calendar.MONTH) // 0-based
        var year = gcal.get(Calendar.YEAR)

        var m = month + 1
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }

        var a = Math.floor(y / 100.0).toInt()
        var b = 2 - a + Math.floor(a / 4.0).toInt()
        if (y < 1583) b = 0
        if (y == 1582) {
            if (m > 10) b = -10
            if (m == 10) {
                b = 0
                if (day > 4) b = -10
            }
        }

        val jd = Math.floor(365.25 * (y + 4716)).toInt() + Math.floor(30.6001 * (m + 1)).toInt() + day + b - 1524

        b = 0
        var i = Math.floor((jd - 1867216.25) / 36524.25).toInt()
        a = Math.floor(i / 4.0).toInt()
        var e = 1 + i - a
        var f = 331400 + 52 * (jd - 1867216.25 - e)
        var g = Math.floor(f / 19570.5).toInt() * 19570.5
        var h = Math.floor((f - g) / 1062.55).toInt()
        val d = Math.floor((f - g - h * 1062.55 + 28.5) / 29.5306).toInt()
        
        // Adjust standard calculation to hijri epoch
        val ijd = jd + 29
        
        // Basic approximate calculation
        val z = ijd - 1948440 + 10632
        val n = Math.floor((z - 1) / 10631.0).toInt()
        val z1 = z - 10631 * n + 354
        val j = (Math.floor((10985 - z1) / 5316.0).toInt() * Math.floor((50 * z1) / 3671.0).toInt()) +
                (Math.floor(z1 / 5670.0).toInt() * Math.floor((43 * z1) / 3671.0).toInt())
        val l1 = (Math.floor((30 - j) / 15.0).toInt() * Math.floor((17719 * j) / 50.0).toInt()) +
                (Math.floor(j / 16.0).toInt() * Math.floor((15238 * j) / 43.0).toInt()) + 29
        val m1 = Math.floor((24 * z1) / 709.0).toInt()
        val d1 = z1 - Math.floor((709 * m1) / 24.0).toInt()
        
        val hijriYear = 30 * n + j - 30 + 10632 // Adjusted epoch? No, let's use the known 1335 correction
        // Correction from standard algorithm reference (e.g. from vb code ported to kotlin)
        // Let's use a simpler known algorithm or Just use 'jd' to compute
        
        val k = Math.floor((jd - 1948440) / 10631.0).toInt()
        val l = (jd - 1948440) - 10631 * k
        val n1 = Math.floor((l - 1) / 10631.0).toInt() // redundant
        val l2 = l 
        val j1 = Math.floor(l2 / 354.366).toInt()
        val d2 = Math.floor(1.0 + (l2 - Math.floor(354.366 * j1))).toInt()
        
        // Re-calibrating based on standard formula
        val epoch = 1948440
        val shift = 0.6 // Optimization shift
        
        var z2 = jd - epoch
        val cycles = Math.floor(z2 / 10631.0).toInt()
        z2 -= cycles * 10631
        var y2 = Math.floor((z2 - shift) / 354.36667).toInt()
        var z3 = z2 - Math.floor(y2 * 354.36667 + shift).toInt()
        var m2 = Math.floor((z3 + 28.5001) / 29.5).toInt()
        if (m2 == 13) m2 = 12
        var day2 = z3 - Math.floor(29.5001 * m2 - 29).toInt()

        var hYear = cycles * 30 + y2
        var hMonth = m2
        var hDay = day2

        return HijriDate(hDay, hMonth, hYear)
    }

    /**
     * Checks if the given date IS the event.
     */
    fun getEvent(date: Date): IslamicEvent? {
        val hijriDate = toHijri(date)
        return events.find { 
            it.hijriDay == hijriDate.day && it.hijriMonth == hijriDate.month
        }
    }

    /**
     * Checks if tomorrow is a special event.
     * Returns the event if found.
     */
    fun getUpcomingEvent(today: Date): IslamicEvent? {
        val cal = Calendar.getInstance()
        cal.time = today
        cal.add(Calendar.DAY_OF_YEAR, 1) // Besok
        val tomorrow = cal.time
        
        return getEvent(tomorrow)
    }
}
