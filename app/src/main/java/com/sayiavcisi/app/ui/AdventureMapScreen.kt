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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.model.LevelSpec

/**
 * Bölüm listesi: dört sade kart (Toplama, Çıkarma, Çarpma Tablosu, Bölme).
 * Kilit yoktur; her hedef açıktır ve tamamlananlar yeşil ✓ ile işaretlenir.
 * Uzun finaller küçük "UZUN" etiketiyle ayrılır.
 */
@Composable
fun AdventureMapScreen(
    completedLevelIds: Set<String>,
    boardColumns: Int,
    endlessBestScore: Int,
    onLevelClick: (LevelSpec) -> Unit,
    onBack: () -> Unit
) {
    val palette = GameVisuals.palette("toplama")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameVisuals.backgroundBrush(palette))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "‹ Ana sayfa",
                    color = palette.onSurface,
                    fontFamily = AppFonts.Body,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(vertical = 8.dp)
                )
                Text(
                    text = "Bölümler",
                    fontFamily = AppFonts.Display,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (day in 1..CurriculumRepository.dayCount) {
                    SectionCard(
                        day = day,
                        levels = CurriculumRepository.levelsForDay(day),
                        completedLevelIds = completedLevelIds,
                        boardColumns = boardColumns,
                        endlessBestScore = endlessBestScore,
                        onLevelClick = onLevelClick
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    day: Int,
    levels: List<LevelSpec>,
    completedLevelIds: Set<String>,
    boardColumns: Int,
    endlessBestScore: Int,
    onLevelClick: (LevelSpec) -> Unit
) {
    val accent = GameVisuals.palette(levels.first().worldKey)
    val isEndless = day == 5
    val done = levels.count { completedLevelIds.contains(it.levelId) }
    val shape = RoundedCornerShape(20.dp)
    val subtitle = if (isEndless) {
        "Anlık görevler · ${boardColumns}×100 · ayrı yeşil puan kaydı"
    } else {
        CurriculumRepository.daySubtitles[day].orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.surface.copy(alpha = 0.82f), shape)
            .border(1.dp, GameVisuals.line.copy(alpha = 0.9f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.accentDeep, RoundedCornerShape(12.dp))
                    .border(1.dp, accent.accentDeep, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = CurriculumRepository.daySymbols[day] ?: "+",
                    color = Color(0xFFFFFCF4),
                    fontFamily = AppFonts.Display,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CurriculumRepository.dayTitles[day].orEmpty(),
                    fontFamily = AppFonts.Display,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent.onSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = AppFonts.Body,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent.muted,
                    lineHeight = 13.sp
                )
            }
            Text(
                text = if (isEndless) "● $endlessBestScore" else "$done/${levels.size}",
                fontFamily = AppFonts.Body,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEndless) GameVisuals.greenPoint else accent.accentDeep
            )
        }

        levels.chunked(6).forEach { rowLevels ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowLevels.forEach { spec ->
                    LevelChip(
                        spec = spec,
                        completed = completedLevelIds.contains(spec.levelId),
                        accent = accent,
                        boardColumns = boardColumns,
                        modifier = Modifier.weight(1f),
                        onClick = { onLevelClick(spec) }
                    )
                }
                repeat(6 - rowLevels.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LevelChip(
    spec: LevelSpec,
    completed: Boolean,
    accent: WorldPalette,
    boardColumns: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    val subText = when {
        spec.endless -> "${boardColumns}×100"
        completed -> "✓"
        spec.isLong -> "${boardColumns}×${spec.rows}"
        spec.isEasyBoard -> "${boardColumns}×10"
        else -> CurriculumRepository.daySymbols[spec.dayNumber] ?: ""
    }
    val title = if (spec.endless) "∞" else spec.target.toString()
    Column(
        modifier = modifier
            .height(50.dp)
            .background(
                color = if (spec.endless) GameVisuals.greenPoint.copy(alpha = 0.09f)
                else if (completed) GameVisuals.ok.copy(alpha = 0.10f)
                else Color(0xFFFFFCF4),
                shape = shape
            )
            .border(
                width = if (completed || spec.endless) 1.5.dp else 1.dp,
                color = when {
                    spec.endless -> GameVisuals.greenPoint.copy(alpha = 0.65f)
                    completed -> GameVisuals.ok.copy(alpha = 0.7f)
                    else -> GameVisuals.line
                },
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontFamily = AppFonts.Display,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = when {
                spec.endless -> GameVisuals.greenPoint
                completed -> GameVisuals.ok
                else -> accent.onSurface
            },
            maxLines = 1
        )
        Text(
            text = subText,
            fontFamily = AppFonts.Body,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                spec.endless -> GameVisuals.greenPoint
                completed -> GameVisuals.ok
                spec.isLong -> GameVisuals.gold
                else -> accent.muted
            },
            letterSpacing = if (spec.isLong || spec.endless) 0.8.sp else 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
