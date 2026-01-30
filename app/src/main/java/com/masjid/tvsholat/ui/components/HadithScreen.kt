package com.masjid.tvsholat.ui.components

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masjid.tvsholat.data.MasjidConfig
import java.util.*

data class Hadith(val text: String, val narrator: String)

val hadithList = listOf(
    Hadith("Sebaik-baik manusia adalah yang paling bermanfaat bagi manusia.", "HR. Ahmad"),
    Hadith("Kebersihan itu sebagian dari iman.", "HR. Muslim"),
    Hadith("Senyummu di hadapan saudaramu adalah sedekah.", "HR. Tirmidzi"),
    Hadith("Barangsiapa yang menempuh jalan untuk mencari ilmu, maka Allah akan memudahkan baginya jalan ke surga.", "HR. Muslim"),
    Hadith("Dunia adalah perhiasan, dan sebaik-baik perhiasan dunia adalah wanita shalihah.", "HR. Muslim"),
    Hadith("Sampaikanlah dariku walau hanya satu ayat.", "HR. Bukhari"),
    Hadith("Sesungguhnya setiap amalan tergantung pada niatnya.", "HR. Bukhari & Muslim"),
    Hadith("Tangan di atas lebih baik daripada tangan di bawah.", "HR. Bukhari & Muslim"),
    Hadith("Bukanlah orang yang kuat itu yang pandai bergulat, tetapi orang yang kuat adalah yang mampu menahan dirinya saat marah.", "HR. Bukhari & Muslim"),
    Hadith("Siapa yang beriman kepada Allah dan hari akhir, hendaklah ia berkata baik atau diam.", "HR. Bukhari & Muslim"),
    Hadith("Surga di bawah telapak kaki ibu.", "HR. Ahmad"),
    Hadith("Pekerjaan yang paling dicintai Allah adalah sholat pada waktunya.", "HR. Bukhari & Muslim"),
    Hadith("Tidak beriman salah seorang di antara kalian sampai ia mencintai saudaranya sebagaimana ia mencintai dirinya sendiri.", "HR. Bukhari & Muslim"),
    Hadith("Allah tidak melihat kepada rupa dan harta kalian, tetapi Allah melihat kepada hati dan amal kalian.", "HR. Muslim"),
    Hadith("Malu itu sebagian dari iman.", "HR. Bukhari & Muslim"),
    Hadith("Sebaik-baik kalian adalah yang mempelajari Al-Qur'an dan mengajarkannya.", "HR. Bukhari"),
    Hadith("Beribadahlah kepada Allah seolah-olah kamu melihat-Nya. Jika kamu tidak melihat-Nya, sesungguhnya Dia melihatmu.", "HR. Muslim"),
    Hadith("Setiap kalian adalah pemimpin, dan setiap pemimpin akan dimintai pertanggungjawaban atas kepemimpinannya.", "HR. Bukhari & Muslim"),
    Hadith("Mudahkanlah dan jangan dipersulit, berilah kabar gembira dan jangan membuat orang lari.", "HR. Bukhari & Muslim"),
    Hadith("Cukuplah seseorang dikatakan berdusta jika ia menceritakan setiap apa yang ia dengar.", "HR. Muslim"),
    Hadith("Siapa yang membangun masjid karena Allah, maka Allah akan membangunkan baginya rumah di surga.", "HR. Bukhari & Muslim"),
    Hadith("Lidah adalah bagian tubuh yang paling banyak menyebabkan manusia masuk neraka.", "HR. Tirmidzi")
)

@Composable
fun HadithScreen(config: MasjidConfig, now: Date) {
    // Pick a random hadith every time the screen is shown
    val hadith = remember { hadithList.random() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF004D40), Color(0xFF00241B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp)
        ) {
            Text(
                text = "KUTIPAN HADITS HARIAN",
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Hadith Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\"${hadith.text}\"",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        lineHeight = 44.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "- ${hadith.narrator} -",
                        color = Color(0xFFFFD54F),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(config.name.uppercase(), color = Color.Gray, fontSize = 16.sp)
        }
        
        // Anti Burn-in
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            RunningText(text = config.runningText)
        }
    }
}
