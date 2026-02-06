package com.masjid.tvsholat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.utils.QrCodeUtils
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun TreasuryScreen(config: MasjidConfig) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF000000))
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .padding(24.dp)
        ) {
            // Header Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(50))
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LAPORAN KEUANGAN",
                    color = Color(0xFFFFD54F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = config.name.uppercase(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Balance Card (Glassmorphism)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 30.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = config.treasuryDescription.uppercase(),
                        color = Color(0xFFA5D6A7),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rp ${config.treasuryBalance}",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Rekening & QRIS Section
            if (config.treasuryAccountInfo.isNotBlank() || config.treasuryQrisData.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Account Info & Gratitude
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (config.treasuryAccountInfo.isNotBlank()) {
                            Text(
                                text = "INFORMASI TRANSFER :",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = config.treasuryAccountInfo,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Start,
                                    lineHeight = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        Text(
                            text = "Syukran jazakumullah khairan katsiran atas infaq/shadaqah Bapak/Ibu sekalian.\nSemoga menjadi amal jariyah yang berlipat ganda.",
                            color = Color(0xFFA5D6A7),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            lineHeight = 22.sp
                        )
                    }
                    
                    if (config.treasuryQrisData.isNotBlank()) {
                        Spacer(modifier = Modifier.width(32.dp))
                        // Right Column: QRIS Template
                        QrisTemplate(
                            merchantName = config.name,
                            nmid = "ID0309494049908", 
                            content = config.treasuryQrisData, 
                            qrResolution = 400
                        )
                    }
                }
            } else {
                // No info/qris, just show Syukron
                Text(
                    text = "Syukran jazakumullah khairan katsiran atas infaq/shadaqah Bapak/Ibu sekalian.\nSemoga menjadi amal jariyah yang berlipat ganda.",
                    color = Color(0xFFA5D6A7),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Left,
                    lineHeight = 32.sp
                )
            }
        }
        
        // Anti Burn-in
        RunningText(text = config.runningText)
    }
}

@Composable
fun QrisTemplate(merchantName: String, nmid: String, content: String, qrResolution: Int) {
    Box(
        modifier = Modifier
            .width(160.dp) 
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(bottom = 0.dp)
    ) {
        // Geometric Background Decorations (Red Accents)
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height

            // 1. Left Red Arrow/Strip
            val leftArrow = Path().apply {
                moveTo(0f, 0.25f * h)
                lineTo(0.12f * w, 0.3f * h)
                lineTo(0.12f * w, 0.5f * h)
                lineTo(0f, 0.55f * h)
                close()
            }
            drawPath(leftArrow, Color(0xFFD32F2F))

            // 2. Bottom-Right Red Accent (The Shard)
            val bottomRightShard = Path().apply {
                moveTo(w, 0.85f * h)
                lineTo(0.85f * w, h)
                lineTo(w, h)
                close()
            }
            drawPath(bottomRightShard, Color(0xFFD32F2F))
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row (Add padding here manually)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // QRIS Label
                Column {
                    Text(
                        text = "QRIS",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "QR Code Standar\nPembayaran Nasional",
                        color = Color.Black,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 7.sp
                    )
                }

                // GPN Logo Placeholder
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(20.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Simple Red Eagle shape
                            val eagle = Path().apply {
                                moveTo(size.width * 0.5f, 0f)
                                lineTo(size.width, size.height * 0.5f)
                                lineTo(size.width * 0.7f, size.height)
                                lineTo(size.width * 0.3f, size.height)
                                lineTo(0f, size.height * 0.5f)
                                close()
                            }
                            drawPath(eagle, Color(0xFFD32F2F))
                        }
                    }
                    Text("GPN", color = Color(0xFF0D47A1), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Merchant Name (Hardcoded or Dynamic)
            Text(
                text = merchantName,
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            // QR Code Box (NGEPAS - MUST TOUCH EDGES)
            Box(
                modifier = Modifier
                    .fillMaxWidth() 
                    .aspectRatio(1f)
                    .background(Color.White)
                    .padding(0.dp) 
            ) {
                val bitmap = remember(content) {
                    QrCodeUtils.generateQrBitmap(content, qrResolution)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QRIS",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Footer (Add padding here)
            Text(
                text = "Dicetak oleh: $merchantName",
                color = Color.DarkGray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            )
        }
    }
}
