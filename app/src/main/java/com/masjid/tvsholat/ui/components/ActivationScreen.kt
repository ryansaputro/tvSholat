package com.masjid.tvsholat.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.masjid.tvsholat.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock

@Composable
fun ActivationScreen(
    deviceId: String, 
    botUsername: String = "TvSholatBot",
    onRefresh: () -> Unit = {}
) {
    val botUrl = "https://t.me/$botUsername?start=$deviceId"
    val qrBitmap = remember(botUrl) { generateQRCode(botUrl, 400) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(500.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceColor.copy(alpha = 0.9f))
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "AKTIVASI PERANGKAT",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen,
                    letterSpacing = 2.sp
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Silakan hubungi Admin untuk mengaktifkan aplikasi di TV ini.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = DeepGreen.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Device ID Box
            Surface(
                color = DeepGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DEVICE ID", style = MaterialTheme.typography.labelSmall, color = DeepGreen)
                    Text(
                        text = deviceId,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepGreen
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // QR Code
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(180.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code Activation",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Gold,
                trackColor = Gold.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Menunggu persetujuan...",
                style = MaterialTheme.typography.labelLarge,
                color = DeepGreen.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CEK STATUS AKTIVASI",
                    color = DeepGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun generateQRCode(text: String, size: Int): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
