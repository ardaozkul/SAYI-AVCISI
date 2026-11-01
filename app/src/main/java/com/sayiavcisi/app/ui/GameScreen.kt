package com.sayiavcisi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.audio.SoundEffectsManager
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GameSession
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.LevelSpec
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.min

/**
 * Oyun ekranı artık tek işe odaklanır: tahta + üstte bölüm adı.
 *
 * - Üstte yalnızca "7 TOPLAMA" benzeri bölüm adı, ince bir ilerleme çizgisi ve yıldız puanı var.
 * - Hedef kartı, ipucu kartı, joker kutuları ve alt bilgi şeritleri kaldırıldı.
 * - Çocuk parmağını taşın üzerinde tutup komşu taşlara sürükler; bırakınca hamle kendiliğinden
 *   kontrol edilir. Doğruysa taşlar yeşil parlar, yanlışsa kırmızı olup sarsılır.
 * - 3 veya 4 taşlı komboda kocaman "KOMBO" yazısı, yıldızlar ve uçan puan efekti çıkar.
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
    val cellsByCoord: Map<Pair<Int, Int>, BlockCell> =
        session.board.cells.associateBy { it.coord }

    var flashRight by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var flashWrong by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var pointerDown by remember { mutableStateOf(false) }
    var starBanner by remember { mutableStateOf(false) }
    var comboBanner by remember { mutableStateOf(false) }

    LaunchedEffect(session.correctTick) {
        if (session.correctTick > 0) {
            SoundEffectsManager.playCorrectSound()
            flashRight = session.lastMatchedCoords.toSet()
            delay(700)
            flashRight = emptySet()
        }
    }
    LaunchedEffect(session.wrongTick) {
        if (session.wrongTick > 0) {
            SoundEffectsManager.playGentleWrongSound()
            flashWrong = session.lastWrongCoords.toSet()
            delay(900)
            flashWrong = emptySet()
        }
    }
    LaunchedEffect(session.comboTick) {
        if (session.comboTick > 0) {
            SoundEffectsManager.playBonusSound()
            comboBanner = true
            delay(1800)
            comboBanner = false
        }
    }
    LaunchedEffect(session.rescueTick) {
        if (session.rescueTick > 0) {
            SoundEffectsManager.playBonusSound()
            starBanner = true
            delay(2200)
            starBanner = false
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
    // Tek dokunarak seçim yapan çocuk: 2+ taş seçili kalınca kısa bir bekleyişten sonra
    // kendiliğinden kontrol edilir. Sürüklemede bu bekleme çalışmaz.
    LaunchedEffect(session.selected, pointerDown) {
        if (!pointerDown && session.selected.size >= 2 && !session.isFinished && !session.isTimeUp) {
            delay(1200)
            session.submit()
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GameHeader(
                spec = spec,
                session = session,
                palette = palette,
                onBack = onBack
            )

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
                    cellsByCoord = cellsByCoord,
                    flashRight = flashRight,
                    flashWrong = flashWrong,
                    onPressStart = {
                        pointerDown = true
                        session.beginSelection(it)
                    },
                    onDragInto = { session.dragInto(it) },
                    onRelease = { moved ->
                        pointerDown = false
                        session.endGesture(moved)
                    }
                )

                ParticleBurst(
                    trigger = session.correctTick,
                    color = palette.glow,
                    count = 16,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                )
                StarBurst(
                    trigger = session.comboTick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(320.dp)
                )
                StarBurst(
                    trigger = session.rescueTick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(320.dp)
                )

                if (comboBanner) {
                    ComboBanner(
                        size = session.comboSize,
                        points = session.comboPoints,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                    )
                }

                if (starBanner) {
                    Text(
                        text = "⭐ YILDIZ DESTEĞİ ⭐",
                        color = Color(0xFF5B3A00),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .popIn()
                            .background(GameVisuals.gold.copy(alpha = 0.95f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }

            MessageLine(
                text = session.message,
                accepted = session.lastResult?.accepted,
                palette = palette
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextActionButton(
                    label = "İpucu",
                    emoji = "💡",
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = { session.showHint() }
                )
                TextActionButton(
                    label = "Baştan",
                    emoji = "↻",
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = onReplay
                )
            }
            if (dailyScore > 0) {
                Text(
                    text = "Bugün topladığın yıldız: $dailyScore",
                    color = palette.muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
private fun GameHeader(
    spec: LevelSpec,
    session: GameSession,
    palette: WorldPalette,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "‹ Bölümler",
            color = palette.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(palette.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE8DCC8), RoundedCornerShape(12.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = CurriculumRepository.topTitle(spec).uppercase(),
                color = palette.onSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(5.dp)
                    .background(Color(0xFFE7DCC8), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(session.clearedFraction.coerceIn(0f, 1f))
                        .height(5.dp)
                        .background(palette.accent, RoundedCornerShape(50))
                )
            }
            val seconds = session.secondsLeft
            if (seconds != null) {
                Text(
                    text = "⏱ ${clockText(seconds)}",
                    color = if (seconds <= 20) GameVisuals.danger else palette.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .background(palette.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE8DCC8), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("★", color = GameVisuals.gold, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(
                text = session.levelScore.toString(),
                color = palette.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

/**
 * Tahta ve parmak sürükleme katmanı. Hücreler mutlak konumdan hesaplanır:
 * basılı tut → seç, komşuya sürükle → ekle, bırak → otomatik kontrol.
 */
@Composable
private fun NumberBoard(
    spec: LevelSpec,
    session: GameSession,
    palette: WorldPalette,
    cellsByCoord: Map<Pair<Int, Int>, BlockCell>,
    flashRight: Set<Pair<Int, Int>>,
    flashWrong: Set<Pair<Int, Int>>,
    onPressStart: (Pair<Int, Int>) -> Unit,
    onDragInto: (Pair<Int, Int>) -> Unit,
    onRelease: (Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val gap = if (spec.gridSize >= 8) 4.dp else 6.dp
        val innerSide = minOf(maxWidth, maxHeight) - 20.dp
        val cellSize = ((innerSide - gap * (spec.gridSize - 1)) / spec.gridSize)
            .coerceAtLeast(28.dp)
        val textSize: TextUnit = when {
            spec.gridSize >= 10 -> 15.sp
            spec.gridSize >= 8 -> 18.sp
            spec.gridSize >= 6 -> 23.sp
            else -> 28.sp
        }
        val density = LocalDensity.current
        val pitchPx = with(density) { (cellSize + gap).toPx() }
        val cellPx = with(density) { cellSize.toPx() }
        val hintedCoords = session.hinted.toSet()

        fun coordAt(position: Offset): Pair<Int, Int>? {
            if (pitchPx <= 0f) return null
            val col = floor(position.x / pitchPx).toInt()
            val row = floor(position.y / pitchPx).toInt()
            if (row !in 0 until spec.gridSize || col !in 0 until spec.gridSize) return null
            val dx = position.x - col * pitchPx
            val dy = position.y - row * pitchPx
            if (dx > cellPx || dy > cellPx) return null
            return row to col
        }

        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(10.dp)
                .pointerInput(spec.gridSize, spec.levelId, pitchPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = coordAt(down.position) ?: return@awaitEachGesture
                        onPressStart(start)
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val coord = coordAt(change.position)
                            if (coord != null) {
                                if (coord != start) moved = true
                                onDragInto(coord)
                            }
                            change.consume()
                        }
                        onRelease(moved)
                    }
                },
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            for (r in 0 until spec.gridSize) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until spec.gridSize) {
                        val coord = r to c
                        NumberTile(
                            cell = cellsByCoord[coord],
                            isSelected = session.selected.contains(coord),
                            isHinted = hintedCoords.contains(coord),
                            flash = when {
                                flashRight.contains(coord) -> TileFlash.Right
                                flashWrong.contains(coord) -> TileFlash.Wrong
                                else -> TileFlash.None
                            },
                            palette = palette,
                            appearDelayMillis = (r + c) * 18,
                            shakeTick = session.wrongTick,
                            textSize = textSize,
                            modifier = Modifier.size(cellSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageLine(
    text: String,
    accepted: Boolean?,
    palette: WorldPalette
) {
    val color = when (accepted) {
        null -> palette.muted
        true -> GameVisuals.ok
        false -> GameVisuals.danger
    }
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = if (accepted == null) FontWeight.Medium else FontWeight.Bold,
        lineHeight = 16.sp,
        maxLines = 2,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}

/** Kocaman kombo yazısı: kısa süre ekranda kalır, yıldızlarla birlikte parlar. */
@Composable
private fun ComboBanner(
    size: Int,
    points: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .popIn()
                .background(
                    color = Color(0xFFFFF3D6).copy(alpha = 0.97f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(2.dp, GameVisuals.gold, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(
                text = if (size >= 4) "MÜTHİŞ KOMBO!" else "SÜPER KOMBO!",
                color = GameVisuals.gold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = "$size TAŞ  +$points",
                color = Color(0xFF5B3A00),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = "★ ★ ★",
                color = GameVisuals.gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
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
            .background(color = Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiRain(active = true, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(26.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BÖLÜM BİTTİ", color = palette.onSurface, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(
                text = if (flawless) "Hatasız avladın, harikasın!" else "${session.correctCount} doğru, ${session.mistakes} deneme.",
                color = palette.muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "★ ${session.levelScore}",
                color = GameVisuals.gold,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("+${session.rewardGold} altın", GameVisuals.gold)
                if (session.rewardDiamonds > 0) Pill("+${session.rewardDiamonds} elmas", GameVisuals.gem)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${spec.title.uppercase()} tamam",
                color = palette.muted,
                fontSize = 11.sp
            )
            TextActionButton(
                label = "SONRAKİ BÖLÜM",
                emoji = "→",
                palette = palette,
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNextLevel
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextActionButton("Tekrar", "↻", palette, modifier = Modifier.weight(1f), onClick = onReplay)
                TextActionButton("Bölümler", "☰", palette, modifier = Modifier.weight(1f), onClick = onBack)
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
            .background(color = Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(26.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SÜRE DOLDU", color = GameVisuals.danger, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(
                text = "${spec.title} bölümünde süre bitti. Puan kaybetmedin, baştan deneyebilirsin.",
                color = palette.muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            TextActionButton(
                label = "TEKRAR DENE",
                emoji = "↻",
                palette = palette,
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onReplay
            )
            TextActionButton(
                label = "BÖLÜMLERE DÖN",
                emoji = "☰",
                palette = palette,
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            )
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/** Sayacı çocuğun okuyabileceği biçime çevirir: 90 sn ve üstü "2:15". */
private fun clockText(seconds: Int): String {
    if (seconds < 90) return "$seconds sn"
    return "${seconds / 60}:${
        (seconds % 60).toString().padStart(2, '0')
    }"
}
