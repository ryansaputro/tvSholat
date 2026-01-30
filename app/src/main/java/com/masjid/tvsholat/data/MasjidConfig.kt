package com.masjid.tvsholat.data

data class MasjidConfig(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val iqomahMinutes: Int,
    val backgroundUrl: String = "",
    val themeName: String = "simple",
    val runningText: String = "Selamat datang di Masjid Al-Kautsar. Luruskan dan rapatkan shaf sholat kita.",
    val timeOffsetMinutes: Int = 0,
    val dateOffsetDays: Int = 0,
    val treasuryBalance: String = "0",
    val treasuryDescription: String = "Saldo Kas Masjid",
    val treasuryDisplayInterval: Int = 0,
    val treasuryDisplayDuration: Int = 15,
    val treasuryAccountInfo: String = "BSI 0044448884",
    val treasuryQrisData: String = "",
    val hadithDisplayInterval: Int = 0,
    val hadithDisplayDuration: Int = 20,
    val isActivated: Boolean = false,
    val deviceId: String = "",
    val lastUpdated: String = ""
)
