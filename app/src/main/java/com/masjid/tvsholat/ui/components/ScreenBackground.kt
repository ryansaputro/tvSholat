package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.masjid.tvsholat.R

@Composable
fun ScreenBackground(content: @Composable () -> Unit) {

    Box(modifier = Modifier.fillMaxSize()) {

        // GAMBAR MASJID
        Image(
            painter = painterResource(id = R.drawable.bg_masjid),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // OVERLAY GELAP (BIAR TEKS KEBACA)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // CONTENT UTAMA
        content()
    }
}
