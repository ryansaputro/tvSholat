package com.masjid.tvsholat.domain.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.masjid.tvsholat.data.MasjidConfigRepository
import com.masjid.tvsholat.data.TimeRepository
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.currentCoroutineContext

class TimeSyncService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repo: MasjidConfigRepository

    companion object {
        const val SYNC_PORT = 9092
        const val SYNC_MSG_PREFIX = "TVSHOLAT_TIME:"
        // Broadcast interval for Master
        const val BROADCAST_INTERVAL_MS = 3000L 
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = MasjidConfigRepository.getInstance(applicationContext)
        startSync()
    }

    private fun startSync() {
        scope.launch {
            repo.configFlow.collect { config ->
                // Cancel existing jobs to restart with new config
                coroutineContext.cancelChildren()
                
                if (config.isTimeMaster) {
                    launchMasterMode()
                } else {
                    launchSlaveMode()
                }
            }
        }
    }

    private suspend fun launchMasterMode() {
        android.util.Log.d("TimeSyncService", "Starting Master Mode")
        // Reset offset because we ARE the master
        TimeRepository.resetOffset()
        
        while (currentCoroutineContext().isActive) {
            try {
                val now = System.currentTimeMillis()
                val msg = "$SYNC_MSG_PREFIX$now"
                val data = msg.toByteArray(StandardCharsets.UTF_8)
                
                val socket = DatagramSocket()
                socket.broadcast = true
                val packet = DatagramPacket(
                    data, 
                    data.size, 
                    InetAddress.getByName("255.255.255.255"), 
                    SYNC_PORT
                )
                socket.send(packet)
                socket.close()
                // android.util.Log.d("TimeSyncService", "Broadcasted time: $now")
            } catch (e: Exception) {
                android.util.Log.e("TimeSyncService", "Master broadcast error", e)
            }
            delay(BROADCAST_INTERVAL_MS)
        }
    }

    private suspend fun launchSlaveMode() {
        android.util.Log.d("TimeSyncService", "Starting Slave Mode")
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(SYNC_PORT)
            socket.broadcast = true
            
            while (currentCoroutineContext().isActive) {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                
                val message = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                
                // Expect: TVSHOLAT_TIME:<timestamp>
                if (message.startsWith(SYNC_MSG_PREFIX)) {
                    val masterTimeStr = message.substringAfter(SYNC_MSG_PREFIX)
                    val masterTime = masterTimeStr.toLongOrNull()
                    
                    if (masterTime != null) {
                        // Assuming negligible latency on LAN or just simple sync
                        TimeRepository.updateOffset(masterTime)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TimeSyncService", "Slave listener error", e)
             // Retry if socket fails
             delay(5000)
             if (currentCoroutineContext().isActive) launchSlaveMode()
        } finally {
            socket?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
