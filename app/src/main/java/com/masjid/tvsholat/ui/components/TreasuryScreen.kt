package com.masjid.tvsholat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig

@Composable
fun TreasuryScreen(config: MasjidConfig) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B5E20), Color(0xFF0D2D10))
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
            Text(
                text = "LAPORAN KEUANGAN",
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = config.name.uppercase(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Balance Card
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = config.treasuryDescription.uppercase(),
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rp ${config.treasuryBalance}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rekening & QRIS Section
            if (config.treasuryAccountInfo.isNotEmpty() || config.treasuryQrisData.isNotEmpty()) {
                if (config.treasuryAccountInfo.isNotEmpty() && config.treasuryQrisData.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.98f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Account Info & Syukron (Left Side)
                        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TRANSFER :",
                                color = Color(0xFFFFD54F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = config.treasuryAccountInfo,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Syukran jazakumullah khairan katsiran atas infaq/shadaqah Bapak/Ibu sekalian.\nSemoga menjadi amal jariyah yang berlipat ganda.",
                                color = Color(0xFFA5D6A7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // QRIS (Right Side)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "QRIS :",
                                color = Color(0xFFFFD54F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(4.dp)
                            ) {
                                QrPanel(content = config.treasuryQrisData, size = 122)
                            }
                        }
                    }
                } else {
                    // Only one exists, use Column and include Syukron
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        if (config.treasuryAccountInfo.isNotEmpty()) {
                            Text(
                                text = "TRANSFER :",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = config.treasuryAccountInfo,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        if (config.treasuryQrisData.isNotEmpty()) {
                            Text(
                                text = "SCAN QRIS :",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            ) {
                                QrPanel(content = config.treasuryQrisData, size = 144)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Syukran jazakumullah khairan katsiran atas infaq/shadaqah Bapak/Ibu sekalian.\nSemoga menjadi amal jariyah yang berlipat ganda.",
                            color = Color(0xFFA5D6A7),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // No info/qris, just show Syukron at bottom of column
                Text(
                    text = "Syukran jazakumullah khairan katsiran atas infaq/shadaqah Bapak/Ibu sekalian.\nSemoga menjadi amal jariyah yang berlipat ganda.",
                    color = Color(0xFFA5D6A7),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
        
        // Anti Burn-in
        RunningText(text = config.runningText)
    }
}
