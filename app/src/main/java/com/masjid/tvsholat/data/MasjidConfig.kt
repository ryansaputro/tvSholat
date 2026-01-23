package com.masjid.tvsholat.data

data class MasjidConfig(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val iqomahMinutes: Int,
    val backgroundUrl: String,
    val timeOffsetMinutes: Int = 0,
    val dateOffsetDays: Int = 0
)
