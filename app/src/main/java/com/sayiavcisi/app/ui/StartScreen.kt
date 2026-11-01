package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.ProgressRules

private enum class ActionStyle {
    Primary,
    Secondary,
    Ghost
}

/**
 * Ana giriş ekranı: tilki maskotu, günlük puan sayacı ve üç ana eylem
 * (maceraya başla, kaldığın yerden devam, ebeveyn paneli) orman dünyası gradyanı üzerinde.
 */
@Composable
fun StartScreen(
    dailyScore: Int,
    dailyCap: Int,
    totalGold: Int,
    totalDiamonds: Int,
    completedLevelIds: Set<String>,
    resumeLevelId: String?,
    timedMode: Boolean,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onParentPanel: () -> Unit
) {
    val palette = GameVisuals.palette("orman")
    val resumeSpec = resumeLevelId?.let { CurriculumRepository.levelById(it) }
    val completedCount = ProgressRules.completedLevelCount(completedLevelIds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameVisuals.backgroundBrush(palette))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(text = "Bugün: $dailyScore/$dailyCap", palette = palette)
                StatPill(text = "🪙 $totalGold", palette = palette)
                StatPill(text = "💎 $totalDiamonds", palette = palette)
            }

            Text(
                text = "Her gün hedefin ${dailyCap} puan — yeni günde puan taze başlar, macera kaldığı yerden sürer.",
                color = palette.surface.copy(alpha = 0.92f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.35f))

            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(126.dp)) {
                    GlowRing(active = true, color = palette.glow, modifier = Modifier.matchParentSize())
                }
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(palette.surface)
                        .border(4.dp, palette.accent, CircleShape)
                        .popIn(100)
                        .pulseWhile(true),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🦊", fontSize = 50.sp)
                }
            }

            Row(modifier = Modifier.popIn(180)) {
                Text(
                    text = "SAYI ",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.surface,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "AVCISI",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.glow,
                    letterSpacing = 2.sp
                )
            }

            Text(
                text = "Gün gün sayıları avla: toplama, çıkarma, çarpma, bölme",
                color = palette.surface.copy(alpha = 0.95f),
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "🗺️ ${CurriculumRepository.dayCount} gün × ${CurriculumRepository.stagesPerDay} bölüm   •   ✅ $completedCount/${CurriculumRepository.allLevels.size} bölüm bitti",
                color = palette.surface.copy(alpha = 0.92f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(0.6f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(
                    label = "MACERAYA BAŞLA",
                    sublabel = null,
                    style = ActionStyle.Primary,
                    palette = palette,
                    modifier = Modifier.popIn(260),
                    onClick = onPlay
                )
                if (resumeLevelId != null) {
                    ActionButton(
                        label = "KALDIĞIN YERDEN DEVAM",
                        sublabel = resumeSpec?.title ?: resumeLevelId,
                        style = ActionStyle.Secondary,
                        palette = palette,
                        modifier = Modifier.popIn(320),
                        onClick = onResume
                    )
                }
                ActionButton(
                    label = "EBEVEYN PANELİ",
                    sublabel = null,
                    style = ActionStyle.Ghost,
                    palette = palette,
                    modifier = Modifier.popIn(380),
                    onClick = onParentPanel
                )
            }

            Text(
                text = if (timedMode) "🕒 Süreli mod açık: zamana karşı oynuyorsun" else "🕒 Süreli mod kapalı: rahat rahat oyna",
                color = palette.surface.copy(alpha = 0.9f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(0.35f))
        }
    }
}

@Composable
private fun StatPill(
    text: String,
    palette: WorldPalette
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(palette.surface.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = palette.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    sublabel: String?,
    style: ActionStyle,
    palette: WorldPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val height = if (style == ActionStyle.Primary) 62.dp else 52.dp
    val contentColor = when (style) {
        ActionStyle.Primary -> Color.White
        ActionStyle.Secondary -> palette.onSurface
        ActionStyle.Ghost -> palette.surface
    }
    val base = modifier
        .fillMaxWidth()
        .height(height)
        .clip(shape)
    val surfaceModifier = when (style) {
        ActionStyle.Primary ->
            base.background(Brush.linearGradient(listOf(palette.accent, palette.accentDeep)))
        ActionStyle.Secondary ->
            base.background(palette.surface.copy(alpha = 0.95f))
            .border(2.dp, palette.accent.copy(alpha = 0.6f), shape)
        ActionStyle.Ghost ->
            base.background(Color.Transparent)
                .border(2.dp, palette.surface.copy(alpha = 0.75f), shape)
    }

    Box(
        modifier = surfaceModifier
            .then(if (style == ActionStyle.Primary) Modifier.pulseWhile(true) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (sublabel != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Text(
                    text = sublabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = label,
                fontSize = if (style == ActionStyle.Primary) 19.sp else 13.sp,
                fontWeight = if (style == ActionStyle.Primary) FontWeight.Black else FontWeight.Bold,
                color = contentColor,
                letterSpacing = if (style == ActionStyle.Primary) 2.sp else 1.sp,
                maxLines = 1
            )
        }
    }
}
