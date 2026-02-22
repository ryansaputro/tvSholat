package com.masjid.tvsholat.server

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

data class PeerInfo(
    val ip: String,
    val deviceId: String,
    val isMaster: Boolean = false,
    val name: String = "Sistem TV"
)

class NetworkDiscovery(context: Context) {
    
    // Gunakan Application Context
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

    companion object {
        const val DISCOVERY_PORT = 9091
        const val DISCOVERY_MSG_PREFIX = "TVSHOLAT_DISCOVERY:"
        const val DISCOVERY_RESPONSE_PREFIX = "TVSHOLAT_HERE:"
    }

    // Cache to store known peers (Key: Device ID)
    private val knownPeers = java.util.concurrent.ConcurrentHashMap<String, PeerInfo>()  

    /**
     * Mengirim string UDP broadcast untuk mencari TV lain.
     * Mengembalikan list PeerInfo dari TV yang merespon.
     */
    suspend fun findPeers(myDeviceId: String, timeoutMs: Int = 2000): List<PeerInfo> = withContext(Dispatchers.IO) {
        val peers = mutableListOf<PeerInfo>()
        var socket: DatagramSocket? = null

        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = timeoutMs

            // Send Discovery Packet with My Device ID
            val msg = "$DISCOVERY_MSG_PREFIX$myDeviceId"
            val data = msg.toByteArray(StandardCharsets.UTF_8)
            // Use broadcast address 255.255.255.255
            val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT)
            socket.send(packet)
            
            android.util.Log.d("NetworkDiscovery", "Broadcast sent: $msg")

            // Listen for responses
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val buffer = ByteArray(1024)
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)

                    val message = String(responsePacket.data, 0, responsePacket.length, StandardCharsets.UTF_8)
                    val senderIp = responsePacket.address.hostAddress ?: ""

                     // Message format: TVSHOLAT_HERE:<deviceId>[:MASTER] or <deviceId>|<MASTER|SLAVE>|<name>
                     if (message.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                          val payload = message.substringAfter(DISCOVERY_RESPONSE_PREFIX)
                          val parts = payload.split("|")
                          val oldParts = payload.split(":")
                          
                          val senderDeviceId = if (parts.size > 1) parts[0] else oldParts[0]
                          val senderIsMaster = if (parts.size > 1) parts[1] == "MASTER" else if (oldParts.size > 1) oldParts[1] == "MASTER" else false
                          val senderName = if (parts.size > 2) parts[2] else "Sistem TV"
                          
                          // Don't include self (check by IP or Device ID)
                          if (senderIp != getMyIpAddress() && senderDeviceId != myDeviceId) {
                             if (peers.none { it.ip == senderIp }) {
                                 val peer = PeerInfo(senderIp, senderDeviceId, senderIsMaster, senderName)
                                 peers.add(peer)
                                 knownPeers[senderDeviceId] = peer // Update cache
                                 android.util.Log.d("NetworkDiscovery", "Found peer: $senderIp ($senderDeviceId) Master: $senderIsMaster, Name: $senderName")
                             }
                         }
                     }
                } catch (e: java.net.SocketTimeoutException) {
                    // Timeout is expected
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("NetworkDiscovery", "Error finding peers", e)
        } finally {
            socket?.close()
        }

        // Return all known peers (cached + new)
        knownPeers.values.toList()
    }

    /**
     * Memulai listener UDP di background thread.
     * @param getDeviceId lambda untuk mendapatkan deviceId saat ini
     * @param getIsMaster lambda untuk mendapatkan status master saat ini
     * @param getName lambda untuk mendapatkan nama TV saat ini
     */
    suspend fun listenForDiscovery(getDeviceId: () -> String, getIsMaster: () -> Boolean, getName: () -> String) = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            // Bind to specific port
            socket = DatagramSocket(DISCOVERY_PORT)
            socket.broadcast = true
            
            android.util.Log.d("NetworkDiscovery", "Listening for discovery on port $DISCOVERY_PORT")

            while (true) {
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                
                // Blocks until packet received
                socket.receive(packet)
                
                val message = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                val senderIp = packet.address.hostAddress ?: ""
                
                // Log untuk debug
                // android.util.Log.d("NetworkDiscovery", "Received: $message from $senderIp")

                // Message format: TVSHOLAT_DISCOVERY:<deviceId>
                if (message.startsWith(DISCOVERY_MSG_PREFIX)) {
                    val senderDeviceId = message.substringAfter(DISCOVERY_MSG_PREFIX)
                    val myDeviceId = getDeviceId()
                    val isMaster = getIsMaster()
                    val myName = getName()
                    
                    // Jangan respon ke diri sendiri
                    if (senderIp != getMyIpAddress() && senderDeviceId != myDeviceId) {
                         // Send Response back to sender with My Device ID, Master status, and Name
                        val responseMsg = "$DISCOVERY_RESPONSE_PREFIX$myDeviceId|${if (isMaster) "MASTER" else "SLAVE"}|$myName"
                        val responseData = responseMsg.toByteArray(StandardCharsets.UTF_8)
                        val responsePacket = DatagramPacket(
                            responseData, 
                            responseData.size, 
                            packet.address, 
                            packet.port
                        )
                        socket.send(responsePacket)
                        
                        // Also add sender to our known peers!
                        // Format sender isMaster is unknown from discovery request usually, 
                        // but we can assume false or just leave it for findPeers to update.
                        val peer = PeerInfo(senderIp, senderDeviceId, false, "Sistem TV") 
                        knownPeers[senderDeviceId] = peer
                        
                        android.util.Log.d("NetworkDiscovery", "Responded to discovery from $senderIp ($senderDeviceId) as ${if (isMaster) "MASTER" else "SLAVE"} $myName")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NetworkDiscovery", "Error listening for discovery", e)
        } finally {
            socket?.close()
        }
    }

    fun getMyIpAddress(): String? {
        val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        linkProperties?.linkAddresses?.forEach { linkAddress ->
            val address = linkAddress.address
            if (address is java.net.Inet4Address && !address.isLoopbackAddress) {
                return address.hostAddress
            }
        }
        
        // Fallback for older devices or if CM fails
        @Suppress("DEPRECATION")
        val ip = wifiManager.connectionInfo.ipAddress
        return formatIp(ip)
    }

    private fun formatIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
