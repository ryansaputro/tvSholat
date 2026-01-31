package com.masjid.tvsholat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.masjid.tvsholat.data.MasjidConfigRepository
import com.masjid.tvsholat.domain.service.ActivationService
import com.masjid.tvsholat.server.AdminServer
import com.masjid.tvsholat.ui.HomeScreen
import com.masjid.tvsholat.ui.components.ActivationScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var activationService: ActivationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔥 BIAR LAYAR GAK MATI (STANDBY TERUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val repo = MasjidConfigRepository.getInstance(this.applicationContext)
        activationService = ActivationService(this, repo)

        // 🔥 CEK IZIN JALAN DI ATAS APLIKASI LAIN (Penting buat Auto-Start & Admin Server)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Mohon aktifkan 'Tampilkan di atas aplikasi lain' untuk TvSholat", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("PERMISSION", "Could not open overlay settings", e)
            }
        }

        // 🔥 NYALAIN ADMIN SERVER (Repo pake application context biar awet)
        startAdminServer(repo)

        setContent {
            val config by repo.configFlow.collectAsState()
            
            // rememberSaveable biar nggak muncul lagi kalau cuma kedip/refresh (Activity Recreation)
            var showSplash by rememberSaveable { mutableStateOf(true) }
            
            // Reactive IP Address (updates if Wi-Fi connects late)
            var deviceIp by remember { mutableStateOf(com.masjid.tvsholat.utils.NetworkUtils.getLocalIpAddress()) }
            
            LaunchedEffect(Unit) {
                while(true) {
                    delay(5000) // Cek IP tiap 5 detik
                    val currentIp = com.masjid.tvsholat.utils.NetworkUtils.getLocalIpAddress()
                    if (currentIp != deviceIp) {
                        deviceIp = currentIp
                    }
                }
            }

            // --- GATEKEEPER AKTIVASI ---
            if (!config.isActivated) {
                // Jalankan polling aktivasi kalau belum aktif
                DisposableEffect(Unit) {
                    activationService.startPolling()
                    onDispose { activationService.stopPolling() }
                }
                
                ActivationScreen(deviceId = activationService.getDeviceId())
            } else {
                // --- MAIN APP FLOW ---
                if (showSplash) {
                    com.masjid.tvsholat.ui.components.SplashScreen(onFinished = {
                        showSplash = false
                    })
                } else {
                    HomeScreen(repo, deviceIp, BuildConfig.VERSION_NAME)
                }
            }
        }
    }

    private fun startAdminServer(repo: MasjidConfigRepository) {
        Thread {
            try {
                val adminServer = AdminServer.getInstance(repo, this@MainActivity)
                if (!adminServer.isAlive) {
                    adminServer.start()
                    Log.d("ADMIN_SERVER", "Admin server started on port 9090")
                }
            } catch (e: Exception) {
                Log.e("ADMIN_SERVER", "Failed to start server", e)
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        // Pastiin server tetep idup pas aplikasi balik ke depan (Foreground)
        startAdminServer(MasjidConfigRepository.getInstance(this.applicationContext))
    }

    override fun onDestroy() {
        // Stop polling activation if activity destroyed
        if (::activationService.isInitialized) {
            activationService.stopPolling()
        }
        super.onDestroy()
    }
}
