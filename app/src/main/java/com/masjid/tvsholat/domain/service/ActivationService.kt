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
            // 1. Kirim Laporan Pertama kali kalau belum ada di Telegram
            sendActivationRequest()
            
            // 2. Poll untuk Approval
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

            for (i in 0 until results.length()) {
                val update = results.getJSONObject(i)
                val message = update.optJSONObject("message")
                val text = message?.optString("text")
                
                if (text == approvalCommand) {
                    activateDevice()
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("ACTIVATION", "Error checking approval", e)
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
