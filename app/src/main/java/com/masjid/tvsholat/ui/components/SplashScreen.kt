package com.masjid.tvsholat.ui.components

import androidx.compose.animation.core.*
import com.masjid.tvsholat.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
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
    val fullTextMain = "MARS"
    val fullTextSub = "DEVELOPER"
    var displayedTextMain by remember { mutableStateOf("") }
    var displayedTextSub by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Blinking cursor logic
        launch {
            while(true) {
                delay(500)
                showCursor = !showCursor
            }
        }

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
                animationSpec = tween(1000, easing = LinearOutSlowInEasing)
            )
        }
        
        // Typewriter Effect for MARS
        fullTextMain.forEachIndexed { index, _ ->
            displayedTextMain = fullTextMain.substring(0, index + 1)
            delay(150)
        }
        
        delay(300)
        
        // Typewriter Effect for DEVELOPER
        fullTextSub.forEachIndexed { index, _ ->
            displayedTextSub = fullTextSub.substring(0, index + 1)
            delay(100)
        }

        // Hold for a moment
        delay(2000)
        
        // Fade out before finishing
        alpha.animateTo(0f, tween(800))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF000000))
                )
            ),
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
                text = displayedTextMain + (if (displayedTextSub.isEmpty() && showCursor) "|" else ""),
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 12.sp
            )
            Text(
                text = displayedTextSub + (if (displayedTextSub.isNotEmpty() && showCursor) "|" else ""),
                color = Gold,
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
