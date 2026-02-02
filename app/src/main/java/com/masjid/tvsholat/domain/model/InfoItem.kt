package com.masjid.tvsholat.domain.model

data class InfoItem(
    val title: String,
    val content: String,
    val type: String = "text" // "text" (Rich Text) or "image" (Fullscreen Poster)
)
