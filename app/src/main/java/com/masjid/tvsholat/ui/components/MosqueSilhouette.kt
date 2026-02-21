package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun MosqueSilhouette(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.1f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.9f
        
        val mosquePath = Path().apply {
            // Main Base
            moveTo(w * 0.1f, groundY)
            lineTo(w * 0.9f, groundY)
            
            // Main Dome
            val domeR = w * 0.15f
            val domeCenterX = w * 0.5f
            val domeBaseY = groundY - 100f
            moveTo(domeCenterX - domeR, domeBaseY)
            quadraticTo(domeCenterX, domeBaseY - domeR * 1.5f, domeCenterX + domeR, domeBaseY)
            
            // Minaret Left
            moveTo(w * 0.25f, groundY)
            lineTo(w * 0.25f, groundY - 250f)
            lineTo(w * 0.28f, groundY - 280f)
            lineTo(w * 0.31f, groundY - 250f)
            lineTo(w * 0.31f, groundY)
            
            // Minaret Right
            moveTo(w * 0.69f, groundY)
            lineTo(w * 0.69f, groundY - 250f)
            lineTo(w * 0.72f, groundY - 280f)
            lineTo(w * 0.75f, groundY - 250f)
            lineTo(w * 0.75f, groundY)
            
            // Side Domes
            val sideDomeR = w * 0.08f
            moveTo(w * 0.35f - sideDomeR, domeBaseY + 20f)
            quadraticTo(w * 0.35f, domeBaseY - sideDomeR, w * 0.35f + sideDomeR, domeBaseY + 20f)
            
            moveTo(w * 0.65f - sideDomeR, domeBaseY + 20f)
            quadraticTo(w * 0.65f, domeBaseY - sideDomeR, w * 0.65f + sideDomeR, domeBaseY + 20f)
        }
        
        drawPath(
            path = mosquePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color, color.copy(alpha = 0.2f)),
                startY = h * 0.5f,
                endY = groundY
            )
        )
    }
}
