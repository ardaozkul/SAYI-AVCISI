package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GameSession
import com.sayiavcisi.app.engine.ScoreEngine
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec

/**
 * Sayı bloğu: pastel yüzey + koyu rakam; seçilince büyür ve parıltı halkası alır.
 * Temizlenen blok soluk bir iz bırakır, çocuk taşın buradan kalktığını görür.
 */
@Composable
fun NumberTile(
    cell: BlockCell?,
    isSelected: Boolean,
    isHinted: Boolean,
    palette: WorldPalette,
    modifier: Modifier = Modifier,
    appearDelayMillis: Int = 0,
    textSize: TextUnit = 22.sp,
    onClick: (Pair<Int, Int>) -> Unit = {}
) {
    val shape = RoundedCornerShape(percent = 22)
    val activeCell = cell?.takeIf { !it.isCleared }
    val isActive = activeCell != null
    Box(
        modifier = modifier
            .popIn(delayMillis = appearDelayMillis)
            .then(
                if (isActive) {
                    Modifier
                        .background(brush = GameVisuals.tileBrush(palette), shape = shape)
                        .border(
                            width = if (isSelected) 3.dp else 1.5.dp,
                            color = if (isSelected) GameVisuals.ok else Color.Black.copy(alpha = 0.16f),
                            shape = shape
                        )
                        .scale(if (isSelected) 1.08f else 1f)
                        .clickable { cell?.let { onClick(it.coord) } }
                } else {
                    Modifier
                        .background(color = Color.White.copy(alpha = 0.10f), shape = shape)
                        .border(width = 1.dp, color = Color.White.copy(alpha = 0.18f), shape = shape)
                        .alpha(0.55f)
                }
            )
            .pulseWhile(isHinted && !isSelected)
            .semantics {
                contentDescription = if (activeCell != null) {
                    "Sayı ${activeCell.value}, satır ${activeCell.row}, sütun ${activeCell.col}" +
                        if (isSelected) ", seçili" else ""
                } else {
                    "Temizlendi, satır ${cell?.row ?: 0}, sütun ${cell?.col ?: 0}"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (activeCell != null) {
            Text(
                text = activeCell.value.toString(),
                color = palette.tileText,
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        if (isActive && (isSelected || isHinted)) {
            GlowRing(
                active = true,
                color = if (isSelected) GameVisuals.ok else palette.glow,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Joker düğmesi: kalan hak ve puan maliyeti birlikte yazılı. */
@Composable
fun JokerButton(
    type: JokerType,
    remaining: Int,
    palette: WorldPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val enabled = remaining > 0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = palette.surface.copy(alpha = if (enabled) 0.94f else 0.45f),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (enabled) palette.accentDeep.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.6f))
            .semantics { contentDescription = "${type.title}, $remaining hak, eksi ${type.cost} puan" }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = type.emoji, fontSize = 14.sp)
            Text(
                text = type.shortTitle,
                color = palette.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (enabled) "$remaining hak · −${type.cost}p" else "hak yok",
            color = if (enabled) palette.accentDeep else palette.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Hedef kartı: bölümün amacının açıkça yazıldığı yer. */
@Composable
fun ObjectiveCard(
    spec: LevelSpec,
    session: GameSession,
    palette: WorldPalette,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = palette.surface.copy(alpha = 0.95f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🎯 HEDEF ${spec.target}",
                color = palette.accentDeep,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            ProgressRing(
                progress = session.clearedFraction,
                color = GameVisuals.ok,
                trackColor = palette.muted.copy(alpha = 0.3f),
                strokeWidth = 5.dp,
                label = "%${(session.clearedFraction * 100).toInt()}",
                modifier = Modifier.size(42.dp)
            )
        }
        Text(
            text = CurriculumRepository.objectiveText(spec),
            color = palette.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp
        )
        Text(
            text = buildString {
                append(spec.operation.symbol)
                append(" ")
                append(spec.operation.title)
                append(" • ")
                append(spec.gridSize)
                append("x")
                append(spec.gridSize)
                append(" • ")
                append(spec.ruleMode.title)
                if (spec.focusZero) append(" • sıfır da oyunda")
            },
            color = palette.muted,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

/** Üst bilgi çubuğu: bölüm puanı, seri, günlük puan tavanı ve süre gerginliği. */
@Composable
fun ScoreBar(
    session: GameSession,
    dailyScore: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color.Black.copy(alpha = 0.22f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BarStat(label = "BÖLÜM PUANI", value = session.levelScore.toString(), color = GameVisuals.gold)
        // Yeşil seri rengi koyu yeşil zemine karışıyordu: sıfırken beyaz, seri yakalanınca altın.
        BarStat(
            label = "SERİ",
            value = "x${session.streak}",
            color = if (session.streak >= 2) GameVisuals.gold else Color.White.copy(alpha = 0.9f)
        )
        BarStat(
            label = "BUGÜN",
            value = "$dailyScore/${ScoreEngine.DAILY_CAP}",
            color = Color.White
        )
        Spacer(modifier = Modifier.weight(1f))
        val seconds = session.secondsLeft
        when {
            seconds != null -> Text(
                text = "⏱ ${clockText(seconds)}",
                color = if (seconds <= 20) GameVisuals.danger else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = if (seconds <= 20) Modifier.pulseWhile(true) else Modifier
            )

            else -> Text(
                text = "⏱ rahat mod",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BarStat(    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

/** Sayacı çocuğun okuyabileceği kısa biçime çevirir: 90 sn ve üstü "2:15". */
private fun clockText(seconds: Int): String {
    if (seconds < 90) return "$seconds sn"
    return "${seconds / 60}:${"%02d".format(seconds % 60)}"
}
