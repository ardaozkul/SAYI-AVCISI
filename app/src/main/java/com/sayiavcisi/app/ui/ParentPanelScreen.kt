package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.data.ParentStatsSnapshot
import java.util.Locale

/**
 * Ebeveyn Paneli (Bölüm 14):
 * - Hangi gün tamamlandı?
 * - Toplam kaç işlem yaptı?
 * - Toplam doğru sayısı & Yanlış cevap sayısı
 * - Toplama, Çıkarma, Çarpma, Bölme başarı oranları (%)
 * - En çok zorlandığı işlemler
 * - Çarpım tablosunda hangi sayılarda zorlandığı (0..10)
 * - Ortalama cevap süresi
 * - Günlük oyun süresi
 * - Ebeveyn, süre sınırını ve ses ayarlarını değiştirebilmeli.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentPanelScreen(
    stats: ParentStatsSnapshot,
    onToggleSound: (Boolean) -> Unit,
    onToggleRelaxedMode: (Boolean) -> Unit,
    onChangeExtraTime: (Int) -> Unit,
    onResetDailyScore: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val playMinutes = stats.dailyPlayTimeSeconds / 60
    val playSeconds = stats.dailyPlayTimeSeconds % 60
    val formattedAvgTime = String.format(Locale.US, "%.1f sn", stats.averageResponseTimeSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Üst Başlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "👨‍👩‍👧 EBEVEYN PANELİ",
                    color = Color(0xFFFBBF24),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Detaylı Öğrenme Analizi ve Oyun Ayarları",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("← Oyuna Dön", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. 5 Günlük Eğitim Programı Tamamlanma Durumu
        SectionCard(title = "📅 5 Günlük Macera İlerlemesi") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (day in 1..5) {
                    val isDone = stats.completedDays.contains(day)
                    val stagesDone = (1..6).count { s -> stats.completedLevelIds.contains("D${day}_S${s}") }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDone) Color(0xFF065F46) else Color(0xFF1E293B))
                            .border(
                                1.dp,
                                if (isDone) Color(0xFF34D399) else Color(0xFF475569),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isDone) "✅" else "⏳",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "$day. Gün",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$stagesDone/6 B.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Genel İşlem, Doğru/Yanlış ve Süre Özeti
        SectionCard(title = "📊 Genel Performans ve Süre") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    label = "Toplam İşlem",
                    value = "${stats.totalOperations}",
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Doğru Sayısı",
                    value = "${stats.totalCorrect}",
                    color = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Yanlış Sayısı",
                    value = "${stats.totalWrong}",
                    color = Color(0xFFF87171),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBox(
                    label = "Ort. Cevap Süresi",
                    value = formattedAvgTime,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Günlük Oyun Süresi",
                    value = "${playMinutes} dk ${playSeconds} sn",
                    color = Color(0xFFA78BFA),
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    label = "Günlük Puan",
                    value = "${stats.dailyScore} / 100",
                    color = Color(0xFFF472B6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Dört İşlem Başarı Oranları
        SectionCard(title = "🎯 İşlem Türlerine Göre Başarı Oranları") {
            SkillRateBar(
                title = "Toplama Başarı Oranı (+)",
                rate = stats.additionRate,
                detail = noDataTextOr(stats.additionTotal) { "${stats.additionCorrect} / ${stats.additionTotal} doğru" },
                barColor = Color(0xFF10B981)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkillRateBar(
                title = "Çıkarma Başarı Oranı (-)",
                rate = stats.subtractionRate,
                detail = noDataTextOr(stats.subtractionTotal) { "${stats.subtractionCorrect} / ${stats.subtractionTotal} doğru" },
                barColor = Color(0xFFF59E0B)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkillRateBar(
                title = "Çarpma Başarı Oranı (×)",
                rate = stats.multiplicationRate,
                detail = noDataTextOr(stats.multiplicationTotal) { "${stats.multiplicationCorrect} / ${stats.multiplicationTotal} doğru" },
                barColor = Color(0xFF8B5CF6)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkillRateBar(
                title = "Bölme Başarı Oranı (÷)",
                rate = stats.divisionRate,
                detail = noDataTextOr(stats.divisionTotal) { "${stats.divisionCorrect} / ${stats.divisionTotal} doğru" },
                barColor = Color(0xFF06B6D4)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. En Çok Zorlandığı İşlemler ve Çarpım Tablosu Zorluk Analizi
        SectionCard(title = "🔍 Zorlanılan İşlemler ve Çarpım Tablosu Analizi") {
            Text(
                text = "En Çok Zorlandığı İşlemler:",
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (stats.hardestOperations.isEmpty()) {
                Text(
                    text = "Henüz zorlanılan bir işlem kaydedilmedi. Harika gidiyor! 🌟",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            } else {
                stats.hardestOperations.forEach { (opName, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• $opName", color = Color.White, fontSize = 12.sp)
                        Text(
                            text = "$count kez hata",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Çarpım Tablosunda Zorlanılan Sayılar (0 - 10):",
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (num in 0..10) {
                    val errors = stats.multiplicationStruggleByNumber[num] ?: 0
                    val chipColor = when {
                        errors == 0 -> Color(0xFF065F46)
                        errors in 1..2 -> Color(0xFF92400E)
                        else -> Color(0xFF991B1B)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$num'lar: ${if (errors == 0) "Sorunsuz" else "$errors hata"}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Ebeveyn Kontrolü: Süre Sınırı ve Ses Ayarları
        SectionCard(title = "⚙️ Ebeveyn Ayarları (Süre Sınırı ve Ses)") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Oyun Ses Efektleri ve Müzik",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Doğru/yanlış sesleri ve kutlama melodileri",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = stats.soundEnabled,
                    onCheckedChange = onToggleSound,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rahat Oyun Modu (Süre Sınırı Olmadan)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Orta/Zor/Uzman seviyelerdeki geri sayımı kapatır, stresi önler",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = stats.relaxedUntimedMode,
                    onCheckedChange = onToggleRelaxedMode,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Süreli Seviyeler İçin Ek Süre Ayarı: ${if (stats.customTimeAdditionSeconds >= 0) "+${stats.customTimeAdditionSeconds}" else "${stats.customTimeAdditionSeconds}"} sn",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Standart Süreler: Orta 90 sn • Zor 60 sn • Uzman 45 sn",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-15, 0, 15, 30, 60).forEach { extra ->
                    val isSelected = stats.customTimeAdditionSeconds == extra
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF334155))
                            .clickable { onChangeExtraTime(extra) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = when {
                                extra == 0 -> "Standart (+0 sn)"
                                extra > 0 -> "+$extra sn Ek Süre"
                                else -> "$extra sn Daha Kısa"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onResetDailyScore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Günlük Puanı Şimdi Sıfırla (yeni günde otomatik sıfırlanır)", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.9f))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFFFBBF24),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Ebeveyn panelinde veri olmayan işlem türü "yüzde 100 başarılı" gibi gösterilmez. */
private fun noDataTextOr(total: Int, filledText: () -> String): String =
    if (total <= 0) "Henüz deneme yok" else filledText()

@Composable
private fun SkillRateBar(
    title: String,
    rate: Int,
    detail: String,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "%$rate ($detail)", color = barColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (rate / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = Color(0xFF0F172A)
        )
    }
}
