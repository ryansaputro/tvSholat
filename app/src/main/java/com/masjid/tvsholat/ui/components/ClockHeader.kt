package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockHeader(now: Date) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = SimpleDateFormat("HH:mm:ss").format(now),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).format(now),
            fontSize = 24.sp,
            color = Color.LightGray
        )
    }
}
