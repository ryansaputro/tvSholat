package com.masjid.tvsholat.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
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
import androidx.compose.ui.viewinterop.AndroidView
import com.masjid.tvsholat.data.MasjidConfig
import kotlinx.coroutines.delay
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale

import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InfoScreen(config: MasjidConfig, now: Date, runningText: String) {
    // Filter out empty items to prevent rendering issues
    val items = config.infoItems.filter { it.content.isNotEmpty() }
    var currentIndex by remember { mutableIntStateOf(0) }

    // Reset index if items change significantly (e.g. number of items changes)
    LaunchedEffect(items.size) {
        currentIndex = 0
    }

    // Calculate duration per item dynamically
    // If total duration is 15s and 3 items -> 5s per item
    val durationPerItem = if (items.isNotEmpty() && config.infoDisplayDuration > 0) {
        val calculated = config.infoDisplayDuration.toLong() / items.size
        calculated.coerceAtLeast(3L) // Minimum 3 seconds to be readable
    } else {
        5L // Default fallback
    }

    // Rotation Loop - use stable keys to avoid restarts during ticker recomposition
    LaunchedEffect(items.size, config.infoDisplayDuration) {
        if (items.isNotEmpty()) {
            while (true) {
                delay(durationPerItem * 1000L)
                if (items.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % items.size
                }
            }
        }
    }

    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada informasi.", color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp)
        }
        return
    }

    val currentItem = items.getOrElse(currentIndex) { items.first() }
    val isFullscreen = currentItem.type == "image"

    // 🔥 FAIL-SAFE CONTENT EXTRACTION
    val imageUrl = remember(currentItem) {
        if (isFullscreen) {
            val content = currentItem.content
            if (content.startsWith("http") || content.startsWith("/") || content.startsWith("content")) {
                content
            } else {
                // Try to extract from HTML if user saved it as HTML accidentally
                val srcRegex = """src=["']([^"']+)["']""".toRegex()
                srcRegex.find(content)?.groupValues?.get(1) ?: content
            }
        } else null
    }

    val backgroundColor = if (isFullscreen) Color.Black else Color.Transparent
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isFullscreen) Brush.verticalGradient(listOf(Color.Black, Color.Black))
                else Brush.radialGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF000000))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFullscreen) {
                Spacer(modifier = Modifier.height(32.dp))
                // Header Badge (Info Masjid)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0288D1)) // Blue for general info
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "INFORMASI MASJID",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            AnimatedContent(
                targetState = currentItem,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "InfoTransition",
                modifier = Modifier.weight(1f)
            ) { item ->
                Box(
                    modifier = if (isFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(42.dp)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (isFullscreen && imageUrl != null) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        SubcomposeAsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .allowHardware(false) // Better compatibility for some TVs
                                .build(),
                            contentDescription = "Poster",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
                                }
                            },
                            error = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Gagal Memuat Gambar", color = Color.White.copy(alpha = 0.5f))
                                        Text(imageUrl ?: "", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                                    }
                                }
                            }
                        )
                        
                        // 🔥 FLOATING CLOCK (Top Right)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = timeFormat.format(now),
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    } else {
                        // WEBVIEW FOR HTML RENDERING (TinyMCE content)
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    webViewClient = WebViewClient()
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        setSupportZoom(false)
                                        builtInZoomControls = false
                                    }
                                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                    setBackgroundColor(0) // Transparent
                                }
                            },
                            update = { webView ->
                                val htmlContent = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <style>
                                            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
                                            * { margin: 0; padding: 0; box-sizing: border-box; }
                                            html, body { 
                                                background: transparent; 
                                                color: white;
                                                font-family: 'Inter', -apple-system, sans-serif;
                                                font-size: 24px;
                                                line-height: 1.6;
                                                padding: 20px;
                                                overflow-x: hidden;
                                            }
                                            }
                                            h1, h2, h3 { 
                                                color: #FFD54F; 
                                                margin-bottom: 24px;
                                                font-weight: 900;
                                                text-transform: uppercase;
                                                border-bottom: 3px solid #FFD54F;
                                                padding-bottom: 8px;
                                                display: inline-block;
                                            }
                                            h1 { font-size: 2.2em; }
                                            h2 { font-size: 1.8em; }
                                            h3 { font-size: 1.5em; }
                                            p { 
                                                margin-bottom: 16px; 
                                                color: #E1F5FE;
                                                font-size: 22px;
                                                line-height: 1.5;
                                                font-weight: 500;
                                            }
                                            strong, b { 
                                                color: #FFD54F; 
                                                font-weight: 900;
                                            }
                                            ul, ol { 
                                                margin: 16px 0; 
                                                background: rgba(255,255,255,0.05);
                                                padding: 24px 24px 24px 50px;
                                                border-radius: 12px;
                                            }
                                            li { 
                                                margin: 12px 0;
                                                color: #E1F5FE;
                                                font-size: 24px;
                                            }
                                            img {
                                                max-width: 100%;
                                                border-radius: 16px;
                                                margin: 20px 0;
                                                box-shadow: 0 4px 20px rgba(0,0,0,0.5);
                                            }
                                        </style>
                                    </head>
                                    <body>
                                        <div style="display: flex; flex-direction: column; justify-content: center; min-height: 100%;">
                                            ${if (item.title.isNotEmpty()) "<h1>${item.title}</h1>" else ""}
                                            ${item.content}
                                        </div>
                                    </body>
                                    </html>
                                """.trimIndent()
                                
                                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            
            // Running Text at the very bottom, hide if fullscreen image
            if (!isFullscreen) {
                RunningText(text = runningText)
            }
        }
    }
}
