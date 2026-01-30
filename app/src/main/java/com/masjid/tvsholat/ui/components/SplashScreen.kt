package com.masjid.tvsholat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animation States
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Parallel animations: Scale up and Fade in
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1500, easing = LinearOutSlowInEasing)
            )
        }
        
        // Hold for a moment
        delay(3000)
        
        // Fade out before finishing
        alpha.animateTo(0f, tween(800))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00241B)), // Deep Green Mosque Theme
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
        ) {
            Text(
                text = "MARS",
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 12.sp
            )
            Text(
                text = "DEVELOPER",
                color = Color(0xFFFFD54F), // Gold
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
            
            Spacer(modifier = Modifier.height(60.dp))
            
            // Subtle loading bar or element
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}
