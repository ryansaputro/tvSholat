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

data class Hadith(val arabic: String, val text: String, val narrator: String)

val hadithList = listOf(
    Hadith("خَيْرُ النَّاسِ أَنْفَعُهُمْ لِلنَّاسِ", "Sebaik-baik manusia adalah yang paling bermanfaat bagi manusia.", "HR. Thabrani"),
    Hadith("اَلطُّهُورُ شَطْرُ الْإِيمَانِ", "Kebersihan itu sebagian dari iman.", "HR. Muslim"),
    Hadith("تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ", "Senyummu di hadapan saudaramu adalah sedekah.", "HR. Tirmidzi"),
    Hadith("مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ طَرِيقًا إِلَى الْجَنَّةِ", "Barangsiapa yang menempuh jalan untuk mencari ilmu, maka Allah akan memudahkan baginya jalan ke surga.", "HR. Muslim"),
    Hadith("الدُّنْيَا مَتَاعٌ وَخَيْرُ مَتَاعِ الدُّنْيَا الْمَرْأَةُ الصَّالِحَةُ", "Dunia adalah perhiasan, dan sebaik-baik perhiasan dunia adalah wanita shalihah.", "HR. Muslim"),
    Hadith("بَلِّغُوا عَنِّي وَلَوْ آيَةً", "Sampaikanlah dariku walau hanya satu ayat.", "HR. Bukhari"),
    Hadith("إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ", "Sesungguhnya setiap amalan tergantung pada niatnya.", "HR. Bukhari & Muslim"),
    Hadith("اليَدُ العُلْيَا خَيْرٌ مِنَ اليَدِ السُّفْلَى", "Tangan di atas lebih baik daripada tangan di bawah.", "HR. Bukhari & Muslim"),
    Hadith("لَيْسَ الشَّدِيدُ بِالصُّرَعَةِ إِنَّمَا الشَّدِيدُ الَّذِي يَمْلِكُ نَفْسَهُ عِنْدَ الْغَضَبِ", "Bukanlah orang yang kuat itu yang pandai bergulat, tetapi orang yang kuat adalah yang mampu menahan dirinya saat marah.", "HR. Bukhari & Muslim"),
    Hadith("مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ", "Siapa yang beriman kepada Allah dan hari akhir, hendaklah ia berkata baik atau diam.", "HR. Bukhari & Muslim"),
    Hadith("الْجَنَّةُ تَحْتَ أَقْدَامِ الأُمَّهَاتِ", "Surga di bawah telapak kaki ibu.", "HR. Ahmad"),
    Hadith("أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ الصَّلاةُ عَلَى وَقْتِهَا", "Pekerjaan yang paling dicintai Allah adalah sholat pada waktunya.", "HR. Bukhari & Muslim"),
    Hadith("لاَ يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ", "Tidak beriman salah seorang di antara kalian sampai ia mencintai saudaranya sebagaimana ia mencintai dirinya sendiri.", "HR. Bukhari & Muslim"),
    Hadith("إِنَّ اللَّهَ لا يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ", "Allah tidak melihat kepada rupa dan harta kalian, tetapi Allah melihat kepada hati dan amal kalian.", "HR. Muslim"),
    Hadith("الْحَيَاءُ شُعْبَةٌ مِنَ الإِيمَانِ", "Malu itu sebagian dari iman.", "HR. Bukhari & Muslim"),
    Hadith("خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ", "Sebaik-baik kalian adalah yang mempelajari Al-Qur'an dan mengajarkannya.", "HR. Bukhari"),
    Hadith("أَنْ تَعْبُدَ اللَّهَ كَأَنَّكَ تَرَاهُ فَإِنْ لَمْ تَكُنْ تَرَاهُ فَإِنَّهُ يَرَاكَ", "Beribadahlah kepada Allah seolah-olah kamu melihat-Nya. Jika kamu tidak melihat-Nya, sesungguhnya Dia melihatmu.", "HR. Muslim"),
    Hadith("كُلُّكُمْ رَاعٍ وَكُلُّكُمْ مَسْئُولٌ عَنْ رَعِيَّتِهِ", "Setiap kalian adalah pemimpin, dan setiap pemimpin akan dimintai pertanggungjawaban atas kepemimpinannya.", "HR. Bukhari & Muslim"),
    Hadith("يَسِّرُوا وَلا تُعَسِّرُوا وَبَشِّرُوا وَلا تُنَفِّرُوا", "Mudahkanlah dan jangan dipersulit, berilah kabar gembira dan jangan membuat orang lari.", "HR. Bukhari & Muslim"),
    Hadith("كَفَى بِالْمَرْءِ كَذِبًا أَنْ يُحَدِّثَ بِكُلِّ مَا سَمِعَ", "Cukuplah seseorang dikatakan berdusta jika ia menceritakan setiap apa yang ia dengar.", "HR. Muslim"),
    Hadith("مَنْ بَنَى مَسْجِدًا لِلَّهِ بَنَى اللَّهُ لَهُ بَيْتًا فِي الْجَنَّةِ", "Siapa yang membangun masjid karena Allah, maka Allah akan membangunkan baginya rumah di surga.", "HR. Bukhari & Muslim"),
    Hadith("وَهَلْ يَكُبُّ النَّاسَ فِي النَّارِ عَلَى وُجُوهِهِمْ إِلَّا حَصَائِدُ أَلْسِنَتِهِمْ", "Lidah adalah bagian tubuh yang paling banyak menyebabkan manusia masuk neraka.", "HR. Tirmidzi"),
    Hadith("مَنْ اَمْسَى كَالًّا مِنْ عَمَلِ يَدَيْهِ اَمْسَى مَغْفُوْرًا لَهُ", "Barang siapa di waktu sore merasa lelah karena mencari nafkah, maka di waktu itu ia diampuni dosanya.", "HR. Thabrani"),
    Hadith("مَا أَكَلَ أَحَدٌ طَعَامًا قَطُّ خَيْرًا مِنْ أَنْ يَأْكُلَ مِنْ عَمَلِ يَدِهِ", "Tidaklah seseorang makan makanan yang lebih baik daripada hasil usahanya (bekerja) sendiri.", "HR. Bukhari"),
    Hadith("دِيْنارٌ أَنْفَقْتَهُ عَلَى أَهْلِكَ أَعْظَمُهَا أَجْرًا", "Dinar yang engkau nafkahkan kepada keluargamu; maka itulah yang paling besar pahalanya.", "HR. Muslim")
)

@Composable
fun HadithScreen(config: MasjidConfig, now: Date, runningText: String) {
    // Pick a random hadith every time the screen is shown
    val hadith = remember { hadithList.random() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF000000))
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 48.dp, vertical = 24.dp)
        ) {
            // Header Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE65100)) // Deep Orange for Wisdom/Hadith
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "KUTIPAN HADITS HARIAN",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Hadith Box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Arabic Text
                    Text(
                        text = hadith.arabic,
                        color = Color(0xFFFFD54F),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Translation
                    Text(
                        text = "\"${hadith.text}\"",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Narrator
                    Text(
                        text = "- ${hadith.narrator} -",
                        color = Color(0xFFA5D6A7),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(config.name.uppercase(), color = Color.Gray, fontSize = 14.sp)
        }
        
        // Anti Burn-in
        RunningText(text = runningText)
    }
}
