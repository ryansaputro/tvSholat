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
        // Simple Network Time Protocol (SNTP) simplified:
        // RemoteTime = MasterTime + Latency/2
        // Offset = RemoteTime - LocalTime
        
        val estimatedMasterTime = masterTimeMillis + (roundTripLatency / 2)
        val newOffset = estimatedMasterTime - now

        // Only update if difference is noticeable (> 500ms) to avoid jitter
        // or if we haven't synced yet (offset is 0)
        if (timeOffsetMillis == 0L || abs(newOffset - timeOffsetMillis) > 200) {
            timeOffsetMillis = newOffset
            android.util.Log.d("TimeRepository", "Time synced. Offset logic: $newOffset ms (Master: $masterTimeMillis, Local: $now)")
        }
    }
    
    fun resetOffset() {
        timeOffsetMillis = 0L
    }
}
