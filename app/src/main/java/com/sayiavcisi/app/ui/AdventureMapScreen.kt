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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
 */
@Composable
fun AdventureMapScreen(
    completedLevelIds: Set<String>,
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "‹ Ana sayfa",
                    color = palette.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(palette.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE8DCC8), RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
                Text(
                    text = "Bölümler",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
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
                        onLevelClick = onLevelClick
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    day: Int,
    levels: List<LevelSpec>,
    completedLevelIds: Set<String>,
    onLevelClick: (LevelSpec) -> Unit
) {
    val accent = GameVisuals.palette(levels.first().worldKey)
    val done = levels.count { completedLevelIds.contains(it.levelId) }
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.surface, shape)
            .border(1.dp, Color(0xFFEDE2CE), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.verticalGradient(listOf(accent.accent, accent.accentDeep)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = CurriculumRepository.daySymbols[day] ?: "+",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CurriculumRepository.dayTitles[day].orEmpty(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = accent.onSurface
                )
                Text(
                    text = CurriculumRepository.daySubtitles[day].orEmpty(),
                    fontSize = 11.sp,
                    color = accent.muted,
                    lineHeight = 15.sp
                )
            }
            Text(
                text = "$done/${levels.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accent.accentDeep
            )
        }

        levels.chunked(6).forEach { rowLevels ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowLevels.forEach { spec ->
                    LevelChip(
                        spec = spec,
                        completed = completedLevelIds.contains(spec.levelId),
                        accent = accent,
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .height(52.dp)
            .background(
                color = if (completed) GameVisuals.ok.copy(alpha = 0.14f) else Color(0xFFFFFBF2),
                shape = shape
            )
            .border(
                width = if (completed) 2.dp else 1.dp,
                color = if (completed) GameVisuals.ok else Color(0xFFEDE2CE),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = spec.target.toString(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = if (completed) Color(0xFF15803D) else accent.onSurface,
            maxLines = 1
        )
        Text(
            text = if (completed) "✓" else (CurriculumRepository.daySymbols[spec.dayNumber] ?: ""),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (completed) GameVisuals.ok else accent.muted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
