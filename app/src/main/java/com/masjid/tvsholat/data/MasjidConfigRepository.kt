package com.masjid.tvsholat.data

import com.masjid.tvsholat.domain.model.InfoItem
import org.json.JSONArray
import org.json.JSONObject
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MasjidConfigRepository private constructor(context: Context) {
    // 🔥 PAKE APPLICATION CONTEXT BIAR GAK LEAK
    private val appContext = context.applicationContext
    
    private val prefs =
        appContext.getSharedPreferences("masjid_config", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: MasjidConfigRepository? = null

        fun getInstance(context: Context): MasjidConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: MasjidConfigRepository(context).also { instance = it }
            }
        }
    }

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
        // 🔥 PROTECTION: Jangan biarkan isActivated pindah dari true ke false secara tidak sengaja
        val current = load()
        val finalIsActivated = config.isActivated || current.isActivated
        val finalDeviceId = config.deviceId.ifEmpty { current.deviceId }
        
        prefs.edit()
            .putString("name", config.name)
            .putString("address", config.address)
            .putFloat("lat", config.latitude.toFloat())
            .putFloat("lng", config.longitude.toFloat())
            .putInt("iqomah", config.iqomahMinutes)
            .putString("bg_url", config.backgroundUrl)
            .putString("bg_type", config.backgroundType)
            .putString("bg_local_path", config.backgroundLocalPath)
            .putString("theme_name", config.themeName)
            .putString("running_text", config.runningText)
            .putInt("time_offset", config.timeOffsetMinutes)
            .putInt("date_offset", config.dateOffsetDays)
            .putString("treasury_balance", config.treasuryBalance)
            .putString("treasury_desc", config.treasuryDescription)
            .putInt("treasury_interval", config.treasuryDisplayInterval)
            .putInt("treasury_duration", config.treasuryDisplayDuration)
            .putString("treasury_account", config.treasuryAccountInfo)
            .putString("treasury_qris", config.treasuryQrisData)
            .putInt("hadith_interval", config.hadithDisplayInterval)
            .putInt("hadith_duration", config.hadithDisplayDuration)
            .putBoolean("is_activated", finalIsActivated) // ✅ GUNAKAN FINAL STATUS
            .putString("device_id", finalDeviceId)      // ✅ GUNAKAN FINAL ID
            .putString("device_id", finalDeviceId)      // ✅ GUNAKAN FINAL ID
            .putString("last_updated", config.lastUpdated)
            .putInt("info_interval", config.infoDisplayInterval)
            .putInt("info_duration", config.infoDisplayDuration)
            .putString("info_items_json", JSONArray().apply {
                config.infoItems.forEach { item ->
                    put(JSONObject().apply {
                        put("title", item.title)
                        put("content", item.content)
                    })
                }
            }.toString())
            .commit() // 🔥 PAKE COMMIT BIAR SINCRONOUS (Sync ke Disk)
    }

    fun load(): MasjidConfig {
        return MasjidConfig(
            name = prefs.getString("name", "Masjid Al-Kautsar") ?: "",
            address = prefs.getString("address", "Perum Arcadia Residence") ?: "",
            latitude = prefs.getFloat("lat", -6.321f).toDouble(),
            longitude = prefs.getFloat("lng", 107.022f).toDouble(),
            iqomahMinutes = prefs.getInt("iqomah", 5),
            backgroundUrl = prefs.getString("bg_url", "") ?: "",
            backgroundType = prefs.getString("bg_type", "url") ?: "url",
            backgroundLocalPath = prefs.getString("bg_local_path", "") ?: "",
            themeName = prefs.getString("theme_name", "simple") ?: "simple",
            runningText = prefs.getString("running_text", "Selamat datang di Masjid Al-Kautsar. Luruskan dan rapatkan shaf sholat kita.") ?: "",
            timeOffsetMinutes = prefs.getInt("time_offset", 0),
            dateOffsetDays = prefs.getInt("date_offset", 0),
            treasuryBalance = prefs.getString("treasury_balance", "0") ?: "0",
            treasuryDescription = prefs.getString("treasury_desc", "Saldo Kas Masjid") ?: "Saldo Kas Masjid",
            treasuryDisplayInterval = prefs.getInt("treasury_interval", 0),
            treasuryDisplayDuration = prefs.getInt("treasury_duration", 15),
            treasuryAccountInfo = prefs.getString("treasury_account", "BSI 0044448884") ?: "BSI 0044448884",
            treasuryQrisData = prefs.getString("treasury_qris", "") ?: "",
            hadithDisplayInterval = prefs.getInt("hadith_interval", 0),
            hadithDisplayDuration = prefs.getInt("hadith_duration", 20),
            isActivated = prefs.getBoolean("is_activated", false),
            deviceId = prefs.getString("device_id", "") ?: "",
            lastUpdated = prefs.getString("last_updated", "") ?: "",
            infoDisplayInterval = prefs.getInt("info_interval", 0),
            infoDisplayDuration = prefs.getInt("info_duration", 15),
            infoItems = try {
                val jsonString = prefs.getString("info_items_json", "[]") ?: "[]"
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<InfoItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(InfoItem(
                        title = obj.optString("title"),
                        content = obj.optString("content")
                    ))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
}
