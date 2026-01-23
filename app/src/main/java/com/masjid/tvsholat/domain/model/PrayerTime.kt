package com.masjid.tvsholat.domain.model

import java.util.Date

data class PrayerTime(
    val name: String,
    val time: String,
    val date: Date,
    val isPrayer: Boolean = true
)
