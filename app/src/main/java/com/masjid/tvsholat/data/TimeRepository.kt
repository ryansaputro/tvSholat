package com.masjid.tvsholat.data

import java.util.Date
import kotlin.math.abs

/**
 * Singleton object to manage time synchronization offset.
 * Stores the difference between local system time and the "Master" time.
 */
object TimeRepository {
    
    // Offset in milliseconds (Master Time - Local Time)
    // Marked volatile to ensure thread visibility
    @Volatile
    private var timeOffsetMillis: Long = 0L

    // Flag to indicate if the time was restored from storage and might be inaccurate
    var isTimeUnreliable: Boolean = false
        private set

    // Timestamp of the last activity on the Admin Panel
    @Volatile
    var lastAdminActivityTime: Long = 0L

    /**
     * Gets the current synchronized time.
     * @param manualOffsetMinutes Additional manual offset from Config (optional)
     */
    fun getCurrentTime(manualOffsetMinutes: Int = 0): Date {
        val now = System.currentTimeMillis()
        val syncedTime = now + timeOffsetMillis
        
        // Add manual user preference offset
        val finalTime = syncedTime + (manualOffsetMinutes * 60 * 1000L)
        return Date(finalTime)
    }

    /**
     * Updates the offset based on a received master timestamp.
     * We use a simple smoothing or direct application. 
     * For simplicity: Direct application if difference is significant.
     */
    fun updateOffset(masterTimeMillis: Long, roundTripLatency: Long = 0) {
        val now = System.currentTimeMillis()
        
        // NTP Logic:
        // RemoteClockTime = masterTimeMillis + (RTT / 2)
        // newOffset = RemoteClockTime - now
        val estimatedMasterTime = masterTimeMillis + (roundTripLatency / 2)
        val newOffset = estimatedMasterTime - now

        // Smoothing: Alih-alih langsung ganti total (bikin jam lompat), 
        // kita pake weighted average kalo bedanya tipis.
        // Kalo bedanya gede (> 2 detik), langsung sinkron biar gak kelamaan.
        if (timeOffsetMillis == 0L || abs(newOffset - timeOffsetMillis) > 2000) {
            timeOffsetMillis = newOffset
            isTimeUnreliable = false // Sync confirmed, time is now reliable
            android.util.Log.d("TimeRepository", "Heavy Sync: Offset set to $newOffset ms (RTT: $roundTripLatency)")
        } else {
            // Weighted average: 80% old, 20% new untuk mencegah jitter
            timeOffsetMillis = (timeOffsetMillis * 0.8 + newOffset * 0.2).toLong()
            
            // Log hanya jika ada perubahan signifikan (> 50ms) biar gak nyepam log
            if (abs(newOffset - timeOffsetMillis) > 50) {
                android.util.Log.d("TimeRepository", "Smooth Sync: New target $newOffset ms, current smoothed: $timeOffsetMillis ms")
            }
        }
    }
    
    fun resetOffset() {
        timeOffsetMillis = 0L
    }
}
