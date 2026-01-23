package com.masjid.tvsholat.utils

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    fun getLocalIpAddress(): String {
        var foundIp = "Unknown IP"
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val host = address.hostAddress ?: continue
                        android.util.Log.d("NETWORK_UTILS", "Found IP: $host on ${networkInterface.name}")
                        // Prioritas IP lokal standar 192.168
                        if (host.startsWith("192.168.")) return host
                        foundIp = host
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return foundIp
    }
}
