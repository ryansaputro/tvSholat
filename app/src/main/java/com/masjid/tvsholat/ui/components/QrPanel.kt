package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.utils.QrCodeUtils

@Composable
fun QrPanel(content: String, size: Int = 100) {
    val bitmap = remember(content) {
        QrCodeUtils.generateQrBitmap(content, size * 2) // Higher res for better scanning
    }

    bitmap?.let {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(2.dp)
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code Admin",
                    modifier = Modifier.size(size.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            androidx.compose.material3.Text(
                text = "Scan Admin",
                color = Color.Gray,
                fontSize = (size / 6).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
