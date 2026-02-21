package com.masjid.tvsholat.domain.service

import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import com.masjid.tvsholat.data.MasjidConfigRepository
import com.masjid.tvsholat.data.TimeRepository
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

class TimeSyncService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repo: MasjidConfigRepository
    private var mainSyncJob: Job? = null

    companion object {
        const val SYNC_PORT = 9092
        const val SYNC_MSG_PREFIX = "TVSHOLAT_TIME:"
        const val PING_MSG = "TVSHOLAT_PING"
        const val PONG_MSG_PREFIX = "TVSHOLAT_PONG:"
        const val BROADCAST_INTERVAL_MS = 3000L
        const val SLAVE_SYNC_INTERVAL_MS = 10000L // Slave pings master every 10s for precision
        const val CHANNEL_ID = "TimeSyncChannel"
        const val NOTIFICATION_ID = 101
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = MasjidConfigRepository.getInstance(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerNetworkCallback()
        startSync()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                android.util.Log.d("TimeSyncService", "Network available, restarting sync...")
                // Restart sync immediately when network is back
                startSync()
            }
        })
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val serviceChannel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Sinkronisasi Waktu Masjid",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): android.app.Notification {
        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TvSholat - Sinkronisasi Waktu")
            .setContentText("Menjaga akurasi jam di semua TV...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    private fun startSync() {
        mainSyncJob?.cancel()
        mainSyncJob = scope.launch {
            repo.configFlow.collect { config ->
                coroutineContext.cancelChildren()
                if (config.isTimeMaster) {
                    launchMasterMode()
                } else {
                    launchSlaveMode()
                }
            }
        }
    }

    /**
     * Helper to create a DatagramSocket with SO_REUSEADDR to avoid EADDRINUSE
     */
    private fun createBoundSocket(port: Int): DatagramSocket {
        return DatagramSocket(null).apply {
            reuseAddress = true
            bind(java.net.InetSocketAddress(port))
        }
    }

    private suspend fun launchMasterMode() = coroutineScope {
        android.util.Log.d("TimeSyncService", "Starting Master Mode")
        TimeRepository.resetOffset()
        
        // Job 1: Broadcast time every 3s
        launch {
            while (isActive) {
                var socket: DatagramSocket? = null
                try {
                    socket = DatagramSocket()
                    socket.broadcast = true
                    while (isActive) {
                        val now = System.currentTimeMillis()
                        val msg = "$SYNC_MSG_PREFIX$now"
                        val data = msg.toByteArray(StandardCharsets.UTF_8)
                        val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), SYNC_PORT)
                        socket.send(packet)
                        delay(BROADCAST_INTERVAL_MS)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TimeSyncService", "Master broadcast socket error, retrying in 10s...", e)
                    delay(10000)
                } finally {
                    socket?.close()
                }
            }
        }

        // Job 2: Listen for PING from Slaves
        launch {
            while (isActive) {
                var socket: DatagramSocket? = null
                try {
                    socket = createBoundSocket(SYNC_PORT)
                    socket.broadcast = true
                    socket.soTimeout = 2000 // Crucial for responsive cancellation
                    
                    // Close socket immediately if job is cancelled
                    val handler = coroutineContext[Job]?.invokeOnCompletion { socket?.close() }
                    
                    while (isActive) {
                        try {
                            val buffer = ByteArray(256)
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                            if (msg == PING_MSG) {
                                val now = System.currentTimeMillis()
                                val response = "$PONG_MSG_PREFIX$now"
                                val data = response.toByteArray(StandardCharsets.UTF_8)
                                val responsePacket = DatagramPacket(data, data.size, packet.address, packet.port)
                                socket.send(responsePacket)
                            }
                        } catch (e: java.net.SocketTimeoutException) {
                            // Loop to check isActive
                        }
                    }
                    handler?.dispose()
                } catch (e: Exception) {
                    if (isActive) {
                        android.util.Log.e("TimeSyncService", "Master ping socket error, retrying in 10s...", e)
                        delay(10000)
                    }
                } finally {
                    socket?.close()
                }
            }
        }
    }


    private suspend fun launchSlaveMode() = coroutineScope {
        android.util.Log.d("TimeSyncService", "Starting Slave Mode")
        val pingSentTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()

        // Job 1: Listen for Broadcasts or PONGs
        launch {
            while (isActive) {
                var socket: DatagramSocket? = null
                try {
                    socket = createBoundSocket(SYNC_PORT)
                    socket.broadcast = true
                    socket.soTimeout = 2000 // Allow isActive check
                    
                    val handler = coroutineContext[Job]?.invokeOnCompletion { socket?.close() }

                    while (isActive) {
                        try {
                            val buffer = ByteArray(1024)
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                            val receivedAt = System.currentTimeMillis()
    
                            if (msg.startsWith(SYNC_MSG_PREFIX)) {
                                val masterTime = msg.substringAfter(SYNC_MSG_PREFIX).toLongOrNull()
                                if (masterTime != null) {
                                    TimeRepository.updateOffset(masterTime)
                                }
                            } else if (msg.startsWith(PONG_MSG_PREFIX)) {
                                val masterTime = msg.substringAfter(PONG_MSG_PREFIX).toLongOrNull()
                                if (masterTime != null) {
                                    val sentAt = pingSentTimes[packet.address.hostAddress] 
                                        ?: pingSentTimes["broadcast"] 
                                        ?: (receivedAt - 10)
                                    val rtt = receivedAt - sentAt
                                    TimeRepository.updateOffset(masterTime, rtt)
                                }
                            }
                        } catch (e: java.net.SocketTimeoutException) {
                            // Timeout ok, loop again to check isActive
                        }
                    }
                    handler?.dispose()
                } catch (e: Exception) {
                    if (isActive) {
                        android.util.Log.e("TimeSyncService", "Slave receiver socket error, retrying in 10s...", e)
                        delay(10000)
                    }
                } finally {
                    socket?.close()
                }
            }
        }

        // Job 2: Occasionally PING master for high precision
        launch {
            while (isActive) {
                var socket: DatagramSocket? = null
                try {
                    socket = DatagramSocket()
                    socket.broadcast = true
                    while (isActive) {
                        delay(SLAVE_SYNC_INTERVAL_MS)
                        val data = PING_MSG.toByteArray(StandardCharsets.UTF_8)
                        val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), SYNC_PORT)
                        pingSentTimes["broadcast"] = System.currentTimeMillis()
                        socket.send(packet)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TimeSyncService", "Slave ping sender socket error, retrying in 10s...", e)
                    delay(10000)
                } finally {
                    socket?.close()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
