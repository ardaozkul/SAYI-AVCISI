package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.data.ParentStatsSnapshot
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.ProgressRules
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Ebeveyn Paneli: çocuğun cihaz içinde biriken gerçek ilerleme ve başarı verilerini gösteren,
 * tüm değerleri [ParentStatsSnapshot] üzerinden okuyan salt-okunur rapor + ayar ekranı.
 *
 * Bölümler: özet kartları, gün gün ilerleme, işlem bazında başarı, en çok zorlanılan işlemler,
 * çarpım tablosu dökümü, ayarlar (ses / süre modu / süre eklentisi) ve onaylı sıfırlama alanı.
 * Veriler çevrimdışıdır; hiçbir şey cihazdan dışarı gitmez.
 */
@Composable
fun ParentPanelScreen(
    snapshot: ParentStatsSnapshot,
    onToggleSound: (Boolean) -> Unit,
    onToggleTimedMode: (Boolean) -> Unit,
    onTimeExtension: (Int) -> Unit,
    onBoardColumns: (Int) -> Unit,
    onResetDailyScore: () -> Unit,
    onResetAllProgress: () -> Unit,
    onBack: () -> Unit
) {
    val p = GameVisuals.palette("lab")
    var showDailyResetDialog by remember { mutableStateOf(false) }
    var showAllResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameVisuals.backgroundBrush(p))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopBar(
                accent = p.accentDeep,
                onSurface = p.onSurface,
                muted = p.muted,
                onBack = onBack
            )

            SummaryRow(snapshot = snapshot)

            SectionCard(title = "Sonsuz Av · yeşil puan kaydı", p = p, delayMillis = 60) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryTile(
                            value = "${snapshot.endlessBestScore}",
                            caption = "en iyi yeşil puan",
                            note = "ayrı kayıt",
                            modifier = Modifier.weight(1f),
                            valueColor = GameVisuals.greenPoint
                        )
                        SummaryTile(
                            value = "${snapshot.endlessTotalScore}",
                            caption = "toplam yeşil puan",
                            note = "${snapshot.endlessPlays} oyun",
                            modifier = Modifier.weight(1f),
                            valueColor = GameVisuals.greenPoint
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryTile(
                            value = "${snapshot.endlessMissions}",
                            caption = "tamamlanan görev",
                            note = "toplam",
                            modifier = Modifier.weight(1f),
                            valueColor = GameVisuals.greenPoint
                        )
                        SummaryTile(
                            value = "${snapshot.endlessBestStreak}",
                            caption = "en iyi seri",
                            note = "üst üste görev",
                            modifier = Modifier.weight(1f),
                            valueColor = GameVisuals.greenPoint
                        )
                    }
                    Text(
                        text = "Yeşil puan yalnızca Sonsuz Av'dan gelir; klasik bölümlerin sarı puanıyla karışmaz.",
                        color = p.muted,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            SectionCard(title = "Gün gün ilerleme", p = p, delayMillis = 80) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (day in 1..CurriculumRepository.dayCount) {
                        DayProgressRow(day = day, snapshot = snapshot, p = p)
                    }
                }
            }

            SectionCard(title = "İşlem bazında başarı", p = p, delayMillis = 120) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OperationRow(
                        label = "Toplama",
                        rate = snapshot.additionRate,
                        correct = snapshot.additionCorrect,
                        total = snapshot.additionTotal,
                        color = GameVisuals.ok,
                        p = p
                    )
                    OperationRow(
                        label = "Çıkarma",
                        rate = snapshot.subtractionRate,
                        correct = snapshot.subtractionCorrect,
                        total = snapshot.subtractionTotal,
                        color = GameVisuals.gem,
                        p = p
                    )
                    OperationRow(
                        label = "Çarpma",
                        rate = snapshot.multiplicationRate,
                        correct = snapshot.multiplicationCorrect,
                        total = snapshot.multiplicationTotal,
                        color = GameVisuals.gold,
                        p = p
                    )
                    OperationRow(
                        label = "Bölme",
                        rate = snapshot.divisionRate,
                        correct = snapshot.divisionCorrect,
                        total = snapshot.divisionTotal,
                        color = p.accentDeep,
                        p = p
                    )
                }
            }

            SectionCard(title = "En çok zorlanılan işlemler", p = p, delayMillis = 160) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zorlanılan işlemler tekrar için işaretlenir, puandan düşülmez.",
                        color = p.muted,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (snapshot.hardestOperations.isEmpty()) {
                        Text(
                            text = "Şimdilik zorlanılan işlem görünmüyor 👏",
                            color = p.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        snapshot.hardestOperations.forEach { (label, count) ->
                            HardOperationRow(label = label, count = count, p = p)
                        }
                    }
                }
            }

            SectionCard(title = "Çarpım tablosu dökümü", p = p, delayMillis = 200) {
                MultiplicationStruggleGrid(map = snapshot.multiplicationStruggleByNumber, p = p)
            }

            SectionCard(title = "Ayarlar", p = p, delayMillis = 240) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingSwitchRow(
                        title = "Ses efektleri",
                        subtitle = "Doğru/yanlış ve ödül sesleri",
                        checked = snapshot.soundEnabled,
                        onCheckedChange = { onToggleSound(it) },
                        p = p
                    )
                    SettingSwitchRow(
                        title = "Süreye karşı mod",
                        subtitle = "Kapalıyken rahat süreler, çocuk acele ettirilmez",
                        checked = !snapshot.relaxedUntimedMode,
                        onCheckedChange = { onToggleTimedMode(it) },
                        p = p
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Süre eklentisi",
                            color = p.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Süre yalnızca rehberdir; geç veya yanlış cevap puandan asla düşmez.",
                            color = p.muted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        TimeExtensionChips(
                            currentSeconds = snapshot.customTimeAdditionSeconds,
                            onSelect = { onTimeExtension(it) },
                            p = p
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Tablet / geniş ekran tahtası",
                            color = p.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "10–11 inç tablette tahtayı yatay büyütür. Otomatik: telefonda 6, geniş ekranda 9–15 sütun. Sonsuz Av bu genişlikte 100 satır olur.",
                            color = p.muted,
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        BoardColumnsChips(
                            current = snapshot.boardColumns,
                            onSelect = onBoardColumns,
                            p = p
                        )
                    }
                }
            }

            DangerZone(
                p = p,
                onRequestDailyReset = { showDailyResetDialog = true },
                onRequestAllReset = { showAllResetDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDailyResetDialog) {
        ConfirmDialog(
            title = "Bugünlük puanı sıfırla",
            message = "Bugünkü puan sayacı sıfırlanacak. Yarın 100 puan hakkı yeniden başlar.",
            confirmLabel = "Sıfırla",
            confirmColor = GameVisuals.danger,
            p = p,
            onConfirm = {
                showDailyResetDialog = false
                onResetDailyScore()
            },
            onDismiss = { showDailyResetDialog = false }
        )
    }

    if (showAllResetDialog) {
        ConfirmDialog(
            title = "Tüm ilerlemeyi sıfırla",
            message = "Tamamlanan bölümler, puanlar ve tüm istatistikler silinir. Bu geri alınamaz.",
            confirmLabel = "Her şeyi sil",
            confirmColor = GameVisuals.danger,
            p = p,
            onConfirm = {
                showAllResetDialog = false
                onResetAllProgress()
            },
            onDismiss = { showAllResetDialog = false }
        )
    }
}

@Composable
private fun TopBar(
    accent: Color,
    onSurface: Color,
    muted: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(accent, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ebeveyn Paneli",
                color = onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Her şey bu cihazda kalır; hiçbir veri internete gitmez.",
                color = muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryRow(snapshot: ParentStatsSnapshot) {
    val minutes = (snapshot.dailyPlayTimeSeconds / 60L).toInt()
    // Dört kart tek satıra sığmıyor (metin kısalıyordu): 2x2 ızgara.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile(
                value = "${snapshot.dailyScore}/${ParentStatsSnapshot.DAILY_CAP}",
                caption = "bugünkü sarı puan",
                note = "günlük tavan ${ParentStatsSnapshot.DAILY_CAP}",
                modifier = Modifier
                    .weight(1f)
                    .popIn(0)
            )
            SummaryTile(
                value = "${snapshot.totalOperations}",
                caption = "toplam işlem",
                note = "doğru ${snapshot.totalCorrect} · yanlış ${snapshot.totalWrong}",
                modifier = Modifier
                    .weight(1f)
                    .popIn(60)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile(
                value = "${successPercent(snapshot)}%",
                caption = "isabet",
                note = "genel doğruluk",
                modifier = Modifier
                    .weight(1f)
                    .popIn(120)
            )
            SummaryTile(
                value = "${minutes} dk",
                caption = "günlük süre",
                note = "ort. tepki ${formatSeconds(snapshot.averageResponseTimeSeconds)}",
                modifier = Modifier
                    .weight(1f)
                    .popIn(180)
            )
        }
    }
}

@Composable
private fun SummaryTile(
    value: String,
    caption: String,
    note: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val p = GameVisuals.palette("lab")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = p.surface,
        contentColor = p.onSurface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = valueColor ?: p.accentDeep,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = caption,
                color = p.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = note,
                color = p.muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayProgressRow(day: Int, snapshot: ParentStatsSnapshot, p: WorldPalette) {
    val levels = CurriculumRepository.levelsForDay(day)
    val done = levels.count { snapshot.completedLevelIds.contains(it.levelId) }
    val total = levels.size
    val progress = ProgressRules.dayProgress(day, snapshot.completedLevelIds)
    val completed = CurriculumRepository.isDayCompleted(day, snapshot.completedLevelIds)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = p.surface,
        contentColor = p.onSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$day. Gün · ${CurriculumRepository.dayTitles[day] ?: ""}",
                    color = p.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$done/$total bölüm · ${CurriculumRepository.dayMinutes(day)} dk",
                    color = p.muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (completed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(GameVisuals.ok, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Tamamlandı ✓",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            ProgressRing(
                progress = progress,
                color = if (completed) GameVisuals.ok else p.accent,
                trackColor = p.tileFaceBottom,
                modifier = Modifier.size(48.dp),
                strokeWidth = 6.dp,
                label = "${(progress * 100f).roundToInt()}%"
            )
        }
    }
}

@Composable
private fun OperationRow(
    label: String,
    rate: Int,
    correct: Int,
    total: Int,
    color: Color,
    p: WorldPalette
) {
    val hasData = snapshotHasData(total)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = p.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (hasData) "$correct/$total · %$rate" else "henüz veri yok",
                color = if (hasData) p.onSurface else p.muted,
                fontSize = 12.sp,
                fontWeight = if (hasData) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BarTrack(fraction = if (hasData) rate / 100f else 0f, color = color, p = p)
    }
}

@Composable
private fun BarTrack(fraction: Float, color: Color, p: WorldPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(p.tileFaceBottom, RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color, RoundedCornerShape(50))
        )
    }
}

@Composable
private fun HardOperationRow(label: String, count: Int, p: WorldPalette) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = p.surface.copy(alpha = 0.6f),
        contentColor = p.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = p.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(GameVisuals.danger, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "×$count",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MultiplicationStruggleGrid(map: Map<Int, Int>, p: WorldPalette) {
    val maxFreq = map.values.maxOrNull() ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (n in 0..10) {
                val freq = map[n] ?: 0
                val t = if (maxFreq <= 0) 0f else freq.toFloat() / maxFreq.toFloat()
                val cellColor = if (freq <= 0) p.tileFaceBottom else lerpColor(GameVisuals.ok, GameVisuals.danger, t)
                val textColor = if (freq <= 0) p.onSurface else if (t > 0.55f) Color.White else p.onSurface
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(cellColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$n",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LegendDot(color = p.tileFaceBottom)
            Text("bilgi yok", color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LegendDot(color = GameVisuals.ok)
            Text("az hata", color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LegendDot(color = GameVisuals.gold)
            Text("orta", color = p.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LegendDot(color = GameVisuals.danger)
            Text(
                "çok hata",
                color = p.muted,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f, false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    p: WorldPalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = p.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = p.muted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TimeExtensionChips(currentSeconds: Int, onSelect: (Int) -> Unit, p: WorldPalette) {
    val options = listOf(-15, 0, 30, 60)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { seconds ->
            val selected = seconds == currentSeconds
            val label = when (seconds) {
                0 -> "Normal"
                -15 -> "−15 sn"
                else -> "+$seconds sn"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp)
                    .background(if (selected) p.accentDeep else p.surface, RoundedCornerShape(50))
                    .border(
                        width = 1.dp,
                        color = if (selected) p.accentDeep else p.muted.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onSelect(seconds) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else p.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BoardColumnsChips(current: Int, onSelect: (Int) -> Unit, p: WorldPalette) {
    val options = listOf(0, 6, 9, 12, 15)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { columns ->
            val selected = columns == current
            val label = if (columns == 0) "Oto" else "$columns"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) p.accentDeep else p.surface, RoundedCornerShape(50))
                    .border(
                        width = 1.dp,
                        color = if (selected) p.accentDeep else p.muted.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onSelect(columns) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else p.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DangerZone(
    p: WorldPalette,
    onRequestDailyReset: () -> Unit,
    onRequestAllReset: () -> Unit
) {
    SectionCard(title = "Sıfırlama", p = p, delayMillis = 280) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Bu işlemler kaydedilmiş ilerlemeyi siler; onay istenir.",
                color = p.muted,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onRequestDailyReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameVisuals.gold,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Bugünlük puanı sıfırla",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onRequestAllReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameVisuals.danger,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Tüm ilerlemeyi sıfırla",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    p: WorldPalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        titleContentColor = p.onSurface,
        textContentColor = p.muted,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = confirmColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Vazgeç", color = p.muted)
            }
        }
    )
}

@Composable
private fun SectionCard(
    title: String,
    p: WorldPalette,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .popIn(delayMillis),
        shape = RoundedCornerShape(20.dp),
        color = p.surface,
        contentColor = p.onSurface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = p.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            content()
        }
    }
}

private fun successPercent(snapshot: ParentStatsSnapshot): Int =
    snapshot.successRatePercent(snapshot.totalCorrect, snapshot.totalOperations)

private fun snapshotHasData(total: Int): Boolean = total > 0

private fun formatSeconds(seconds: Float): String =
    String.format(Locale.US, "%.1f", seconds).replace('.', ',') + " sn"

private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = from.alpha + (to.alpha - from.alpha) * t
    )
}
