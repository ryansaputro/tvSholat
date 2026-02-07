package com.masjid.tvsholat.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.masjid.tvsholat.data.MasjidConfig
import java.io.File

@Composable
fun MasjidLogo(
    config: MasjidConfig,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val model = when {
        config.logoLocalPath.isNotEmpty() && File(config.logoLocalPath).exists() -> {
            File(config.logoLocalPath)
        }
        config.logoUrl.isNotEmpty() -> {
            config.logoUrl
        }
        else -> null
    }

    if (model != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = "Logo Masjid",
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit
        )
    }
}
