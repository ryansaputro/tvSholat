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
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.masjid.tvsholat.R
import java.io.File

@Composable
fun ScreenBackground(
    backgroundUrl: String,
    backgroundType: String = "url",
    backgroundLocalPath: String = "",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {

        // Determine which image to show based on type
        val imageData = when (backgroundType) {
            "upload" -> {
                if (backgroundLocalPath.isNotEmpty()) {
                    val file = File(backgroundLocalPath)
                    if (file.exists()) file.toUri() else null
                } else null
            }
            else -> backgroundUrl.takeIf { it.isNotEmpty() }
        }

        if (imageData != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.bg_masjid),
                placeholder = painterResource(id = R.drawable.bg_masjid)
            )
        } else {
            // GAMBAR MASJID DEFAULT
            Image(
                painter = painterResource(id = R.drawable.bg_masjid),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

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
