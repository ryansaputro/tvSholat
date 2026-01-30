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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B5E20), Color(0xFF0D2D10))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "LAPORAN KEUANGAN",
                color = Color(0xFFFFD54F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = config.name.uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Balance Card
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = config.treasuryDescription.uppercase(),
                        color = Color.LightGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rp ${config.treasuryBalance}",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Rekening & QRIS Section
            if (config.treasuryAccountInfo.isNotEmpty() || config.treasuryQrisData.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (config.treasuryQrisData.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            QrPanel(content = config.treasuryQrisData, size = 124)
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                    
                    if (config.treasuryAccountInfo.isNotEmpty()) {
                        Column {
                            Text(
                                text = "INFAQ & SHADAQAH VIA TRANSFER:",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = config.treasuryAccountInfo,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
            
            Text(
                text = "Syukran jazakumullah khairan katsiran atas infaq/shadaqah Bapak/Ibu sekalian.\nSemoga menjadi amal jariyah yang berlipat ganda.",
                color = Color(0xFFA5D6A7),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
        
        // Anti Burn-in for this screen too
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            RunningText(text = config.runningText)
        }
    }
}
