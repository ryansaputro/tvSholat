package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import com.masjid.tvsholat.utils.HijriCalendar
import com.masjid.tvsholat.utils.IslamicEvent
import java.util.Date

@Composable
fun IslamicEventScreen(config: MasjidConfig, event: IslamicEvent, isTomorrow: Boolean, now: Date) {
    
    val hijriDate = HijriCalendar.toHijri(if (isTomorrow) Date(now.time + 24*3600000) else now)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF000000)) // Deep Purple / Gold Vibe
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            // Header Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isTomorrow) Color(0xFFD32F2F) else Color(0xFF388E3C)) // Red for Alert/Besok, Green for Today
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isTomorrow) "BESOK — ${hijriDate.day} ${hijriDate.getMonthName()}" else "HARI INI — ${hijriDate.day} ${hijriDate.getMonthName()}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "PERINGATAN HARI BESAR ISLAM",
                color = Color(0xFFFFD54F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Event Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Event Name
                    Text(
                        text = event.name.uppercase(),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 56.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Divider
                    Box(modifier = Modifier.width(100.dp).height(2.dp).background(Color(0xFFFFD54F)))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Description
                    Text(
                        text = "\"${event.description}\"",
                        color = Color(0xFFE1F5FE),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    )
                }
            }
        }
        
        // Anti Burn-in
        RunningText(text = config.runningText)
    }
}
