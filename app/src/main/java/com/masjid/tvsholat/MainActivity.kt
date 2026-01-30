package com.masjid.tvsholat

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.masjid.tvsholat.data.MasjidConfigRepository
import com.masjid.tvsholat.server.AdminServer
import com.masjid.tvsholat.ui.HomeScreen
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔥 BIAR LAYAR GAK MATI (STANDBY TERUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val repo = MasjidConfigRepository(this)
        val deviceIp = com.masjid.tvsholat.utils.NetworkUtils.getLocalIpAddress()

        // 🔥 NYALAIN ADMIN SERVER (Pake thread biar lebih stabil)
        Thread {
            try {
                val adminServer = AdminServer.getInstance(repo)
                if (!adminServer.isAlive) {
                    adminServer.start(60000, false)
                    Log.d("ADMIN_SERVER", "Admin server started on $deviceIp:9090")
                } else {
                    Log.d("ADMIN_SERVER", "Admin server is already running")
                }
            } catch (e: Exception) {
                Log.e("ADMIN_SERVER", "Failed to start server", e)
            }
        }.start()

        setContent {
            HomeScreen(repo, deviceIp, BuildConfig.VERSION_NAME)
        }
    }

    override fun onDestroy() {
        try {
            val repo = MasjidConfigRepository(this)
            AdminServer.getInstance(repo).stop()
            Log.d("ADMIN_SERVER", "Admin server stopped")
        } catch (e: Exception) {
            Log.e("ADMIN_SERVER", "Error stopping server", e)
        }
        super.onDestroy()
    }
}
