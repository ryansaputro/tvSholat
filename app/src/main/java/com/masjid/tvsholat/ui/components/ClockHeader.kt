package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.utils.HijriCalendar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockHeader(now: Date) {
    // Use local HijriCalendar utility which is compatible with API 24
    val hijri = HijriCalendar.toHijri(now)
    val hijriFormatted = "${hijri.day} ${hijri.getMonthName()} ${hijri.year} H"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
            fontSize = 56.sp, // Restored original size
            fontWeight = FontWeight.Bold,
            color = Color.White,
            softWrap = false,
            maxLines = 1
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.forLanguageTag("id")).format(now),
                fontSize = 20.sp, // Restored original size
                fontWeight = FontWeight.Bold,
                color = Color.White,
                softWrap = false
            )
            Text(
                text = hijriFormatted,
                fontSize = 18.sp, // Restored original size
                color = Color(0xFFFFD54F)
            )
        }
    }
}
