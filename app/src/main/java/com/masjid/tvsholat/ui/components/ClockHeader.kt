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
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockHeader(now: Date) {
    // Sync Hijri with 'now' clock (which already includes the offset)
    val localDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val hijri = HijrahDate.from(localDate)
    val hijriFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))
    val hijriFormatted = hijri.format(hijriFormatter) + " H"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = SimpleDateFormat("HH:mm:ss").format(now),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(now),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = hijriFormatted,
                fontSize = 18.sp,
                color = Color(0xFFFFD54F)
            )
        }
    }
}
