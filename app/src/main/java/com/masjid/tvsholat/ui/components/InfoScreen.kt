package com.masjid.tvsholat.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InfoScreen(config: MasjidConfig) {
    val items = config.infoItems
    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (items.isNotEmpty()) {
            while (true) {
                delay(10000) // Rotate each 10 seconds
                currentIndex = (currentIndex + 1) % items.size
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D47A1), Color(0xFF002171)) // Modern Blue Gradient
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "INFORMASI MASJID",
                color = Color(0xFF90CAF9),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (items.isNotEmpty()) {
                val currentItem = items.getOrElse(currentIndex) { items.first() }
                
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        fadeIn().togetherWith(fadeOut())
                    },
                    label = "InfoTransition"
                ) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = item.title.uppercase(),
                                color = Color(0xFFFFD54F), // Amber
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = item.content,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                lineHeight = 40.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Belum ada informasi.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 24.sp
                )
            }
        }
        
        // Running Text at the very bottom
        RunningText(text = config.runningText)
    }
}
