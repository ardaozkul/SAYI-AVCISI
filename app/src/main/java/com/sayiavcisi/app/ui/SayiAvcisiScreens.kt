package com.sayiavcisi.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sayiavcisi.app.audio.SoundEffectsManager
import com.sayiavcisi.app.data.LocalProgressStorage
import com.sayiavcisi.app.data.ParentStatsSnapshot
import com.sayiavcisi.app.engine.BoardGeneratorAndSolver
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.MatchingValidator
import com.sayiavcisi.app.engine.ScoreEngine
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.DifficultyTier
import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationFeedback
import com.sayiavcisi.app.model.OperationType
import kotlinx.coroutines.delay

enum class AppScreen {
    ADVENTURE_HUB,
    CUSTOM_ADDITION_LAB,
    GAMEPLAY,
    PARENT_PANEL
}

@Composable
fun SayiAvcisiApp(storage: LocalProgressStorage) {
    var stats by remember { mutableStateOf(storage.loadSnapshot()) }
    var currentScreen by remember { mutableStateOf(AppScreen.ADVENTURE_HUB) }
    var selectedDayTab by remember { mutableIntStateOf(1) }
    var activeLevelSpec by remember {
        val initialId = stats.lastActiveLevelId ?: "D1_S1"
        mutableStateOf(CurriculumRepository.getLevelById(initialId))
    }

    LaunchedEffect(stats.soundEnabled) {
        SoundEffectsManager.isSoundEnabled = stats.soundEnabled
    }

    fun refreshStats() {
        stats = storage.loadSnapshot()
    }

    when (currentScreen) {
        AppScreen.ADVENTURE_HUB -> {
            AdventureHubScreen(
                stats = stats,
                selectedDay = selectedDayTab,
                onSelectDay = { selectedDayTab = it },
                onStartLevel = { spec ->
                    activeLevelSpec = spec
                    storage.saveLastActiveLevel(spec.levelId)
                    refreshStats()
                    currentScreen = AppScreen.GAMEPLAY
                },
                onContinueLastLevel = {
                    val lastId = stats.lastActiveLevelId ?: "D1_S1"
                    activeLevelSpec = CurriculumRepository.getLevelById(lastId)
                    currentScreen = AppScreen.GAMEPLAY
                },
                onOpenAdditionLab = {
                    currentScreen = AppScreen.CUSTOM_ADDITION_LAB
                },
                onOpenParentPanel = {
                    refreshStats()
                    currentScreen = AppScreen.PARENT_PANEL
                },
                onToggleSoundQuick = {
                    val next = !stats.soundEnabled
                    storage.setSoundEnabled(next)
                    SoundEffectsManager.isSoundEnabled = next
                    refreshStats()
                }
            )
        }

        AppScreen.CUSTOM_ADDITION_LAB -> {
            CustomAdditionLabScreen(
                onSelectCustomSpec = { spec ->
                    activeLevelSpec = spec
                    currentScreen = AppScreen.GAMEPLAY
                },
                onBack = {
                    currentScreen = AppScreen.ADVENTURE_HUB
                }
            )
        }

        AppScreen.GAMEPLAY -> {
            ActivePuzzleGameScreen(
                levelSpec = activeLevelSpec,
                stats = stats,
                storage = storage,
                onStatsChanged = { refreshStats() },
                onNextLevelRequested = { nextSpec ->
                    activeLevelSpec = nextSpec
                    storage.saveLastActiveLevel(nextSpec.levelId)
                    refreshStats()
                },
                onExitToHub = {
                    refreshStats()
                    currentScreen = AppScreen.ADVENTURE_HUB
                }
            )
        }

        AppScreen.PARENT_PANEL -> {
            ParentPanelScreen(
                stats = stats,
                onToggleSound = { enabled ->
                    storage.setSoundEnabled(enabled)
                    SoundEffectsManager.isSoundEnabled = enabled
                    refreshStats()
                },
                onToggleRelaxedMode = { relaxed ->
                    storage.setRelaxedUntimedMode(relaxed)
                    refreshStats()
                },
                onChangeExtraTime = { extraSec ->
                    storage.setCustomTimeAdditionSeconds(extraSec)
                    refreshStats()
                },
                onResetDailyScore = {
                    storage.resetDailyScoreOnly()
                    refreshStats()
                },
                onBack = {
                    refreshStats()
                    currentScreen = AppScreen.ADVENTURE_HUB
                }
            )
        }
    }
}

/**
 * 5 Günlük Eğitim Programı ve Macera Haritası Ana Ekranı.
 */
@Composable
fun AdventureHubScreen(
    stats: ParentStatsSnapshot,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    onStartLevel: (LevelSpec) -> Unit,
    onContinueLastLevel: () -> Unit,
    onOpenAdditionLab: () -> Unit,
    onOpenParentPanel: () -> Unit,
    onToggleSoundQuick: () -> Unit
) {
    val dayLevels = remember(selectedDay) { CurriculumRepository.getLevelsForDay(selectedDay) }
    val theme = dayLevels.firstOrNull()?.themeWorld ?: com.sayiavcisi.app.model.ThemeWorld.SAYI_ORMANI
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(theme.bgTopHex), Color(theme.bgBottomHex))
                )
            )
            .verticalScroll(scrollState)
            .padding(14.dp)
    ) {
        // Üst Bar: Başlık, Ses Butonu ve Ebeveyn Paneli
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🦊", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SAYI AVCISI",
                        color = Color(0xFFFBBF24),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "3. Sınıf Matematik Bulmaca Macerası",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onToggleSoundQuick() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (stats.soundEnabled) "🔊 Ses" else "🔇 Sessiz",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF334155))
                        .clickable { onOpenParentPanel() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "👨‍👩‍👧 Ebeveyn",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Karakter & Puan Durumu
        ExplorerMascotBanner(
            moodMessage = "Hoş geldin Sayı Avcısı! Blokları eşleştir, yıldızları ve elmasları topla!",
            isCelebrating = false,
            isGentleWarning = false,
            dailyScore = stats.dailyScore,
            goldCoins = stats.totalGoldCoins,
            diamonds = stats.totalDiamonds
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Kaldığı Yerden Devam Et & Özel Toplama Basamakları Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onContinueLastLevel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "▶ Kaldığın Yerden Devam Et",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Button(
                onClick = onOpenAdditionLab,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "🎯 Hedef Toplam Seç (1-100)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5 Günlük Sekmeler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (day in 1..5) {
                val isSelected = day == selectedDay
                val isDayCompleted = stats.completedDays.contains(day)
                val dayTheme = CurriculumRepository.getLevelsForDay(day).first().themeWorld
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(dayTheme.primaryColorHex)
                            else Color(0xFF0F172A).copy(alpha = 0.7f)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) Color.White else Color(0xFF475569),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectDay(day) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = dayTheme.badgeEmoji, fontSize = 16.sp)
                    Text(
                        text = "$day. GÜN",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isDayCompleted) "✅ Tamam" else "60 dk",
                        color = Color(0xFFE2E8F0),
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Seçili Günün Tema Başlığı ve 60 Dakikalık Plan Özeti
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                .border(1.dp, Color(theme.primaryColorHex), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "${theme.badgeEmoji} ${theme.subtitle} — ${theme.title}",
                color = Color(0xFFFBBF24),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Günlük 60 Dakikalık Program: 10 dk Isınma • 15 dk 1. Bulmaca • 5 dk Mola • 15 dk 2. Bulmaca • 10 dk Karışık • 5 dk Ödül",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Günün 6 Aşaması
        dayLevels.forEach { level ->
            val isCompleted = stats.completedLevelIds.contains(level.levelId)
            val isFreeMode = level.matchRuleMode == MatchRuleMode.FREE

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                    .border(
                        1.5.dp,
                        if (isCompleted) Color(0xFF34D399) else Color(theme.primaryColorHex).copy(alpha = 0.7f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onStartLevel(level) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) Color(0xFF059669)
                                else Color(theme.primaryColorHex)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCompleted) "✓" else "${level.stageIndex}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${level.segmentType.label} (${level.segmentType.recommendedMinutes} dk)",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFreeMode) "• Serbest Kural" else "• Doğrusal Kural",
                                color = if (isFreeMode) Color(0xFF38BDF8) else Color(0xFFA7F3D0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = level.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = level.subtitle,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(theme.primaryColorHex))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "OYNA",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * Bölüm 3'te istenen tüm özel Toplama Hedeflerine (1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
 * 11-15, 16-20, 21-30, 31-50, 51-100), 2'li / 3'lü eşleştirmeye ve 4×4..10×10 tahta boyutlarına
 * doğrudan erişim sağlayan Eğitim Laboratuvarı ekranı.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomAdditionLabScreen(
    onSelectCustomSpec: (LevelSpec) -> Unit,
    onBack: () -> Unit
) {
    var operandCount by remember { mutableIntStateOf(2) }
    var selectedTier by remember { mutableStateOf(DifficultyTier.STARTER) }
    var selectedRuleMode by remember { mutableStateOf(MatchRuleMode.DIRECTIONAL) }
    val scrollState = rememberScrollState()

    val targetRanges = listOf(
        Pair(1, 1) to "1'e Ulaşma (0+1)",
        Pair(2, 2) to "2'ye Ulaşma",
        Pair(3, 3) to "3'e Ulaşma",
        Pair(4, 4) to "4'e Ulaşma",
        Pair(5, 5) to "5'e Ulaşma",
        Pair(6, 6) to "6'ya Ulaşma",
        Pair(7, 7) to "7'ye Ulaşma",
        Pair(8, 8) to "8'e Ulaşma",
        Pair(9, 9) to "9'a Ulaşma",
        Pair(10, 10) to "10'a Ulaşma",
        Pair(11, 15) to "11-15 Arası",
        Pair(16, 20) to "16-20 Arası",
        Pair(21, 30) to "21-30 Arası",
        Pair(31, 50) to "31-50 Arası",
        Pair(51, 100) to "51-100 Arası"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF0F172A))))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🎯 ADIM ADIM TOPLAMA HEDEFLERİ",
                    color = Color(0xFFFBBF24),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "1'den 100'e kadar istediğin hedef toplamı ve tahta boyutunu seç!",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("← Geri", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Kaç sayı eşleştirilsin? (2 veya 3)
        Text("Eşleştirilecek Sayı Adedi:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2 to "2 Sayı Eşleştir (Örn: 3+4=7)", 3 to "3 Sayı Eşleştir (Örn: 2+3+2=7)").forEach { (cnt, title) ->
                val active = operandCount == cnt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color(0xFF10B981) else Color(0xFF1E293B))
                        .clickable { operandCount = cnt }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tahta Boyutu Seçimi (4x4, 5x5, 6x6, 8x8, 10x10)
        Text("Oyun Tahtası Boyutu:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DifficultyTier.entries.forEach { tier ->
                val active = selectedTier == tier
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color(0xFFF59E0B) else Color(0xFF1E293B))
                        .clickable { selectedTier = tier }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(text = tier.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Eşleştirme Kuralı (Doğrusal vs Serbest)
        Text("Eşleştirme Kuralı:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MatchRuleMode.entries.forEach { mode ->
                val active = selectedRuleMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Color(0xFF3B82F6) else Color(0xFF1E293B))
                        .clickable { selectedRuleMode = mode }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = mode.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Hedef Toplamı Seç ve Başla:", color = Color(0xFFFBBF24), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            targetRanges.forEach { (range, label) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                        .clickable {
                            val safeOperand = if (range.second < 3) 2 else operandCount
                            onSelectCustomSpec(
                                CurriculumRepository.createCustomAdditionLevel(
                                    minTarget = range.first,
                                    maxTarget = range.second,
                                    operandCount = safeOperand,
                                    tier = selectedTier,
                                    ruleMode = selectedRuleMode
                                )
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "🎯 $label",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Ana Bulmaca ve Oyun Tahtası Ekranı:
 * - 4×4, 5×5, 6×6, 8×8, 10×10 dinamik ızgara
 * - Doğrusal (yatay/dikey/çapraz, arasında aktif blok olmadan) ve Serbest eşleştirme modu
 * - 3 Joker (İpucu -5, Yeniden Düzenle -10, Çifte Hamle -5)
 * - Gizli 4. Kurtarma: ALTIN KURTARMA (3 joker biter ve hamle kalmazsa otomatik, sessiz, -0 puan)
 * - Süreli ve Rahat (süresiz) oyun modu, süre dolduğunda cezalandırmadan tekrar deneme imkanı
 * - Görsel çıkarma nesne eksilme animasyonu, görsel çarpma gruplama, yıldız yağmuru ve konfeti
 */
@Composable
fun ActivePuzzleGameScreen(
    levelSpec: LevelSpec,
    stats: ParentStatsSnapshot,
    storage: LocalProgressStorage,
    onStatsChanged: () -> Unit,
    onNextLevelRequested: (LevelSpec) -> Unit,
    onExitToHub: () -> Unit
) {
    // Seviye değiştiğinde garantili çözülebilir tahtayı üret
    var boardGenerationSeed by remember(levelSpec.levelId) { mutableIntStateOf(0) }
    val generatedBoard = remember(levelSpec.levelId, boardGenerationSeed) {
        BoardGeneratorAndSolver.generateVerifiedBoard(levelSpec)
    }

    var cells by remember(generatedBoard) { mutableStateOf(generatedBoard.cells) }
    var remainingTargets by remember(generatedBoard) { mutableStateOf(generatedBoard.targets) }
    var effectiveRuleMode by remember(generatedBoard) { mutableStateOf(generatedBoard.matchRuleMode) }
    var selectedCoords by remember(generatedBoard) { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    // Başlangıçta 3 joker hakkı
    var hintJokerCount by remember(generatedBoard) { mutableIntStateOf(1) }
    var shuffleJokerCount by remember(generatedBoard) { mutableIntStateOf(1) }
    var doubleScoreJokerCount by remember(generatedBoard) { mutableIntStateOf(1) }
    var isDoubleScoreActive by remember(generatedBoard) { mutableStateOf(false) }

    var currentStreak by remember(generatedBoard) { mutableIntStateOf(0) }
    var mistakesInLevel by remember(generatedBoard) { mutableIntStateOf(0) }
    var feedback by remember(generatedBoard) { mutableStateOf<OperationFeedback?>(null) }
    var mascotMessage by remember(generatedBoard) {
        mutableStateOf("${levelSpec.themeWorld.title} bölümüne hoş geldin! Hedefe uygun sayıları eşleştir.")
    }
    var isCelebrating by remember(generatedBoard) { mutableStateOf(false) }
    var isGentleWarning by remember(generatedBoard) { mutableStateOf(false) }
    var burstTriggerKey by remember { mutableIntStateOf(0) }
    var isLevelCompleted by remember(generatedBoard) { mutableStateOf(false) }
    var isTimeExpiredPrompt by remember(generatedBoard) { mutableStateOf(false) }

    // Süre yönetimi (Kolay/Başlangıç: Süresiz, Orta: 90 sn, Zor: 60 sn, Uzman: 45 sn)
    var isRelaxedModeLocal by remember(generatedBoard) { mutableStateOf(stats.relaxedUntimedMode) }
    val baseTimeLimit = levelSpec.difficultyTier.defaultTimeSeconds
    val initialSeconds = if (baseTimeLimit != null) {
        (baseTimeLimit + stats.customTimeAdditionSeconds).coerceAtLeast(25)
    } else null

    var remainingSeconds by remember(generatedBoard, isRelaxedModeLocal) {
        mutableStateOf(if (isRelaxedModeLocal) null else initialSeconds)
    }

    var stepStartTimestampMs by remember(generatedBoard) { mutableLongStateOf(System.currentTimeMillis()) }

    // Günlük oyun süresi sayacı ve geri sayım
    LaunchedEffect(generatedBoard, isRelaxedModeLocal, isLevelCompleted, isTimeExpiredPrompt) {
        while (!isLevelCompleted && !isTimeExpiredPrompt) {
            delay(1000L)
            storage.addPlayTimeSeconds(1L)
            val currentRem = remainingSeconds
            if (!isRelaxedModeLocal && currentRem != null) {
                if (currentRem > 1) {
                    remainingSeconds = currentRem - 1
                } else {
                    remainingSeconds = 0
                    isTimeExpiredPrompt = true
                    mascotMessage = "Süre doldu ama hiç sorun değil! Ceza yok, süreli veya rahat modda hemen devam edebilirsin!"
                }
            }
        }
    }

    // Her hamleden veya durum değişiminden sonra:
    // Mevcut hedef için geçerli hamle var mı kontrol et!
    // Eğer geçerli hamle kalmadıysa ve 3 joker de tükendiyse (veya oyuncunun tahtayı açacak jokeri kalmadıysa)
    // ALTIN KURTARMA (Bölüm 9) sessiz ve otomatik olarak devreye girer!
    val currentTargetStep = remainingTargets.firstOrNull()
    LaunchedEffect(cells, remainingTargets, hintJokerCount, shuffleJokerCount, doubleScoreJokerCount, effectiveRuleMode) {
        if (currentTargetStep != null && !isLevelCompleted) {
            val validMoves = MatchingValidator.findAllValidMovesForStep(
                cells = cells,
                step = currentTargetStep,
                mode = effectiveRuleMode
            )
            if (validMoves.isEmpty()) {
                // Tahtada geçerli hamle bulunamadı!
                // Eğer üç joker de tükendiyse (veya Yeniden Düzenle jokeri bittiyse) ALTIN KURTARMA devreye girer:
                // Çocuğa başarısız olduğu hissettirilmez, ekstra puan düşülmez, tahta otomatik çözülebilir duruma getirilir.
                val rescue = BoardGeneratorAndSolver.triggerGoldenRescue(
                    currentCells = cells,
                    remainingTargets = remainingTargets,
                    mode = effectiveRuleMode,
                    gridSize = generatedBoard.gridSize
                )
                if (rescue.wasTriggered) {
                    cells = rescue.updatedCells
                    effectiveRuleMode = rescue.effectiveMode
                    mascotMessage = "✨ Altın Kurtarma Işığı parladı! Yeni sayı dizilimiyle maceraya devam!"
                }
            }
        }
    }

    // Hücre seçimi işleyicisi
    fun onCellClicked(cell: BlockCell) {
        if (cell.isCleared || isLevelCompleted || isTimeExpiredPrompt || currentTargetStep == null) return

        val coord = Pair(cell.row, cell.col)
        val nextSelected = if (selectedCoords.contains(coord)) {
            selectedCoords - coord
        } else {
            selectedCoords + coord
        }

        // Henüz gereken sayı adedine ulaşılmadıysa seçimi güncelle
        if (nextSelected.size < currentTargetStep.operandCount) {
            selectedCoords = nextSelected
            return
        }

        // Gereken sayı adedine ulaşıldı (2 veya 3 blok seçildi): Doğrulama yap!
        val activeCoords = cells.filter { !it.isCleared }.map { Pair(it.row, it.col) }.toSet()
        val (geoValid, geoErrorMsg) = MatchingValidator.isGeometricallyValidSelection(
            selectedCoords = nextSelected,
            activeCoords = activeCoords,
            mode = effectiveRuleMode
        )

        val selectedCells = nextSelected.mapNotNull { c ->
            cells.firstOrNull { !it.isCleared && it.row == c.first && it.col == c.second }
        }
        val selectedValues = selectedCells.map { it.value }
        val elapsedMs = (System.currentTimeMillis() - stepStartTimestampMs).coerceAtLeast(300L)

        if (!geoValid) {
            // Geometrik kural ihlali (ör. aralarında başka aktif sayı bloğu var veya aynı doğrultuda değil)
            SoundEffectsManager.playGentleWrongSound()
            isCelebrating = false
            isGentleWarning = true
            selectedCoords = emptyList()
            val msg = geoErrorMsg ?: "Bu bloklar aynı doğrultuda değil veya aralarında başka bir sayı var!"
            mascotMessage = msg
            feedback = OperationFeedback(
                isCorrect = false,
                expressionText = selectedValues.joinToString(" , "),
                explanationText = msg,
                operation = currentTargetStep.operation,
                operands = selectedValues,
                result = 0,
                pointsDelta = 0
            )
            return
        }

        // Matematiksel doğrulama
        val mathResult = MatchingValidator.evaluateMathMatch(selectedValues, currentTargetStep)
        storage.recordOperationAttempt(
            operation = currentTargetStep.operation,
            isCorrect = mathResult.isValid,
            operands = selectedValues,
            targetPrompt = currentTargetStep.promptDisplay,
            responseTimeMs = elapsedMs
        )

        if (mathResult.isValid) {
            // DOĞRU EŞLEŞME!
            SoundEffectsManager.playCorrectSound()
            val scoreRes = ScoreEngine.onCorrectMatch(
                currentDailyScore = storage.loadSnapshot().dailyScore,
                currentStreak = currentStreak,
                operandCount = currentTargetStep.operandCount,
                isDoubleScoreActive = isDoubleScoreActive
            )
            storage.saveDailyScore(scoreRes.newDailyScore)
            storage.addRewards(goldDelta = 5 + (if (isDoubleScoreActive) 5 else 0), diamondDelta = if (scoreRes.streakBonusAwarded > 0) 1 else 0)

            currentStreak = scoreRes.newStreak
            isDoubleScoreActive = false
            isCelebrating = true
            isGentleWarning = false
            burstTriggerKey += 1

            // Eşleşen blokları temizle
            val clearedSet = nextSelected.toSet()
            cells = cells.map { c ->
                if (clearedSet.contains(Pair(c.row, c.col))) {
                    c.copy(isCleared = true, isHinted = false)
                } else {
                    c.copy(isHinted = false)
                }
            }
            selectedCoords = emptyList()
            val nextTargets = remainingTargets.drop(1)
            remainingTargets = nextTargets
            stepStartTimestampMs = System.currentTimeMillis()

            val streakNote = if (scoreRes.streakBonusAwarded > 0) " 🔥 ${scoreRes.newStreak}'li Seri Bonusu (+${scoreRes.streakBonusAwarded} Puan)!" else ""
            mascotMessage = "${mathResult.feedbackMessage} (+${scoreRes.appliedDelta} Puan)$streakNote"

            feedback = OperationFeedback(
                isCorrect = true,
                expressionText = mathResult.expression,
                explanationText = mathResult.feedbackMessage,
                operation = currentTargetStep.operation,
                operands = selectedValues,
                result = mathResult.resultValue,
                pointsDelta = scoreRes.appliedDelta
            )

            // Tüm bloklar temizlendi mi?
            if (nextTargets.isEmpty() || cells.all { it.isCleared }) {
                isLevelCompleted = true
                SoundEffectsManager.playLevelCompleteFanfare()
                storage.markLevelCompleted(levelSpec.levelId)
                val updatedSnap = storage.loadSnapshot()
                val completedDayNow = updatedSnap.completedDays.contains(levelSpec.dayNumber)
                val finishedInTime = remainingSeconds == null || (remainingSeconds ?: 0) > 0

                val (finalDailyScore, _) = ScoreEngine.onLevelCompleted(
                    currentDailyScore = updatedSnap.dailyScore,
                    hadZeroMistakes = mistakesInLevel == 0,
                    finishedWithinTimeLimit = finishedInTime,
                    completedFullDayQuest = completedDayNow && levelSpec.stageIndex == 6
                )
                storage.saveDailyScore(finalDailyScore)
                storage.addRewards(goldDelta = 20, diamondDelta = 2)
                burstTriggerKey += 1
            }

            onStatsChanged()
        } else {
            // YANLIŞ EŞLEŞME (-1 puan, sıfırın altına düşmez, nazik görsel ipucu)
            SoundEffectsManager.playGentleWrongSound()
            mistakesInLevel += 1
            val scoreRes = ScoreEngine.onWrongMatch(storage.loadSnapshot().dailyScore)
            storage.saveDailyScore(scoreRes.newDailyScore)
            currentStreak = 0
            selectedCoords = emptyList()
            isCelebrating = false
            isGentleWarning = true

            mascotMessage = mathResult.feedbackMessage
            feedback = OperationFeedback(
                isCorrect = false,
                expressionText = mathResult.expression,
                explanationText = "${mathResult.feedbackMessage} (${scoreRes.appliedDelta} Puan)",
                operation = currentTargetStep.operation,
                operands = selectedValues,
                result = mathResult.resultValue,
                pointsDelta = scoreRes.appliedDelta
            )
            onStatsChanged()
        }
    }

    // 3 Joker Kullanım İşleyicisi
    fun useJoker(jokerType: JokerType) {
        if (isLevelCompleted || currentTargetStep == null) return

        when (jokerType) {
            JokerType.HINT -> {
                if (hintJokerCount <= 0) return
                var validMoves = MatchingValidator.findAllValidMovesForStep(cells, currentTargetStep, effectiveRuleMode)
                if (validMoves.isEmpty()) {
                    val rescue = BoardGeneratorAndSolver.triggerGoldenRescue(cells, remainingTargets, effectiveRuleMode, generatedBoard.gridSize)
                    cells = rescue.updatedCells
                    effectiveRuleMode = rescue.effectiveMode
                    validMoves = MatchingValidator.findAllValidMovesForStep(cells, currentTargetStep, effectiveRuleMode)
                }
                val hintCoords = validMoves.firstOrNull()?.toSet() ?: emptySet()
                if (hintCoords.isNotEmpty()) {
                    hintJokerCount -= 1
                    val (newScore, delta) = ScoreEngine.onJokerUsed(storage.loadSnapshot().dailyScore, JokerType.HINT)
                    storage.saveDailyScore(newScore)
                    SoundEffectsManager.playBonusSound()
                    cells = cells.map { c ->
                        c.copy(isHinted = !c.isCleared && hintCoords.contains(Pair(c.row, c.col)))
                    }
                    val hintedValues = hintCoords.mapNotNull { coord ->
                        cells.firstOrNull { !it.isCleared && it.row == coord.first && it.col == coord.second }?.value
                    }
                    mascotMessage = "💡 İPUCU: Parlayan ${hintedValues.joinToString(" ve ")} bloklarını sırayla seçebilirsin! ($delta Puan)"
                    onStatsChanged()
                }
            }

            JokerType.SHUFFLE -> {
                if (shuffleJokerCount <= 0) return
                shuffleJokerCount -= 1
                val (newScore, delta) = ScoreEngine.onJokerUsed(storage.loadSnapshot().dailyScore, JokerType.SHUFFLE)
                storage.saveDailyScore(newScore)
                SoundEffectsManager.playBonusSound()

                val (rearranged, _) = BoardGeneratorAndSolver.rearrangeRemainingBoardGuaranteed(
                    currentCells = cells,
                    remainingTargets = remainingTargets,
                    mode = effectiveRuleMode
                )
                cells = rearranged
                selectedCoords = emptyList()
                mascotMessage = "🔄 YENİDEN DÜZENLE: Tahta çözülebilir yeni bir düzene sokuldu! ($delta Puan)"
                onStatsChanged()
            }

            JokerType.DOUBLE_SCORE -> {
                if (doubleScoreJokerCount <= 0 || isDoubleScoreActive) return
                doubleScoreJokerCount -= 1
                val (newScore, delta) = ScoreEngine.onJokerUsed(storage.loadSnapshot().dailyScore, JokerType.DOUBLE_SCORE)
                storage.saveDailyScore(newScore)
                SoundEffectsManager.playBonusSound()
                isDoubleScoreActive = true
                mascotMessage = "⚡ ÇİFTE HAMLE Aktif! Bir sonraki doğru eşleşmenin puanı ikiye katlanacak! ($delta Puan)"
                onStatsChanged()
            }
        }
    }

    val theme = levelSpec.themeWorld

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(theme.bgTopHex), Color(theme.bgBottomHex))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Üst Bilgi Çubuğu (Haritaya Dön, Seviye Başlığı, Süre ve Rahat Mod Geçişi)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { onExitToHub() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("← Harita", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${theme.badgeEmoji} ${levelSpec.title}",
                            color = Color(0xFFFBBF24),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = "${levelSpec.difficultyTier.label} • ${effectiveRuleMode.title}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp
                        )
                    }

                    // Süre göstergesi veya Rahat Mod butonu
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (remainingSeconds != null && (remainingSeconds ?: 99) <= 15) Color(0xFF991B1B)
                                else Color(0xFF1E293B)
                            )
                            .clickable {
                                isRelaxedModeLocal = !isRelaxedModeLocal
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val timerText = if (isRelaxedModeLocal || remainingSeconds == null) {
                            "🌿 Rahat Mod"
                        } else {
                            "⏱️ ${remainingSeconds} sn"
                        }
                        Text(text = timerText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Karakter & Puan Banner
                ExplorerMascotBanner(
                    moodMessage = mascotMessage,
                    isCelebrating = isCelebrating,
                    isGentleWarning = isGentleWarning,
                    dailyScore = stats.dailyScore,
                    goldCoins = stats.totalGoldCoins,
                    diamonds = stats.totalDiamonds
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. HEDEF GÖSTERGESİ (Örn: HEDEF: 10 veya ? × 4 = 20 veya 20 ÷ ? = 5)
                if (currentTargetStep != null) {
                    val activeCellsCount = cells.count { !it.isCleared }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(theme.primaryColorHex), Color(theme.secondaryColorHex))
                                )
                            )
                            .border(2.dp, Color(0xFFFBBF24), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTargetStep.promptDisplay,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = currentTargetStep.helperSubtext,
                                color = Color(0xFFFEF08A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Kalan Blok: $activeCellsCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Seçilen: ${selectedCoords.size}/${currentTargetStep.operandCount}",
                                color = Color(0xFFFEF08A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Görsel İşlem Açıklaması (Çıkarma nesne eksilme / Çarpma gruplama)
                VisualMathExplanationCard(feedback = feedback)
            }

            // 3. OYUN TAHTASI (4×4, 5×5, 6×6, 8×8, 10×10 Sudoku benzeri sayı ızgarası)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                PuzzleBoardGrid(
                    gridSize = generatedBoard.gridSize,
                    cells = cells,
                    selectedCoords = selectedCoords,
                    themePrimaryColor = Color(theme.primaryColorHex),
                    onCellClick = { onCellClicked(it) }
                )
            }

            // 4. ALT JOKER VE BONUS PANELİ (İpucu -5, Yeniden Düzenle -10, Çifte Hamle -5 + Altın Kurtarma Güvencesi)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    JokerActionButton(
                        title = "💡 İPUCU",
                        subtitle = "-5 Puan (${hintJokerCount})",
                        enabled = hintJokerCount > 0 && !isLevelCompleted,
                        activeHighlight = false,
                        color = Color(0xFF0284C7),
                        onClick = { useJoker(JokerType.HINT) },
                        modifier = Modifier.weight(1f)
                    )
                    JokerActionButton(
                        title = "🔄 DÜZENLE",
                        subtitle = "-10 Puan (${shuffleJokerCount})",
                        enabled = shuffleJokerCount > 0 && !isLevelCompleted,
                        activeHighlight = false,
                        color = Color(0xFF7C3AED),
                        onClick = { useJoker(JokerType.SHUFFLE) },
                        modifier = Modifier.weight(1f)
                    )
                    JokerActionButton(
                        title = "⚡ ÇİFTE HAMLE",
                        subtitle = if (isDoubleScoreActive) "AKTİF (2x)" else "-5 Puan (${doubleScoreJokerCount})",
                        enabled = doubleScoreJokerCount > 0 && !isDoubleScoreActive && !isLevelCompleted,
                        activeHighlight = isDoubleScoreActive,
                        color = Color(0xFFD97706),
                        onClick = { useJoker(JokerType.DOUBLE_SCORE) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🛡️ Altın Kurtarma Sistemi Aktif: Jokerler bitse bile hiçbir zaman çözümsüz tabloda kalmazsın!",
                    color = Color(0xFFFBBF24),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Yıldız yağmuru / Patlama / Konfeti katmanı
        StarRainAndBurstOverlay(
            triggerKey = burstTriggerKey,
            isConfettiMode = isLevelCompleted
        )

        // Yeni bölüm kapı açılma animasyonu
        DoorOpeningOverlay(
            levelId = levelSpec.levelId,
            themeTitle = theme.title,
            levelTitle = levelSpec.title,
            primaryColor = Color(theme.primaryColorHex)
        )

        // Süre Dolduysa Nazik Tekrar Deneme Diyaloğu (Ceza yok!)
        if (isTimeExpiredPrompt && !isLevelCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B))
                        .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🦊⏰", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Süre Doldu Ama Puan Kaybı Yok!",
                        color = Color(0xFFFBBF24),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Öğrenirken acele etmek zorunda değilsin! Süreyi sıfırlayarak aynı yerden devam edebilir veya süre sınırı olmayan Rahat Moda geçebilirsin.",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                remainingSeconds = initialSeconds ?: 60
                                isTimeExpiredPrompt = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("⏱️ Süreyi Yenile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                isRelaxedModeLocal = true
                                remainingSeconds = null
                                isTimeExpiredPrompt = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("🌿 Rahat Modda Devam", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Bölüm Tamamlandı Kutlama ve Ödül Ekranı (Bütün bloklar temizlendi!)
        if (isLevelCompleted) {
            val allLevels = CurriculumRepository.allLevels
            val currentIdx = allLevels.indexOfFirst { it.levelId == levelSpec.levelId }
            val nextLevel = if (currentIdx in 0 until allLevels.lastIndex) allLevels[currentIdx + 1] else null

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0F172A))
                        .border(2.5.dp, Color(0xFFFBBF24), RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎉🏆💎", fontSize = 44.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "BÖLÜM TAMAMLANDI!",
                        color = Color(0xFFFBBF24),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tablodaki bütün sayı bloklarını eksiksiz temizledin!",
                        color = Color(0xFF34D399),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Kazanılan Ödül: +20 🪙 Altın • +2 💎 Elmas\nGünlük Puanın: ${stats.dailyScore} / 100",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { boardGenerationSeed += 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("🔄 Tekrar Oyna", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (nextLevel != null) {
                            Button(
                                onClick = { onNextLevelRequested(nextLevel) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Sonraki Bölüm ➔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = onExitToHub,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                            ) {
                                Text("🏅 Haritaya Dön", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleBoardGrid(
    gridSize: Int,
    cells: List<BlockCell>,
    selectedCoords: List<Pair<Int, Int>>,
    themePrimaryColor: Color,
    onCellClick: (BlockCell) -> Unit
) {
    val cellByCoord = remember(cells) {
        cells.associateBy { Pair(it.row, it.col) }
    }

    val fontSizeSp = when {
        gridSize <= 4 -> 22.sp
        gridSize == 5 -> 19.sp
        gridSize == 6 -> 17.sp
        gridSize == 8 -> 13.sp
        else -> 11.sp // 10x10
    }

    val cellPadding = when {
        gridSize <= 5 -> 4.dp
        gridSize == 6 -> 3.dp
        gridSize == 8 -> 2.dp
        else -> 1.5.dp
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
            .border(2.dp, themePrimaryColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (r in 0 until gridSize) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (c in 0 until gridSize) {
                        val cell = cellByCoord[Pair(r, c)]
                        val coord = Pair(r, c)
                        val selectionOrder = selectedCoords.indexOf(coord)
                        val isSelected = selectionOrder != -1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(cellPadding)
                        ) {
                            if (cell != null) {
                                NumberBlockTile(
                                    cell = cell,
                                    isSelected = isSelected,
                                    selectionOrder = selectionOrder,
                                    fontSizeSp = fontSizeSp,
                                    themePrimaryColor = themePrimaryColor,
                                    onClick = { onCellClick(cell) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberBlockTile(
    cell: BlockCell,
    isSelected: Boolean,
    selectionOrder: Int,
    fontSizeSp: androidx.compose.ui.unit.TextUnit,
    themePrimaryColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = when {
            cell.isCleared -> 0f
            isSelected -> 1.08f
            cell.isHinted -> 1.05f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 240),
        label = "tile_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (cell.isCleared) 0f else 1f,
        animationSpec = tween(durationMillis = 240),
        label = "tile_alpha"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            cell.isCleared -> Color.Transparent
            isSelected -> Color(0xFFF59E0B)
            cell.isHinted -> Color(0xFF06B6D4)
            else -> Color(0xFF1E293B)
        },
        animationSpec = tween(durationMillis = 180),
        label = "tile_bg"
    )

    val borderColor = when {
        cell.isCleared -> Color.Transparent
        isSelected -> Color.White
        cell.isHinted -> Color(0xFFFEF08A)
        else -> themePrimaryColor.copy(alpha = 0.65f)
    }

    if (cell.isCleared && alpha <= 0.05f) {
        // Temizlenmiş boş hücre izi (doğrusal görüş hattının açık olduğunu görsel olarak belli eder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                .clickable(enabled = !cell.isCleared) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${cell.value}",
                color = Color.White,
                fontSize = fontSizeSp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            if (isSelected) {
                Text(
                    text = "${selectionOrder + 1}",
                    color = Color(0xFFFEF08A),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 3.dp, top = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun JokerActionButton(
    title: String,
    subtitle: String,
    enabled: Boolean,
    activeHighlight: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        activeHighlight -> Color(0xFFF59E0B)
        enabled -> color
        else -> Color(0xFF334155).copy(alpha = 0.5f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
                1.dp,
                if (activeHighlight) Color.White else Color.White.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (enabled || activeHighlight) Color.White else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = subtitle,
            color = if (enabled || activeHighlight) Color(0xFFFEF08A) else Color(0xFF64748B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
