package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.audio.SoundEffectsManager
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GameSession
import com.sayiavcisi.app.engine.ScoreEngine
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec
import kotlinx.coroutines.delay

/**
 * Oyun ekranı: hedef kartı, sayı tahtası, jokerler ve efektler tek ekranda.
 * Amaç her zaman ekranın üstünde yazar; çocuk "ne yapmam gerekiyor" diye tahmin etmez.
 */
@Composable
fun GameScreen(
    spec: LevelSpec,
    session: GameSession,
    dailyScore: Int,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onPlaySecond: () -> Unit
) {
    val palette = GameVisuals.palette(spec.worldKey)
    var showHelp by remember { mutableStateOf(true) }
    var rescueBanner by remember { mutableStateOf(false) }
    val cellsByCoord: Map<Pair<Int, Int>, BlockCell> =
        session.board.cells.associateBy { it.coord }

    // Ses ve efektler
    LaunchedEffect(session.correctTick) {
        if (session.correctTick > 0) SoundEffectsManager.playCorrectSound()
    }
    LaunchedEffect(session.wrongTick) {
        if (session.wrongTick > 0) SoundEffectsManager.playGentleWrongSound()
    }
    LaunchedEffect(session.rescueTick) {
        if (session.rescueTick > 0) {
            SoundEffectsManager.playBonusSound()
            rescueBanner = true
            delay(2400)
            rescueBanner = false
        }
    }
    LaunchedEffect(session.isFinished) {
        if (session.isFinished) SoundEffectsManager.playLevelCompleteFanfare()
    }
    LaunchedEffect(session, spec) {
        while (session.secondsLeft != null && !session.isFinished && !session.isTimeUp) {
            delay(1000)
            session.onTick()
            onPlaySecond()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = GameVisuals.backgroundBrush(palette))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameTopBar(spec = spec, palette = palette, onBack = onBack, onHelp = { showHelp = true })

            ObjectiveCard(spec = spec, session = session, palette = palette)

            ScoreBar(session = session, dailyScore = dailyScore)

            // Tahta
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                NumberBoard(
                    spec = spec,
                    session = session,
                    palette = palette,
                    cellsByCoord = cellsByCoord
                )

                ParticleBurst(
                    trigger = session.correctTick,
                    color = palette.glow,
                    count = 18,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(220.dp)
                )

                if (session.doubleNextActive) {
                    Text(
                        text = "⚡ ÇİFTE HAMLE AKTİF",
                        color = GameVisuals.gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .pulseWhile(true)
                    )
                }

                FloatingScore(
                    text = session.lastResult?.let { if (it.accepted && it.pointsDelta > 0) "+${it.pointsDelta}" else null }
                        ?: "",
                    trigger = session.correctTick,
                    color = GameVisuals.ok,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 140.dp, height = 90.dp)
                )

                if (rescueBanner) {
                    Text(
                        text = "✨ ALTIN KURTARMA ✨",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                color = GameVisuals.gold.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .popIn()
                    )
                }
            }

            FeedbackRibbon(session = session, palette = palette)

            GameControls(
                spec = spec,
                session = session,
                palette = palette,
                onConfirm = { session.submit() },
                onJoker = { type -> session.useJoker(type) },
                onReplay = onReplay
            )
        }

        if (showHelp) {
            HowToPlayOverlay(spec = spec, palette = palette, onGotIt = { showHelp = false })
        }

        if (session.isFinished) {
            LevelCompleteOverlay(
                spec = spec,
                session = session,
                palette = palette,
                onNextLevel = onNextLevel,
                onReplay = onReplay,
                onBack = onBack
            )
        } else if (session.isTimeUp) {
            TimeUpOverlay(spec = spec, palette = palette, onReplay = onReplay, onBack = onBack)
        }
    }
}

@Composable
private fun GameTopBar(
    spec: LevelSpec,
    palette: WorldPalette,
    onBack: () -> Unit,
    onHelp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoundChip(text = "←", palette = palette, onClick = onBack, tag = "haritaya don")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${spec.dayNumber}. Gün • Bölüm ${spec.stageIndex}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = spec.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
        RoundChip(text = "?", palette = palette, onClick = onHelp, tag = "nasil-oynanir")
    }
}

@Composable
private fun RoundChip(
    text: String,
    palette: WorldPalette,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = palette.surface.copy(alpha = 0.9f), shape = CircleShape)
            .border(width = 1.5.dp, color = palette.accentDeep.copy(alpha = 0.4f), shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = palette.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

/** Sayı ızgarası ve seçim çizgileri. */
@Composable
private fun NumberBoard(
    spec: LevelSpec,
    session: GameSession,
    palette: WorldPalette,
    cellsByCoord: Map<Pair<Int, Int>, BlockCell>
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Black.copy(alpha = 0.16f), shape = RoundedCornerShape(22.dp))
            .padding(10.dp)
    ) {
        val gap = if (spec.gridSize >= 8) 3.dp else 5.dp
        val pitch = (maxWidth - gap * (spec.gridSize - 1)) / spec.gridSize
        val cellSize: Dp = pitch.coerceAtLeast(24.dp)
        val textSize: TextUnit = when {
            spec.gridSize >= 10 -> 15.sp
            spec.gridSize >= 8 -> 18.sp
            spec.gridSize >= 6 -> 21.sp
            else -> 25.sp
        }
        val hintedCoords = session.hinted.toSet()

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (r in 0 until spec.gridSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until spec.gridSize) {
                        val coord = r to c
                        NumberTile(
                            cell = cellsByCoord[coord],
                            isSelected = session.selected.contains(coord),
                            isHinted = hintedCoords.contains(coord),
                            palette = palette,
                            textSize = textSize,
                            appearDelayMillis = (r + c) * 22,
                            modifier = Modifier.size(cellSize),
                            onClick = { session.tapCell(it) }
                        )
                    }
                }
            }
        }

        // Seçilen taşların bağlantı çizgisi: seçimin aynı hatta olduğunu çocuk gözle görür.
        if (session.selected.size >= 2) {
            val cellPx = with(LocalDensity.current) { cellSize.toPx() }
            val gapPx = with(LocalDensity.current) { gap.toPx() }
            val step = cellPx + gapPx
            val centers = session.selected.map { coord ->
                Offset(x = coord.second * step + cellPx / 2f, y = coord.first * step + cellPx / 2f)
            }
            for (i in 1 until centers.size) {
                ConnectionLine(
                    from = centers[i - 1],
                    to = centers[i],
                    color = GameVisuals.ok,
                    strokeWidth = if (spec.gridSize >= 8) 4.dp else 6.dp,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Composable
private fun FeedbackRibbon(
    session: GameSession,
    palette: WorldPalette
) {
    val accepted = session.lastResult?.accepted ?: true
    val background = when {
        session.lastResult == null -> palette.surface.copy(alpha = 0.9f)
        accepted -> GameVisuals.ok.copy(alpha = 0.9f)
        else -> GameVisuals.danger.copy(alpha = 0.9f)
    }
    Text(
        text = session.message,
        color = if (session.lastResult == null) palette.onSurface else Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .shakeOnTrigger(session.wrongTick)
            .background(color = background, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun GameControls(
    spec: LevelSpec,
    session: GameSession,
    palette: WorldPalette,
    onConfirm: () -> Unit,
    onJoker: (JokerType) -> Unit,
    onReplay: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (session.needsConfirm) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(
                        brush = GameVisuals.tileBrush(palette),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(width = 2.dp, color = GameVisuals.ok, shape = RoundedCornerShape(14.dp))
                    .clickable(enabled = session.selected.size >= 2, onClick = onConfirm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (session.selected.size >= 2) {
                        "EŞLEŞTİR ✓ (${session.selected.size} taş seçili)"
                    } else {
                        "En az 2 taş seç"
                    },
                    color = palette.tileText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            JokerType.entries.forEach { type ->
                Box(modifier = Modifier.weight(1f)) {
                    JokerButton(
                        type = type,
                        remaining = session.jokers[type] ?: 0,
                        palette = palette,
                        onClick = { onJoker(type) }
                    )
                }
            }
            Box(modifier = Modifier.weight(0.45f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = palette.surface.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = palette.accentDeep.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(onClick = onReplay)
                        .semantics { contentDescription = "BÖLÜMÜ BAŞTAN OYNA" }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↻",
                        color = palette.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HowToPlayOverlay(
    spec: LevelSpec,
    palette: WorldPalette,
    onGotIt: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onGotIt),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(24.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("NASIL OYNANIR?", color = palette.accentDeep, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(
                text = "🎯 HEDEF ${spec.target}: ${CurriculumRepository.objectiveText(spec)}",
                color = palette.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )
            Text(
                text = CurriculumRepository.howToPlayText(spec),
                color = palette.onSurface,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Text(
                text = "Örnek: ${exampleText(spec)}",
                color = palette.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Text(
                text = "Jokerler puanından götürür: 🔍 İpucu -5, 🔄 Yeniden Düzenle -10, ⚡ Çifte Hamle -5." +
                    " Takılırsan ALTIN KURTARMA bedava imdadına gelir.",
                color = palette.onSurface,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(color = GameVisuals.ok, shape = RoundedCornerShape(14.dp))
                    .clickable(onClick = onGotIt),
                contentAlignment = Alignment.Center
            ) {
                Text("ANADIM 👍", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun LevelCompleteOverlay(
    spec: LevelSpec,
    session: GameSession,
    palette: WorldPalette,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    val flawless = session.mistakes == 0 && !session.isRescueUsed
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.58f)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiRain(active = true, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(26.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BÖLÜM BİTTİ 🎉", color = palette.accentDeep, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                text = if (flawless) "Hatasız avladın! Süper iş." else "${session.correctCount} doğru eşleşme, ${session.mistakes} deneme.",
                color = palette.onSurface,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Text(
                text = "${spec.dayNumber}. Gün • ${spec.stageIndex}. Bölüm temizlendi",
                color = palette.muted,
                fontSize = 12.sp,
                maxLines = 1
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RewardPill("⭐", "${session.levelScore} puan", GameVisuals.gold)
                RewardPill("🪙", "+${session.rewardGold} altın", GameVisuals.gold)
                if (session.rewardDiamonds > 0) {
                    RewardPill("💎", "+${session.rewardDiamonds} elmas", GameVisuals.gem)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            PrimaryButton(text = "SONRAKİ BÖLÜM →", onClick = onNextLevel)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(text = "TEKRAR OYNA", onClick = onReplay)
                GhostButton(text = "HARİTA", onClick = onBack)
            }
        }
    }
}

@Composable
private fun TimeUpOverlay(
    spec: LevelSpec,
    palette: WorldPalette,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.58f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(26.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SÜRE DOLDU ⏱", color = GameVisuals.danger, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                text = "${spec.title} bölümünde süre bitti. Kaybettiğin bir puan yok, baştan deneyebilirsin.",
                color = palette.onSurface,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            PrimaryButton(text = "TEKRAR DENE ↻", onClick = onReplay)
            GhostButton(text = "HARİTAYA DÖN", onClick = onBack)
        }
    }
}

@Composable
private fun RewardPill(icon: String, label: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(icon, fontSize = 14.sp)
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = GameVisuals.ok, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .background(color = Color.Black.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Bölüm hedefine göre, negatif olmayan ve gerçekten geçerli bir örnek cümle. */
private fun exampleText(spec: LevelSpec): String {
    val t = spec.target
    return when (spec.ruleKind) {
        com.sayiavcisi.app.model.RuleKind.SUM ->
            if (spec.groupSizes.contains(3) && t >= 6) "1 + 2 + ${t - 3} = $t"
            else "${minOf(t, 2)} + ${t - minOf(t, 2)} = $t"

        com.sayiavcisi.app.model.RuleKind.DIFF -> "${t + 3} - 3 = $t"
        com.sayiavcisi.app.model.RuleKind.PRODUCT -> "${productDivisor(t)} × ${t / productDivisor(t)} = $t"
        com.sayiavcisi.app.model.RuleKind.QUOTIENT -> "${t * 3} ÷ 3 = $t"
    }
}

private fun productDivisor(target: Int): Int =
    (2..9).firstOrNull { target % it == 0 && target / it <= 12 } ?: 2
