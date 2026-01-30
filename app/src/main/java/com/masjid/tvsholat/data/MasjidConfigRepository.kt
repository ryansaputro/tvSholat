package com.masjid.tvsholat.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MasjidConfigRepository(context: Context) {

    private val prefs =
        context.getSharedPreferences("masjid_config", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(load())
    val configFlow = _configFlow.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _configFlow.value = load()
    }

    init {
        val currentConfig = load()
        _configFlow.value = currentConfig
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun save(config: MasjidConfig) {
        prefs.edit()
            .putString("name", config.name)
            .putString("address", config.address)
            .putFloat("lat", config.latitude.toFloat())
            .putFloat("lng", config.longitude.toFloat())
            .putInt("iqomah", config.iqomahMinutes)
            .putString("bg_url", config.backgroundUrl)
            .putString("theme_name", config.themeName)
            .putString("running_text", config.runningText)
            .putInt("time_offset", config.timeOffsetMinutes)
            .putInt("date_offset", config.dateOffsetDays)
            .putString("treasury_balance", config.treasuryBalance)
            .putString("treasury_desc", config.treasuryDescription)
            .putInt("treasury_interval", config.treasuryDisplayInterval)
            .putInt("hadith_interval", config.hadithDisplayInterval)
            .putString("last_updated", config.lastUpdated)
            .apply()
    }

    fun load(): MasjidConfig {
        return MasjidConfig(
            name = prefs.getString("name", "Masjid Al-Kautsar") ?: "",
            address = prefs.getString("address", "Perum Arcadia Residence") ?: "",
            latitude = prefs.getFloat("lat", -6.321f).toDouble(),
            longitude = prefs.getFloat("lng", 107.022f).toDouble(),
            iqomahMinutes = prefs.getInt("iqomah", 5),
            backgroundUrl = prefs.getString("bg_url", "") ?: "",
            themeName = prefs.getString("theme_name", "simple") ?: "simple",
            runningText = prefs.getString("running_text", "Selamat datang di Masjid Al-Kautsar. Luruskan dan rapatkan shaf sholat kita.") ?: "",
            timeOffsetMinutes = prefs.getInt("time_offset", 0),
            dateOffsetDays = prefs.getInt("date_offset", 0),
            treasuryBalance = prefs.getString("treasury_balance", "0") ?: "0",
            treasuryDescription = prefs.getString("treasury_desc", "Saldo Kas Masjid") ?: "Saldo Kas Masjid",
            treasuryDisplayInterval = prefs.getInt("treasury_interval", 0),
            hadithDisplayInterval = prefs.getInt("hadith_interval", 0),
            lastUpdated = prefs.getString("last_updated", "") ?: ""
        )
    }
}
