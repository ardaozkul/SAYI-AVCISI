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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.ProgressRules
import com.sayiavcisi.app.model.LevelSpec

private enum class StageChipState {
    Open,
    Completed,
    Locked
}

/**
 * Macera haritası: 5 gün kartı, her kartta gün ilerleme halkası ve 6 aşama çipi.
 * Kilitli çipler soluk gösterilir ve dokunulamaz; süreli mod anahtarı üstte durur.
 */
@Composable
fun AdventureMapScreen(
    completedLevelIds: Set<String>,
    dailyScore: Int,
    dailyCap: Int,
    timedMode: Boolean,
    onToggleTimedMode: (Boolean) -> Unit,
    onLevelClick: (LevelSpec) -> Unit,
    onBack: () -> Unit
) {
    val palette = GameVisuals.palette("orman")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameVisuals.backgroundBrush(palette))
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = palette.surface
                    )
                }
                Text(
                    text = "Macera Haritası",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.surface,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Bugün: $dailyScore/$dailyCap",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.onSurface,
                        maxLines = 1
                    )
                }
            }

            TimedModeCard(
                timedMode = timedMode,
                palette = palette,
                onToggle = onToggleTimedMode
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (day in 1..CurriculumRepository.dayCount) {
                    val dayLevels = CurriculumRepository.levelsForDay(day)
                    DayCard(
                        day = day,
                        levels = dayLevels,
                        dayPalette = GameVisuals.palette(dayLevels.first().worldKey),
                        completedLevelIds = completedLevelIds,
                        modifier = Modifier.popIn((day - 1) * 90),
                        onLevelClick = onLevelClick
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TimedModeCard(
    timedMode: Boolean,
    palette: WorldPalette,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SÜRELİ MOD",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.onSurface,
                letterSpacing = 1.sp
            )
            Text(
                text = "Açarsan her bölümde zamanla yarışırsın: süre bitince tur kapanır.",
                fontSize = 11.sp,
                color = palette.muted,
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = timedMode,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.accentDeep,
                checkedThumbColor = palette.surface
            )
        )
    }
}

@Composable
private fun DayCard(
    day: Int,
    levels: List<LevelSpec>,
    dayPalette: WorldPalette,
    completedLevelIds: Set<String>,
    modifier: Modifier = Modifier,
    onLevelClick: (LevelSpec) -> Unit
) {
    val cardShape = RoundedCornerShape(24.dp)
    val progress = ProgressRules.dayProgress(day, completedLevelIds)
    val doneCount = levels.count { ProgressRules.isLevelCompleted(it, completedLevelIds) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(dayPalette.surface)
            .border(3.dp, dayPalette.accent.copy(alpha = 0.65f), cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(dayPalette.accent, dayPalette.accentDeep))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$day",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = dayPalette.tileText
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CurriculumRepository.dayTitles[day].orEmpty(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = dayPalette.onSurface,
                    maxLines = 1
                )
                Text(
                    text = CurriculumRepository.daySubtitles[day].orEmpty(),
                    fontSize = 11.5.sp,
                    color = dayPalette.muted,
                    lineHeight = 15.sp,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ProgressRing(
                    progress = progress,
                    color = dayPalette.accent,
                    trackColor = dayPalette.muted.copy(alpha = 0.3f),
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 6.dp,
                    label = "$doneCount/${levels.size}"
                )
                Text(
                    text = "~${CurriculumRepository.dayMinutes(day)} dk",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = dayPalette.onSurface
                )
            }
        }

        levels.chunked(3).forEach { rowLevels ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowLevels.forEach { spec ->
                    val state = when {
                        ProgressRules.isLevelCompleted(spec, completedLevelIds) -> StageChipState.Completed
                        !ProgressRules.isLevelUnlocked(spec, completedLevelIds) -> StageChipState.Locked
                        else -> StageChipState.Open
                    }
                    StageChip(
                        spec = spec,
                        state = state,
                        palette = dayPalette,
                        modifier = Modifier
                            .weight(1f)
                            .popIn(((day - 1) * 3 + spec.stageIndex - 1) * 40),
                        onClick = { onLevelClick(spec) }
                    )
                }
                repeat(3 - rowLevels.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StageChip(
    spec: LevelSpec,
    state: StageChipState,
    palette: WorldPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when (state) {
        StageChipState.Completed -> GameVisuals.ok
        StageChipState.Open -> palette.accent
        StageChipState.Locked -> palette.muted.copy(alpha = 0.5f)
    }
    val base = if (state == StageChipState.Locked) {
        modifier
            .height(82.dp)
            .alpha(0.5f)
            .clip(shape)
            .background(palette.tileFaceBottom.copy(alpha = 0.5f))
    } else {
        modifier
            .height(82.dp)
            .clip(shape)
            .background(GameVisuals.tileBrush(palette))
    }

    Box(
        modifier = base
            .border(2.dp, borderColor, shape)
            .semantics { contentDescription = "bolum-${spec.levelId}-${state.name.lowercase()}" }
            .then(if (state == StageChipState.Locked) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${spec.stageIndex}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.muted
                )
                Spacer(modifier = Modifier.weight(1f))
                val marker = when (state) {
                    StageChipState.Completed -> "✓"
                    StageChipState.Locked -> "🔒"
                    StageChipState.Open -> if (spec.timeLimitSeconds != null) "⏱" else ""
                }
                if (marker.isNotEmpty()) {
                    Text(
                        text = marker,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state == StageChipState.Completed) GameVisuals.ok else palette.accentDeep
                    )
                }
            }
            Text(
                text = "${spec.ruleKind.operation.symbol} ${spec.target}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = palette.tileText,
                maxLines = 1
            )
            Text(
                text = "${spec.gridSize}x${spec.gridSize}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.muted
            )
        }
    }
}
