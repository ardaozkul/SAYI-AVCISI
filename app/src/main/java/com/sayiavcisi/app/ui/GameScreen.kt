package com.sayiavcisi.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.audio.SoundEffectsManager
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GameSession
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.LevelSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.floor

/**
 * Oyun ekranı tek işe odaklanır: tahta + üstte bölüm adı.
 *
 * - Üstte yalnızca "7 TOPLAMA" benzeri bölüm adı, ince bir ilerleme çizgisi ve yıldız puanı var.
 * - Tahta 6 sütun; satır sayısı 6, 10 veya 50 olabilir. Uzun tahtalar dikey kayar.
 * - Bir taşa kısa süre basılı tutulunca taş aktif olur; parmak komşu taşlara sürüklenir.
 * - Doğru kombinasyon parmak kalkmadan anında kabul edilir; taşlar yeşil parlayıp patlar.
 * - Yanlışta kırmızı sarsılma ve puan kaybı olur. 3 veya 4 taşta büyük kombo kutlaması çıkar.
 */
@Composable
fun GameScreen(
    spec: LevelSpec,
    session: GameSession,
    dailyScore: Int,
    endlessBestScore: Int,
    onBack: () -> Unit,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onFinishEndless: () -> Unit,
    onPlaySecond: () -> Unit
) {
    val palette = GameVisuals.palette(spec.worldKey)
    val cellsByCoord: Map<Pair<Int, Int>, BlockCell> =
        session.board.cells.associateBy { it.coord }

    var flashRight by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var flashWrong by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var gestureActive by remember { mutableStateOf(false) }
    var autoMatched by remember { mutableStateOf(false) }
    var comboBanner by remember { mutableStateOf(false) }

    LaunchedEffect(session.correctTick) {
        if (session.correctTick > 0) {
            SoundEffectsManager.playCorrectSound()
            flashRight = session.lastMatchedCoords.toSet()
            delay(760)
            flashRight = emptySet()
        }
    }
    LaunchedEffect(session.wrongTick) {
        if (session.wrongTick > 0) {
            SoundEffectsManager.playGentleWrongSound()
            flashWrong = session.lastWrongCoords.toSet()
            delay(820)
            flashWrong = emptySet()
        }
    }
    LaunchedEffect(session.comboTick) {
        if (session.comboTick > 0) {
            SoundEffectsManager.playBonusSound()
            comboBanner = true
            delay(1750)
            comboBanner = false
        }
    }
    LaunchedEffect(session.rescueTick) {
        if (session.rescueTick > 0) SoundEffectsManager.playBonusSound()
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

    // Tek tek dokunarak seçen çocuk:
    // - Doğru ya da kesin yanlış seçim ANINDA değerlendirilir (140 ms'lik minik nefes payı).
    // - Toplamada hedefe henüz ulaşmadıysa 3. ve 4. taşı eklemesi için kısa süre beklenir;
    //   çocuk devam etmezse seçim 650 ms sonra kesin sonuç olarak denenir.
    // - Sonsuz Av'da görev 4/3/2 taş istiyorsa o sayı dolmadan karar verilmez.
    LaunchedEffect(session.selected, gestureActive) {
        if (gestureActive || session.isFinished || session.isTimeUp) return@LaunchedEffect
        val size = session.selected.size
        if (size < 2) return@LaunchedEffect

        if (session.isEndless) {
            if (size != session.requiredGroupSize) return@LaunchedEffect
            delay(140)
            if (!gestureActive && session.selected.size == size) session.submit()
            return@LaunchedEffect
        }

        delay(140)
        if (gestureActive || session.selected.size != size) return@LaunchedEffect
        if (session.autoRejectIfDecisive()) return@LaunchedEffect
        // Toplama hedefin altında: çocuk 3. ya da 4. taşı ekleyebilir.
        delay(650)
        if (!gestureActive && session.selected.size == size) session.submit()
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (spec.endless) {
                EndlessHeader(
                    session = session,
                    palette = palette,
                    onBack = onBack,
                    onFinish = onFinishEndless
                )
            } else {
                GameHeader(
                    spec = spec,
                    session = session,
                    palette = palette,
                    onBack = onBack
                )
            }

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
                    onTap = { coord ->
                        autoMatched = false
                        session.tapCell(coord)
                        if (!session.autoSubmitIfMatched()) session.autoRejectIfDecisive()
                    },
                    onHoldStart = { coord ->
                        gestureActive = true
                        autoMatched = false
                        session.beginSelection(coord)
                    },
                    onDragInto = { coord ->
                        if (!autoMatched) {
                            session.dragInto(coord)
                            if (session.autoSubmitIfMatched()) autoMatched = true
                        }
                    },
                    onRelease = {
                        gestureActive = false
                        if (!autoMatched) session.endGesture(true)
                        autoMatched = false
                    }
                )

                ParticleBurst(
                    trigger = session.correctTick,
                    color = if (spec.endless) GameVisuals.greenPoint else GameVisuals.gold,
                    count = 18,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(250.dp)
                )
                StarBurst(
                    trigger = session.comboTick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(340.dp)
                )
                StarBurst(
                    trigger = session.rescueTick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(300.dp)
                )

                if (comboBanner) {
                    ComboBanner(
                        size = session.comboSize,
                        points = session.comboPoints,
                        palette = palette,
                        pointColor = if (spec.endless) GameVisuals.greenPoint else GameVisuals.gold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                    )
                }

                FlyingComboPoints(
                    trigger = session.comboTick,
                    points = session.comboPoints,
                    color = if (spec.endless) GameVisuals.greenPoint else GameVisuals.gold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize()
                )
            }

            MessageLine(
                text = session.message,
                accepted = session.lastResult?.accepted,
                palette = palette
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GameVisuals.line.copy(alpha = 0.55f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextActionButton(
                    label = "İpucu",
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = { session.showHint() }
                )
                if (spec.endless) {
                    TextActionButton(
                        label = "Bitir",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = onFinishEndless
                    )
                } else {
                    TextActionButton(
                        label = "Baştan",
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = onReplay
                    )
                }
            }
        }

        if (session.isFinished) {
            if (spec.endless) {
                EndlessCompleteOverlay(
                    session = session,
                    palette = palette,
                    bestScore = endlessBestScore,
                    onReplay = onReplay,
                    onBack = onBack
                )
            } else {
                LevelCompleteOverlay(
                    spec = spec,
                    session = session,
                    palette = palette,
                    onNextLevel = onNextLevel,
                    onReplay = onReplay,
                    onBack = onBack
                )
            }
        } else if (session.isTimeUp) {
            TimeUpOverlay(spec = spec, palette = palette, onReplay = onReplay, onBack = onBack)
        }
    }
}

@Composable
private fun EndlessHeader(
    session: GameSession,
    palette: WorldPalette,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "‹ Bölümler",
            color = palette.onSurface,
            fontFamily = AppFonts.Body,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp, horizontal = 2.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "GÖREV",
                color = GameVisuals.greenPoint,
                fontFamily = AppFonts.Body,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
            Text(
                text = (session.mission?.title ?: "Yeni görev").uppercase(),
                color = palette.onSurface,
                fontFamily = AppFonts.Display,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "● ${session.endlessScore} yeşil puan",
                    color = GameVisuals.greenPoint,
                    fontFamily = AppFonts.Body,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${session.missionsSolved} görev · seri ${session.streak}",
                    color = palette.muted,
                    fontFamily = AppFonts.Body,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "Bitir",
            color = palette.onSurface,
            fontFamily = AppFonts.Body,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onFinish)
                .padding(vertical = 8.dp, horizontal = 2.dp)
        )
    }
}

@Composable
private fun EndlessCompleteOverlay(
    session: GameSession,
    palette: WorldPalette,
    bestScore: Int,
    onReplay: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF1D272B).copy(alpha = 0.34f)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiRain(active = true, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(26.dp))
                .border(1.dp, GameVisuals.line, RoundedCornerShape(26.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SONSUZ AV BİTTİ",
                color = palette.onSurface,
                fontFamily = AppFonts.Display,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "● ${session.endlessScore} yeşil puan",
                color = GameVisuals.greenPoint,
                fontFamily = AppFonts.Display,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${session.missionsSolved} görev · en iyi seri ${session.bestEndlessStreak}",
                color = palette.muted,
                fontFamily = AppFonts.Body,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Ayrı yeşil puan kaydı · en iyi ${maxOf(bestScore, session.endlessScore)}",
                color = GameVisuals.greenPoint,
                fontFamily = AppFonts.Body,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            TextActionButton(
                label = "TEKRAR OYNA",
                palette = palette,
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onReplay
            )
            TextActionButton(
                label = "BÖLÜMLERE DÖN",
                palette = palette,
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            )
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
            fontFamily = AppFonts.Body,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 8.dp, horizontal = 2.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = CurriculumRepository.topTitle(spec).uppercase(),
                    color = palette.onSurface,
                    fontFamily = AppFonts.Display,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1
                )
                if (spec.isLong) {
                    Text(
                        text = "UZUN",
                        color = GameVisuals.gold,
                        fontFamily = AppFonts.Body,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .border(1.dp, GameVisuals.gold.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(124.dp)
                    .height(4.dp)
                    .background(GameVisuals.line.copy(alpha = 0.7f), RoundedCornerShape(50))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(session.clearedFraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(palette.accent, RoundedCornerShape(50))
                )
            }
            val seconds = session.secondsLeft
            if (seconds != null) {
                Text(
                    text = clockText(seconds),
                    color = if (seconds <= 20) GameVisuals.danger else palette.muted,
                    fontFamily = AppFonts.Body,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(end = 2.dp)
        ) {
            Text(
                text = "★",
                color = GameVisuals.gold,
                fontFamily = AppFonts.Display,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = session.levelScore.toString(),
                color = palette.onSurface,
                fontFamily = AppFonts.Display,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

/**
 * Tahta ve parmak katmanı.
 *
 * 6 sütun sabittir; 6×10 ve 6×50 tahtalar dikey kayar. Dokunma koordinatları, kaydırma
 * konumu hesaba katılarak tahta koordinatına çevrilir. Kısa dokunuş seçer, kısa basılı
 * tutuş sürükleyerek seçmeye başlar; böylece kaydırma ile seçim çakışmaz.
 */
@Composable
private fun NumberBoard(
    spec: LevelSpec,
    session: GameSession,
    palette: WorldPalette,
    cellsByCoord: Map<Pair<Int, Int>, BlockCell>,
    flashRight: Set<Pair<Int, Int>>,
    flashWrong: Set<Pair<Int, Int>>,
    onTap: (Pair<Int, Int>) -> Unit,
    onHoldStart: (Pair<Int, Int>) -> Unit,
    onDragInto: (Pair<Int, Int>) -> Unit,
    onRelease: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val gap = 5.dp
        val pad = 10.dp
        val availableWidth = maxWidth - 8.dp
        val cell = ((availableWidth - pad * 2 - gap * (spec.cols - 1)) / spec.cols)
            .coerceIn(14.dp, 64.dp)
        val boardWidth = cell * spec.cols + gap * (spec.cols - 1) + pad * 2
        val contentHeight = cell * spec.rows + gap * (spec.rows - 1) + pad * 2
        val viewportHeight = if (contentHeight < maxHeight) contentHeight else maxHeight
        val listState = rememberLazyListState()
        val density = LocalDensity.current
        val viewConfiguration = LocalViewConfiguration.current

        // İpucu, 6×100 tahtanın ekran dışındaki satırındaysa çocuğu oraya götür.
        LaunchedEffect(session.hintTick, session.hinted) {
            if (session.hinted.isEmpty()) return@LaunchedEffect
            val row = session.hinted.minOf { it.first }
            listState.animateScrollToItem(row.coerceIn(0, spec.rows - 1))
        }

        val cellPx = with(density) { cell.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val padPx = with(density) { pad.toPx() }
        val pitchPx = cellPx + gapPx
        val holdMillis = if (spec.rows > 6) 165L else 130L

        fun coordAt(position: Offset): Pair<Int, Int>? {
            if (pitchPx <= 0f) return null
            val scrollPx = listState.firstVisibleItemIndex * pitchPx +
                listState.firstVisibleItemScrollOffset
            val contentX = position.x - padPx
            val contentY = position.y + scrollPx - padPx
            if (contentX < 0f || contentY < 0f) return null
            val col = floor(contentX / pitchPx).toInt()
            val row = floor(contentY / pitchPx).toInt()
            if (row !in 0 until spec.rows || col !in 0 until spec.cols) return null
            val dx = contentX - col * pitchPx
            val dy = contentY - row * pitchPx
            if (dx > cellPx || dy > cellPx) return null
            return row to col
        }

        Box(
            modifier = Modifier
                .width(boardWidth)
                .height(viewportHeight)
                .background(palette.surface.copy(alpha = 0.62f), RoundedCornerShape(22.dp))
                .border(1.dp, GameVisuals.line.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                .pointerInput(spec.levelId, spec.rows, spec.cols, cellPx, gapPx, padPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = coordAt(down.position) ?: return@awaitEachGesture
                        var moved = false

                        val quick = withTimeoutOrNull(holdMillis) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: return@withTimeoutOrNull false
                                if (!change.pressed) return@withTimeoutOrNull true
                                if ((change.position - down.position).getDistance() >
                                    viewConfiguration.touchSlop
                                ) {
                                    moved = true
                                    return@withTimeoutOrNull false
                                }
                            }
                            @Suppress("UNREACHABLE_CODE")
                            false
                        }

                        if (quick == true) {
                            onTap(start)
                            return@awaitEachGesture
                        }
                        if (moved) return@awaitEachGesture

                        onHoldStart(start)
                        var last = start
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val coord = coordAt(change.position)
                            if (coord != null && coord != last) {
                                last = coord
                                onDragInto(coord)
                            }
                            change.consume()
                        }
                        onRelease()
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(pad),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                items(count = spec.rows, key = { it }) { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        for (c in 0 until spec.cols) {
                            val coord = r to c
                            NumberTile(
                                cell = cellsByCoord[coord],
                                isSelected = session.selected.contains(coord),
                                isHinted = session.hinted.contains(coord),
                                flash = when {
                                    flashRight.contains(coord) -> TileFlash.Right
                                    flashWrong.contains(coord) -> TileFlash.Wrong
                                    else -> TileFlash.None
                                },
                                palette = palette,
                                appearDelayMillis = if (spec.rows <= 10) (r + c) * 14 else 0,
                                animateAppear = spec.rows <= 10,
                                shakeTick = session.wrongTick,
                                textSize = (cell.value * 0.40f).coerceAtLeast(9f).sp,
                                modifier = Modifier.size(cell)
                            )
                        }
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
        fontFamily = AppFonts.Body,
        fontSize = 11.sp,
        fontWeight = if (accepted == null) FontWeight.Medium else FontWeight.Bold,
        lineHeight = 14.sp,
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
    palette: WorldPalette,
    pointColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .popIn()
                .background(
                    color = palette.surface.copy(alpha = 0.98f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.5.dp, pointColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 26.dp, vertical = 14.dp)
        ) {
            Text(
                text = if (size >= 4) "MÜTHİŞ KOMBO" else "SÜPER KOMBO",
                color = palette.accentDeep,
                fontFamily = AppFonts.Display,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = "$size TAŞ  +$points",
                color = pointColor,
                fontFamily = AppFonts.Display,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = "★  ★  ★",
                color = pointColor,
                fontFamily = AppFonts.Display,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** Komboda kazanılan puan büyüyerek yukarıdaki yıldız sayacına uçar. */
@Composable
private fun FlyingComboPoints(
    trigger: Int,
    points: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 1150, easing = FastOutSlowInEasing))
        }
    }
    if (trigger <= 0) return

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val p = progress.value
        val alpha = if (p < 0.72f) 1f else (1f - (p - 0.72f) / 0.28f).coerceIn(0f, 1f)
        val scale = 1.25f - p * 0.35f
        val endX = maxWidth.value * 0.30f
        val startY = maxHeight.value * 0.05f
        val endY = -maxHeight.value * 0.48f
        Text(
            text = "+$points",
            color = color,
            fontFamily = AppFonts.Display,
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.graphicsLayer {
                translationX = endX.dp.toPx() * p
                translationY = startY.dp.toPx() + (endY - startY).dp.toPx() * p
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
        )
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
            .background(color = Color(0xFF1F2C26).copy(alpha = 0.34f)),
        contentAlignment = Alignment.Center
    ) {
        ConfettiRain(active = true, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(26.dp))
                .border(1.dp, GameVisuals.line, RoundedCornerShape(26.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "BÖLÜM BİTTİ",
                color = palette.onSurface,
                fontFamily = AppFonts.Display,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (flawless) "Hatasız avladın, harikasın!" else "${session.correctCount} doğru, ${session.mistakes} deneme.",
                color = palette.muted,
                fontFamily = AppFonts.Body,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "★ ${session.levelScore}",
                color = GameVisuals.gold,
                fontFamily = AppFonts.Display,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("+${session.rewardGold} altın", GameVisuals.gold)
                if (session.rewardDiamonds > 0) Pill("+${session.rewardDiamonds} elmas", GameVisuals.gem)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${spec.title.uppercase()} · ${CurriculumRepository.boardSizeLabel(spec)} tamam",
                color = palette.muted,
                fontFamily = AppFonts.Body,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            TextActionButton(
                label = "SONRAKİ BÖLÜM",
                palette = palette,
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNextLevel
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextActionButton("Tekrar", palette, modifier = Modifier.weight(1f), onClick = onReplay)
                TextActionButton("Bölümler", palette, modifier = Modifier.weight(1f), onClick = onBack)
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
            .background(color = Color(0xFF1F2C26).copy(alpha = 0.34f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .background(color = palette.surface, shape = RoundedCornerShape(26.dp))
                .border(1.dp, GameVisuals.line, RoundedCornerShape(26.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SÜRE DOLDU",
                color = GameVisuals.danger,
                fontFamily = AppFonts.Display,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "${spec.title} bölümünde süre bitti. Puan kaybetmedin, baştan deneyebilirsin.",
                color = palette.muted,
                fontFamily = AppFonts.Body,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            TextActionButton(
                label = "TEKRAR DENE",
                palette = palette,
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onReplay
            )
            TextActionButton(
                label = "BÖLÜMLERE DÖN",
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
        fontFamily = AppFonts.Body,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(50))
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
