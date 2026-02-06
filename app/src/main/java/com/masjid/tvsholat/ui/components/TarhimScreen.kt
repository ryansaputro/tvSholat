package com.masjid.tvsholat.ui.components

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
import com.masjid.tvsholat.data.MasjidConfig
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TarhimScreen(config: MasjidConfig) {
    val tarhimParts = listOf(
        Pair(
            "الصَّلَاةُ وَالسَّلَامُ عَلَيْكَ ۞ يَا إِمَامَ الْمُجَاهِدِيْنَ ۞ يَا رَسُوْلَ اللهِ ۞ الصَّلَاةُ وَالسَّلَامُ عَلَيْكَ ۞ يَا نَاصِرَ الْهُدَى ۞ يَا خَيْرَ خَلْقِ اللهِ ۞",
            "Sholawat dan salam semoga tercurahkan kepadamu, hai pemimpin para pejuang, wahai Rasulullah. Sholawat dan salam semoga tercurahkan kepadamu, wahai penolong petunjuk (agama), wahai makhluk Allah yang terbaik."
        ),
        Pair(
            "الصَّلَاةُ وَالسَّلَامُ عَلَيْكَ ۞ يَا نَاصِرَ الْحَقِّ يَا رَسُوْلَ اللهِ ۞ الصَّلَاةُ وَالسَّلَامُ عَلَيْكَ ۞ يَا مَنْ اَسْرَى بِكَ اللَّهُ لَيْلًا ۞ سُبْحَانَ مَنْ اَسْرَى بِكَ ۞",
            "Sholawat dan salam semoga tercurahkan kepadamu, wahai penolong kebenaran, wahai Rasulullah. Sholawat dan salam semoga tercurahkan kepadamu, wahai orang yang telah diperjalankan oleh Allah di malam hari. Maha suci Allah yang telah memperjalankanmu."
        ),
        Pair(
            "نِلْتَ مَا نِلْتَ وَالْأَنَامُ نِيَامُ ۞ وَتَقَدَّمْتَ لِلصَّلَاةِ فَصَلَّى ۞ كُلُّ مَنْ فِى السَّمَاءِ وَاَنْتَ الْإِمَامُ ۞",
            "Engkau memperoleh apa yang engkau peroleh sementara manusia sedang tidur. Dan engkau maju untuk sholat, maka seluruh penghuni langit bermakmum kepadamu dan engkau menjadi imamnya."
        ),
        Pair(
            "وَاِلَى الْمُنْتَهَى رُفِعْتَ كَرِيْمًا ۞ وَسَمِعْتَ نِدَاءً عَلَيْكَ السَّلَامُ ۞ يَا كَرِيْمَ الْأَخْلَاقِ يَا رَسُوْلَ اللهِ ۞",
            "Engkau dinaikkan ke Sidratul Muntaha dengan mulia, dan engkau mendengar suara: “Salam sejahtera untukmu”. Wahai manusia yang mulia akhlaknya, wahai Rasulullah."
        )
    )

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // 10 detik per slide
            currentIndex = (currentIndex + 1) % tarhimParts.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A237E), Color(0xFF000000)) // Deep Blue / Night Vibe
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Header Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF3949AB))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "SHOLAWAT TARHIM",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TarhimTransition"
            ) { index ->
                val part = tarhimParts[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(42.dp)
                ) {
                    // Arabic
                    Text(
                        text = part.first,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 56.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Divider
                    Box(modifier = Modifier.width(100.dp).height(2.dp).background(Color(0xFFFFD54F)))

                    Spacer(modifier = Modifier.height(32.dp))

                    // Indonesian
                    Text(
                        text = part.second,
                        color = Color(0xFFB2EBF2),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Subtle indication of progress
            Row {
                tarhimParts.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (idx == currentIndex) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
        
        // Running Text at bottom
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            RunningText(text = config.runningText)
        }
    }
}
