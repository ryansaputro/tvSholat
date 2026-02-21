package com.masjid.tvsholat.domain.service

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.data.MasjidConfigRepository
import kotlinx.coroutines.*
import org.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

class ActivationService(private val context: Context, private val repository: MasjidConfigRepository) {

    // --- CONFIGURATION ---
    private val BOT_TOKEN = "8304580523:AAGBhi72SVNfgRfnU5nF47y3GadHVXl9Qy8" 
    private val ADMIN_CHAT_ID = "1242387872"
    // ---------------------

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPolling = false

    fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return "MSR-${androidId.takeLast(6).uppercase()}"
    }

    fun startPolling() {
        if (isPolling) return
        isPolling = true
        
        serviceScope.launch {
            val config = repository.load()
            // 1. Kirim Laporan Pertama kali kalau belum ada di Telegram (hanya jika belum aktif)
            if (!config.isActivated) {
                sendActivationRequest()
            }
            
            // 2. Poll untuk Approval / Commands
            while (isPolling) {
                checkApprovalStatus()
                delay(10000) // Cek tiap 10 detik
            }
        }
    }

    fun stopPolling() {
        isPolling = false
        serviceScope.cancel()
    }

    fun refreshStatus() {
        serviceScope.launch {
            checkApprovalStatus()
        }
    }

    private fun sendActivationRequest() {
        val deviceId = getDeviceId()
        val message = """
            🚀 *INSTALASI BARU TV SHOLAT*
            Device ID: `$deviceId`
            Status: Menunggu Persetujuan
            
            Ketik `/approve_$deviceId` untuk mengaktifkan TV ini.
        """.trimIndent()

        try {
            val urlString = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage?chat_id=$ADMIN_CHAT_ID&text=${java.net.URLEncoder.encode(message, "UTF-8")}&parse_mode=Markdown"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            val responseCode = conn.responseCode
            Log.d("ACTIVATION", "Send request response: $responseCode")
        } catch (e: Exception) {
            Log.e("ACTIVATION", "Error sending activation request", e)
        }
    }

    private fun checkApprovalStatus() {
        try {
            val urlString = "https://api.telegram.org/bot$BOT_TOKEN/getUpdates?offset=-5" // Ambil 5 pesan terakhir
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            
            val scanner = Scanner(conn.inputStream)
            val response = StringBuilder()
            while (scanner.hasNext()) response.append(scanner.next())
            
            val json = JSONObject(response.toString())
            val results = json.getJSONArray("result")
            
            val myDeviceId = getDeviceId()
            val approvalCommand = "/approve_$myDeviceId"
            val setAdminCommand = "/setadmin_$myDeviceId"

            for (i in 0 until results.length()) {
                val update = results.getJSONObject(i)
                val message = update.optJSONObject("message")
                val text = message?.optString("text") ?: ""
                val from = message?.optJSONObject("from")
                val chatId = from?.optLong("id")?.toString() ?: ""
                
                val myDeviceId = getDeviceId()
                val approvalCommand = "/approve_$myDeviceId"
                val startCommand = "/start $myDeviceId"
                val setAdminCommand = "/setadmin_$myDeviceId"

                if (text == approvalCommand || text == startCommand) {
                    saveAdminChatId(chatId)
                    activateDevice()
                    break
                } else if (text == setAdminCommand) {
                    saveAdminChatId(chatId)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("ACTIVATION", "Error checking approval", e)
        }
    }

    private fun saveAdminChatId(chatId: String) {
        if (chatId.isEmpty()) return
        serviceScope.launch {
            val current = repository.load()
            if (current.adminChatId != chatId) {
                repository.save(current.copy(adminChatId = chatId))
                Log.d("ACTIVATION", "✅ Admin Chat ID saved: $chatId")
            }
        }
    }

    fun sendUpdateNotification(oldConfig: MasjidConfig, newConfig: MasjidConfig, sourceIp: String) {
        // Use a fresh scope to ensure it's not cancelled by polling logic
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = getDeviceId()
                Log.d("ACTIVATION", "🔔 Preparing Telegram notification for $deviceId")
                
                // 1. Diffing Logic
                val changes = mutableListOf<String>()
                if (oldConfig.name != newConfig.name) changes.add("Nama Masjid: ${newConfig.name}")
                if (oldConfig.address != newConfig.address) changes.add("Alamat: ${newConfig.address}")
                if (oldConfig.themeName != newConfig.themeName) changes.add("Tema: ${newConfig.themeName}")
                if (oldConfig.runningText != newConfig.runningText) changes.add("Running Text Diupdate")
                if (oldConfig.enablePowerSaving != newConfig.enablePowerSaving) changes.add("Hemat Daya: ${if(newConfig.enablePowerSaving) "Aktif" else "Nonaktif"}")
                if (oldConfig.keepAwakeOnInternet != newConfig.keepAwakeOnInternet) changes.add("Always On Internet: ${if(newConfig.keepAwakeOnInternet) "Aktif" else "Nonaktif"}")
                if (oldConfig.isTimeMaster != newConfig.isTimeMaster) changes.add("Role Master: ${if(newConfig.isTimeMaster) "Aktif" else "Nonaktif"}")
                if (oldConfig.timeOffsetMinutes != newConfig.timeOffsetMinutes) changes.add("Koreksi Waktu: ${newConfig.timeOffsetMinutes}m")
                if (oldConfig.dateOffsetDays != newConfig.dateOffsetDays) changes.add("Koreksi Hijriah: ${newConfig.dateOffsetDays}d")
                
                // Iqomah
                if (oldConfig.iqomahSubuh != newConfig.iqomahSubuh) changes.add("Iqomah Subuh: ${newConfig.iqomahSubuh}m")
                if (oldConfig.iqomahDzuhur != newConfig.iqomahDzuhur) changes.add("Iqomah Dzuhur: ${newConfig.iqomahDzuhur}m")
                if (oldConfig.iqomahAshar != newConfig.iqomahAshar) changes.add("Iqomah Ashar: ${newConfig.iqomahAshar}m")
                if (oldConfig.iqomahMaghrib != newConfig.iqomahMaghrib) changes.add("Iqomah Maghrib: ${newConfig.iqomahMaghrib}m")
                if (oldConfig.iqomahIsya != newConfig.iqomahIsya) changes.add("Iqomah Isya: ${newConfig.iqomahIsya}m")
                if (oldConfig.sholatDurationMinutes != newConfig.sholatDurationMinutes) changes.add("Durasi Sholat: ${newConfig.sholatDurationMinutes}m")
                
                if (oldConfig.backgroundUrl != newConfig.backgroundUrl || oldConfig.backgroundType != newConfig.backgroundType) changes.add("Background Diubah")
                if (oldConfig.logoUrl != newConfig.logoUrl || oldConfig.logoType != newConfig.logoType) changes.add("Logo Diubah")
                if (oldConfig.infoItems.size != newConfig.infoItems.size) changes.add("Jumlah Papan Info Berubah")
                
                if (changes.isEmpty()) {
                    Log.d("ACTIVATION", "No significant changes to notify.")
                    return@launch
                }
                
                val changesText = changes.joinToString("\n• ")
                val message = """
                    🛠️ *UPDATE ADMIN PANEL*
                    KODE TV: `$deviceId`
                    IP Pengakses: `$sourceIp`
                    
                    *Perubahan:*
                    • $changesText
                """.trimIndent()

                val targetChatId = if (newConfig.adminChatId.isNotEmpty()) newConfig.adminChatId else ADMIN_CHAT_ID
                val urlString = "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage?chat_id=${targetChatId}&text=${java.net.URLEncoder.encode(message, "UTF-8")}&parse_mode=Markdown"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                
                val status = conn.responseCode
                if (status == 200) {
                    Log.d("ACTIVATION", "✅ Telegram notification sent successfully to $targetChatId!")
                } else {
                    val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("ACTIVATION", "❌ Telegram notification FAILED! Status: $status, Target: $targetChatId, Error: $errorStream")
                }
            } catch (e: Exception) {
                Log.e("ACTIVATION", "❌ Error in sendUpdateNotification: ${e.message}", e)
            }
        }
    }

    private fun activateDevice() {
        val currentConfig = repository.load()
        if (!currentConfig.isActivated) {
            val updatedConfig = currentConfig.copy(
                isActivated = true,
                deviceId = getDeviceId()
            )
            CoroutineScope(Dispatchers.IO).launch {
                repository.save(updatedConfig)
            }
            Log.d("ACTIVATION", "✅ DEVICE ACTIVATED!")
            stopPolling()
        }
    }
}
