package com.masjid.tvsholat

import android.os.Bundle
import android.util.Log
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

    private var server: AdminServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = MasjidConfigRepository(this)
        val deviceIp = com.masjid.tvsholat.utils.NetworkUtils.getLocalIpAddress()

        // 🔥 NYALAIN ADMIN SERVER (Pake thread biar lebih stabil)
        Thread {
            try {
                server = AdminServer(repo)
                // Timeout agak lama biar browser gak gampang mutus
                server?.start(60000, false)
                Log.d("ADMIN_SERVER", "Admin server started on $deviceIp:9090")
            } catch (e: Exception) {
                Log.e("ADMIN_SERVER", "Failed to start server", e)
            }
        }.start()

        setContent {
            HomeScreen(repo, deviceIp, BuildConfig.VERSION_NAME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        Log.d("ADMIN_SERVER", "Admin server stopped")
    }
}
