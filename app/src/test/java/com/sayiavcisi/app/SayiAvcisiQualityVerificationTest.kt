package com.sayiavcisi.app

import com.sayiavcisi.app.data.GameResumeSnapshot
import com.sayiavcisi.app.data.GameSnapshotCodec
import com.sayiavcisi.app.engine.BoardFactory
import com.sayiavcisi.app.engine.BoardGeneratorAndSolver
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GoldenRescuePolicy
import com.sayiavcisi.app.engine.MatchingValidator
import com.sayiavcisi.app.engine.ScoreEngine
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.DifficultyTier
import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationType
import com.sayiavcisi.app.model.QuestionFormat
import com.sayiavcisi.app.model.TargetStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Bölüm 16 Kalite Kontrol ve Otomatik Matematiksel Doğrulama Test Paketi:
 * 1. Bütün işlem seviyelerinin matematiksel doğruluğunu test eder.
 * 2. Sıfırın kullanıldığı işlemleri (0+1=1, 0+5=5, 0+0=0, 5-5=0, 0×5=0, 0÷2=0) test eder.
 * 3. Sıfıra bölme olmadığını doğrular.
 * 4. Bütün oyun tahtalarının (4×4, 5×5, 6×6, 8×8, 10×10) çözülebilirliğini doğrular.
 * 5. Hiçbir bölümde eşleşmeyen blok kalmadığını doğrular.
 * 6. Üç jokerin (İpucu -5, Yeniden Düzenle -10, Çifte Hamle -5) doğru çalıştığını test eder.
 * 7. Jokerler bitince ALTIN KURTARMA sistemini test eder.
 * 8. Günlük puanın 100'ü geçmediğini ve 0'ın altına düşmediğini test eder.
 * 9. Puan tablosunun kesin değerlerini (+2/+3, seri +5/+10, çifte hamle, yanlış -1, bölüm bonusları) doğrular.
 * 10. ALTIN KURTARMA'nın yalnızca jokerler tükenince (veya çocuk gerçekten takılınca) devreye girdiğini doğrular.
 * 11. Tahta üretiminin tohuma göre deterministik ve çökmesiz olduğunu doğrular.
 * 12. Yarım kalan oyun kaydının (kaldığı yerden devam) bozulmadan saklanıp geri yüklendiğini test eder.
 * 13. Çocuk çözüm sırasını oynadığında tahtanın tamamının temizlendiğini test eder.
 * 14. Eksik çarpan/bölen/bölünen formatlarının kullanıldığını ve matematiksel olarak geçerli olduğunu doğrular.
 * 15. 5 gün x 6 aşama müfredat yapısını ve 4×4 -> 10×10 tahta büyümesini doğrular.
 */
class SayiAvcisiQualityVerificationTest {

    @Test
    fun test01_AllCurriculumLevelsAndCustomRangesAreMathematicallyValidAndSolvable() {
        val random = Random(2026)

        // 5 günlük müfredattaki tüm 30 seviye için çoklu tohumlarla (seed) üretim ve tam çözüm doğrulaması
        for (level in CurriculumRepository.allLevels) {
            repeat(3) {
                val board = BoardGeneratorAndSolver.generateVerifiedBoard(level, random)
                assertEquals(level.difficultyTier.gridSize, board.gridSize)
                assertEquals(board.gridSize * board.gridSize, board.cells.size)

                // Çözüm doğrulama algoritması oyuna başlamadan çalışmalı ve %100 geçmeli
                val isSolvable = BoardGeneratorAndSolver.verifyBoardSolvability(board)
                assertTrue("Seviye çözülebilir olmalı: ${level.levelId}", isSolvable)

                // Toplam adım büyüklükleri tahtadaki tüm hücre sayısına tam eşit olmalı (eşleşmeyen blok kalmamalı)
                val totalClearedInSolution = board.verifiedSolution.sumOf { it.cellCoords.size }
                assertEquals(
                    "Bölüm sonunda eşleşmeyen blok kalmamalı: ${level.levelId}",
                    board.cells.size,
                    totalClearedInSolution
                )
            }
        }

        // Özel Toplama Hedef Basamakları (1..10, 11-15, 16-20, 21-30, 31-50, 51-100)
        val additionRanges = listOf(
            1 to 1, 2 to 2, 3 to 3, 4 to 4, 5 to 5,
            6 to 6, 7 to 7, 8 to 8, 9 to 9, 10 to 10,
            11 to 15, 16 to 20, 21 to 30, 31 to 50, 51 to 100
        )
        for ((minT, maxT) in additionRanges) {
            val customSpec = CurriculumRepository.createCustomAdditionLevel(
                minTarget = minT,
                maxTarget = maxT,
                operandCount = if (maxT >= 3) 3 else 2,
                tier = DifficultyTier.MEDIUM
            )
            val board = BoardGeneratorAndSolver.generateVerifiedBoard(customSpec, random)
            assertTrue(BoardGeneratorAndSolver.verifyBoardSolvability(board))
        }
    }

    @Test
    fun test02_ZeroOperationsAreCorrectlyHandled() {
        // 0+1=1, 0+5=5, 0+0=0
        val addZeroStep = TargetStep(
            operation = OperationType.ADDITION,
            targetResult = 5,
            operandCount = 2,
            promptDisplay = "HEDEF: 5",
            helperSubtext = "",
            expectedOrderedValues = listOf(0, 5)
        )
        assertTrue(MatchingValidator.evaluateMathMatch(listOf(0, 5), addZeroStep).isValid)
        assertTrue(MatchingValidator.evaluateMathMatch(listOf(5, 0), addZeroStep).isValid)

        val addZeroZeroStep = TargetStep(
            operation = OperationType.ADDITION,
            targetResult = 0,
            operandCount = 2,
            promptDisplay = "HEDEF: 0",
            helperSubtext = "",
            expectedOrderedValues = listOf(0, 0)
        )
        assertTrue(MatchingValidator.evaluateMathMatch(listOf(0, 0), addZeroZeroStep).isValid)

        // 5-5=0
        val subZeroStep = TargetStep(
            operation = OperationType.SUBTRACTION,
            targetResult = 0,
            operandCount = 2,
            promptDisplay = "HEDEF: 0",
            helperSubtext = "",
            expectedOrderedValues = listOf(5, 5)
        )
        assertTrue(MatchingValidator.evaluateMathMatch(listOf(5, 5), subZeroStep).isValid)

        // 0×4=0
        val mulZeroStep = TargetStep(
            operation = OperationType.MULTIPLICATION,
            targetResult = 0,
            operandCount = 2,
            promptDisplay = "HEDEF: 0",
            helperSubtext = "",
            expectedOrderedValues = listOf(0, 4)
        )
        assertTrue(MatchingValidator.evaluateMathMatch(listOf(0, 4), mulZeroStep).isValid)

        // 0÷2=0
        val divZeroStep = TargetStep(
            operation = OperationType.DIVISION,
            targetResult = 0,
            operandCount = 2,
            promptDisplay = "HEDEF: 0",
            helperSubtext = "",
            expectedOrderedValues = listOf(0, 2)
        )
        assertTrue(MatchingValidator.evaluateMathMatch(listOf(0, 2), divZeroStep).isValid)
    }

    @Test
    fun test03_DivisionByZeroIsStrictlyPrevented() {
        val random = Random(99)
        val divisionLevels = CurriculumRepository.allLevels.filter {
            it.operation == OperationType.DIVISION || it.operation == OperationType.MIXED
        }

        for (level in divisionLevels) {
            repeat(10) {
                val board = BoardGeneratorAndSolver.generateVerifiedBoard(level, random)
                for (step in board.targets) {
                    if (step.operation == OperationType.DIVISION) {
                        val divisor = step.expectedOrderedValues[1]
                        assertTrue("Bölen hiçbir zaman 0 olmamalı!", divisor != 0)
                    }
                }
            }
        }

        // Kullanıcı tahtada ikinci sayı olarak 0 seçerse kesinlikle reddedilmeli
        val step = TargetStep(
            operation = OperationType.DIVISION,
            targetResult = 0,
            operandCount = 2,
            promptDisplay = "HEDEF: 0",
            helperSubtext = "",
            expectedOrderedValues = listOf(0, 2)
        )
        val evalDivByZero = MatchingValidator.evaluateMathMatch(listOf(6, 0), step)
        assertFalse("Sıfıra bölme geçersiz sayılmalı!", evalDivByZero.isValid)
    }

    @Test
    fun test04_DirectionalLineOfSightBlocksOccupiedIntermediateCells() {
        // Aralarında başka aktif sayı bulunan iki sayının eşleşmesine izin verilmemeli!
        val activeCoords = setOf(
            Pair(0, 0),
            Pair(0, 1), // Aradaki engelleyici aktif blok!
            Pair(0, 2)
        )
        val (blockedOk, _) = MatchingValidator.isGeometricallyValidSelection(
            selectedCoords = listOf(Pair(0, 0), Pair(0, 2)),
            activeCoords = activeCoords,
            mode = MatchRuleMode.DIRECTIONAL
        )
        assertFalse("Aralarında aktif sayı varken eşleşmemeli!", blockedOk)

        // Aradaki (0,1) bloğu temizlendiğinde (0,0) ve (0,2) birbirine bitişik olmasa bile eşleşebilmeli!
        val activeAfterClear = setOf(
            Pair(0, 0),
            Pair(0, 2)
        )
        val (unblockedOk, _) = MatchingValidator.isGeometricallyValidSelection(
            selectedCoords = listOf(Pair(0, 0), Pair(0, 2)),
            activeCoords = activeAfterClear,
            mode = MatchRuleMode.DIRECTIONAL
        )
        assertTrue("Aradaki blok kalkınca aynı doğrultudaki iki sayı eşleşebilmeli!", unblockedOk)

        // Ters yönde okuma: (0,2) -> (0,0)
        val (reverseOk, _) = MatchingValidator.isGeometricallyValidSelection(
            selectedCoords = listOf(Pair(0, 2), Pair(0, 0)),
            activeCoords = activeAfterClear,
            mode = MatchRuleMode.DIRECTIONAL
        )
        assertTrue("Ters yönde okuma geçerli olmalı!", reverseOk)

        // Serbest eşleştirme modunda farklı satır ve sütunlar eşleşebilmeli
        val freeSet = setOf(Pair(0, 0), Pair(2, 3))
        val (freeOk, _) = MatchingValidator.isGeometricallyValidSelection(
            selectedCoords = listOf(Pair(0, 0), Pair(2, 3)),
            activeCoords = freeSet,
            mode = MatchRuleMode.FREE
        )
        assertTrue("Serbest eşleştirme modunda farklı satır/sütun eşleşebilmeli!", freeOk)
    }

    @Test
    fun test05_SubtractionRequiresLargerMinusSmallerAndNoNegativeResults() {
        val subStep = TargetStep(
            operation = OperationType.SUBTRACTION,
            targetResult = 5,
            operandCount = 2,
            promptDisplay = "HEDEF: 5",
            helperSubtext = "",
            expectedOrderedValues = listOf(9, 4)
        )
        // Büyükten küçük çıkarıldığında doğru (9 - 4 = 5)
        assertTrue(MatchingValidator.evaluateMathMatch(listOf(9, 4), subStep).isValid)
        // Küçükten büyük çıkarılmaya çalışıldığında yanlış (4 - 9 = -5)
        assertFalse(MatchingValidator.evaluateMathMatch(listOf(4, 9), subStep).isValid)
    }

    @Test
    fun test06_ThreeJokersAndGoldenRescueWorkProperly() {
        // Joker puan düşüşleri: İpucu -5, Yeniden Düzenle -10, Çifte Hamle -5, Puan >= 0
        val (afterHint, hintDelta) = ScoreEngine.onJokerUsed(50, JokerType.HINT)
        assertEquals(45, afterHint)
        assertEquals(-5, hintDelta)

        val (afterShuffle, shuffleDelta) = ScoreEngine.onJokerUsed(afterHint, JokerType.SHUFFLE)
        assertEquals(35, afterShuffle)
        assertEquals(-10, shuffleDelta)

        val (afterDouble, doubleDelta) = ScoreEngine.onJokerUsed(afterShuffle, JokerType.DOUBLE_SCORE)
        assertEquals(30, afterDouble)
        assertEquals(-5, doubleDelta)

        // Puan sıfırın altına düşmemeli
        val (clampedZero, _) = ScoreEngine.onJokerUsed(3, JokerType.SHUFFLE)
        assertEquals(0, clampedZero)

        // ALTIN KURTARMA Testi: Kasıtlı olarak kilitlenmiş (çözümsüz) bir tahta durumu oluştur
        val lockedCells = listOf(
            BlockCell(id = 0, row = 0, col = 0, value = 1, isCleared = false),
            BlockCell(id = 1, row = 0, col = 1, value = 2, isCleared = false),
            BlockCell(id = 2, row = 1, col = 0, value = 0, isCleared = true),
            BlockCell(id = 3, row = 1, col = 1, value = 0, isCleared = true)
        )
        val neededTarget = TargetStep(
            operation = OperationType.ADDITION,
            targetResult = 10,
            operandCount = 2,
            promptDisplay = "HEDEF: 10",
            helperSubtext = "",
            expectedOrderedValues = listOf(6, 4)
        )
        // Kurtarma öncesi geçerli hamle yok (1 + 2 = 3 != 10)
        assertTrue(
            MatchingValidator.findAllValidMovesForStep(lockedCells, neededTarget, MatchRuleMode.DIRECTIONAL).isEmpty()
        )

        // ALTIN KURTARMA devreye girer
        val rescueOutcome = BoardGeneratorAndSolver.triggerGoldenRescue(
            currentCells = lockedCells,
            remainingTargets = listOf(neededTarget),
            mode = MatchRuleMode.DIRECTIONAL,
            gridSize = 2
        )
        assertTrue(rescueOutcome.wasTriggered)

        // Kurtarma sonrasında tahta tekrar çözülebilir olmalı!
        val movesAfterRescue = MatchingValidator.findAllValidMovesForStep(
            cells = rescueOutcome.updatedCells,
            step = neededTarget,
            mode = rescueOutcome.effectiveMode
        )
        assertFalse("Altın Kurtarma sonrasında en az bir geçerli çözüm bulunmalı!", movesAfterRescue.isEmpty())
    }

    @Test
    fun test07_DailyScoreNeverExceeds100AndNeverDropsBelow0() {
        var score = 0
        var streak = 0

        // 100 işlemlik seri yapılsa bile günlük puan kesinlikle 100'ü geçmemeli
        repeat(80) {
            val res = ScoreEngine.onCorrectMatch(
                currentDailyScore = score,
                currentStreak = streak,
                operandCount = 3,
                isDoubleScoreActive = true
            )
            score = res.newDailyScore
            streak = res.newStreak
            assertTrue("Günlük puan 100'ü geçmemeli!", score <= 100)
        }
        assertEquals(100, score)

        // Bölüm sonu bonusları eklendiğinde de 100'ü geçmemeli
        val (afterLevelBonus, _) = ScoreEngine.onLevelCompleted(
            currentDailyScore = score,
            hadZeroMistakes = true,
            finishedWithinTimeLimit = true,
            completedFullDayQuest = true
        )
        assertEquals(100, afterLevelBonus)

        // Arka arkaya yanlış eşleşmelerde puan 0'ın altına düşmemeli
        var lowScore = 2
        repeat(10) {
            val wrongRes = ScoreEngine.onWrongMatch(lowScore)
            lowScore = wrongRes.newDailyScore
            assertTrue("Puan 0'ın altına düşmemeli!", lowScore >= 0)
        }
        assertEquals(0, lowScore)
    }

    @Test
    fun test08_ExactScoreTableValues() {
        // Doğru iki sayılı eşleşme: +2 | Doğru üç sayılı eşleşme: +3
        assertEquals(2, ScoreEngine.onCorrectMatch(0, 0, 2, false).appliedDelta)
        assertEquals(3, ScoreEngine.onCorrectMatch(0, 0, 3, false).appliedDelta)

        // Çifte Hamle bir sonraki doğru eşleşmeyi ikiye katlar
        assertEquals(4, ScoreEngine.onCorrectMatch(0, 0, 2, true).appliedDelta)
        assertEquals(6, ScoreEngine.onCorrectMatch(0, 0, 3, true).appliedDelta)

        // Arka arkaya 5 doğru: +5 | Arka arkaya 10 doğru: +10
        val fifth = ScoreEngine.onCorrectMatch(0, currentStreak = 4, operandCount = 2, isDoubleScoreActive = false)
        assertEquals(5, fifth.newStreak)
        assertEquals(2 + ScoreEngine.BONUS_STREAK_5, fifth.appliedDelta)

        val tenth = ScoreEngine.onCorrectMatch(0, currentStreak = 9, operandCount = 2, isDoubleScoreActive = false)
        assertEquals(10, tenth.newStreak)
        assertEquals(2 + ScoreEngine.BONUS_STREAK_10, tenth.appliedDelta)

        // Yanlış eşleşme: -1 puan ve seri sıfırlanır
        val wrong = ScoreEngine.onWrongMatch(30)
        assertEquals(-1, wrong.appliedDelta)
        assertEquals(0, wrong.newStreak)

        // Bölüm sonu bonusları: hatasız +5, süre +3, gün görevi +10
        assertEquals(18, ScoreEngine.onLevelCompleted(0, true, true, true).second)
        assertEquals(0, ScoreEngine.onLevelCompleted(0, false, false, false).second)

        // Süre limiti olmayan (Başlangıç/Kolay/Rahat) seviyelerde süre bonusu kazanılmaz, yalnızca +5 hatasız bonusu
        assertEquals(ScoreEngine.BONUS_FLAWLESS_LEVEL, ScoreEngine.onLevelCompleted(0, true, false, false).second)
    }

    @Test
    fun test09_GoldenRescueOnlyTriggersWhenJokersAreExhaustedOrChildIsStranded() {
        // Geçerli hamle varken hiçbir şey yapılmaz
        assertEquals(
            GoldenRescuePolicy.Decision.NOTHING_TO_DO,
            GoldenRescuePolicy.decide(hasValidMove = true, targetsRemaining = 5, jokersRemaining = 0, secondsSinceStuck = 0)
        )

        // Hamle yok ama jokerler duruyor: çocuk önce joker kullanmaya yönlendirilir (puan ekonomisi korunur)
        assertEquals(
            GoldenRescuePolicy.Decision.GUIDE_TO_JOKER,
            GoldenRescuePolicy.decide(hasValidMove = false, targetsRemaining = 5, jokersRemaining = 3, secondsSinceStuck = 4)
        )

        // Üç joker de tükendiyse ALTIN KURTARMA anında ve sessizce devreye girer
        assertEquals(
            GoldenRescuePolicy.Decision.USE_GOLDEN_RESCUE_NOW,
            GoldenRescuePolicy.decide(hasValidMove = false, targetsRemaining = 5, jokersRemaining = 0, secondsSinceStuck = 0)
        )

        // Joker var ama çocuk uzun süre takıldıysa yine mahsur bırakılmaz
        assertEquals(
            GoldenRescuePolicy.Decision.USE_GOLDEN_RESCUE_NOW,
            GoldenRescuePolicy.decide(
                hasValidMove = false,
                targetsRemaining = 5,
                jokersRemaining = 2,
                secondsSinceStuck = GoldenRescuePolicy.GRACE_SECONDS_WITH_JOKER
            )
        )

        assertEquals(3, GoldenRescuePolicy.jokersRemaining(hint = 1, shuffle = 1, doubleScore = 1))
        assertEquals(0, GoldenRescuePolicy.jokersRemaining(hint = 0, shuffle = 0, doubleScore = 0))
    }

    @Test
    fun test10_BoardFactoryIsDeterministicAndNeverCrashes() {
        val spec = CurriculumRepository.getLevelById("D1_S1")

        // Aynı tohum -> aynı tahta (yarım kalan oyunun geri yüklenebilmesi için zorunlu)
        val first = BoardFactory.createReliably(spec, seed = 4242)
        val second = BoardFactory.createReliably(spec, seed = 4242)
        assertEquals(first.seedUsed, second.seedUsed)
        assertEquals(first.board.cells, second.board.cells)
        assertEquals(first.board.targets.map { it.promptDisplay }, second.board.targets.map { it.promptDisplay })

        // Farklı tohumlar farklı tahtalar üretmeli (rastgele görünümlü ama garantili)
        val third = BoardFactory.createReliably(spec, seed = 9911)
        assertNotEquals(first.board.cells, third.board.cells)

        // Hiçbir seviye oyuncuya çözülemez/ üretilemez tahta yüzünden çökme yaşatmamalı
        for (level in CurriculumRepository.allLevels) {
            val prepared = BoardFactory.createReliably(level, seed = level.levelId.hashCode() and 0x3FFF)
            assertTrue(BoardGeneratorAndSolver.verifyBoardSolvability(prepared.board))
        }
    }

    @Test
    fun test11_ResumedGameSnapshotRoundTripAndCorruption() {
        val spec = CurriculumRepository.getLevelById("D2_S3")
        val board = BoardFactory.createReliably(spec, seed = 777).board
        val partiallyCleared = board.cells.mapIndexed { i, cell ->
            cell.copy(isCleared = i < 6, isHinted = i == 7)
        }
        val snapshot = GameResumeSnapshot(
            levelId = spec.levelId,
            boardSeed = 777,
            gridSize = board.gridSize,
            matchRuleMode = board.matchRuleMode,
            remainingTargetCount = board.targets.size - 3,
            hintJokers = 0,
            shuffleJokers = 1,
            doubleScoreJokers = 0,
            streak = 4,
            mistakes = 2,
            cells = partiallyCleared
        )

        val decoded = GameSnapshotCodec.decode(
            raw = GameSnapshotCodec.encode(snapshot),
            expectedLevelId = spec.levelId,
            expectedCellCount = board.cells.size
        )

        assertNotNull(decoded)
        assertEquals(spec.levelId, decoded!!.levelId)
        assertEquals(777, decoded.boardSeed)
        assertEquals(board.matchRuleMode, decoded.matchRuleMode)
        assertEquals(snapshot.remainingTargetCount, decoded.remainingTargetCount)
        assertEquals(snapshot.streak, decoded.streak)
        assertEquals(snapshot.mistakes, decoded.mistakes)
        assertEquals(partiallyCleared.map { Triple(it.row, it.col, it.value) }, decoded.cells.map { Triple(it.row, it.col, it.value) })
        assertEquals(partiallyCleared.count { it.isCleared }, decoded.cells.count { it.isCleared })
        assertFalse(decoded.isFinished)

        // Bozuk/yanlış seviyeye ait kayıtlar yok sayılmalı
        assertNull(GameSnapshotCodec.decode("v1|D9_S9|1|5|0|2|1,1,1,0,0|x", spec.levelId, board.cells.size))
        assertNull(GameSnapshotCodec.decode("bozuk metin", spec.levelId, board.cells.size))
        assertNull(GameSnapshotCodec.decode(null, spec.levelId, board.cells.size))

        // Bitmiş kayır geri yüklenmez
        assertTrue(decoded.copy(remainingTargetCount = 0).isFinished)
    }

    @Test
    fun test12_PlayerCanFinishEveryBoardWithoutLeftoverBlocks() {
        // Çocuk doğrulanmış çözüm sırasını adım adım oynadığında:
        // her adımda tahtada en az bir geçerli hamle bulunmalı ve bölüm sonunda tek blok kalmamalı.
        val random = Random(5150)
        val levelsToPlay = CurriculumRepository.allLevels.filterIndexed { index, _ -> index % 3 == 0 }

        for (level in levelsToPlay) {
            val board = BoardGeneratorAndSolver.generateVerifiedBoard(level, random)
            var cells = board.cells

            for ((index, step) in board.targets.withIndex()) {
                val moves = MatchingValidator.findAllValidMovesForStep(cells, step, board.matchRuleMode)
                assertTrue("Her hedef için en az bir geçerli hamle olmalı: ${level.levelId}", moves.isNotEmpty())

                val planned = board.verifiedSolution[index].cellCoords.toSet()
                assertTrue(
                    "Doğrulanmış çözüm hamlesi geçerli hamleler arasında olmalı: ${level.levelId}",
                    moves.any { it.toSet() == planned }
                )

                cells = cells.map { if (planned.contains(Pair(it.row, it.col))) it.copy(isCleared = true) else it }
            }

            assertTrue("Bölüm sonunda eşleşmeyen tek blok kalmamalı: ${level.levelId}", cells.all { it.isCleared })
        }
    }

    @Test
    fun test13_MissingFactorAndDivisorFormatsAreUsedAndMathematicallyValid() {
        val formats = CurriculumRepository.allLevels.map { it.questionFormat }.toSet()
        assertTrue(formats.contains(QuestionFormat.MISSING_FACTOR))
        assertTrue(formats.contains(QuestionFormat.MISSING_DIVISOR))
        assertTrue(formats.contains(QuestionFormat.MISSING_DIVIDEND))

        val missingFactorLevels = CurriculumRepository.allLevels.filter {
            it.questionFormat in setOf(QuestionFormat.MISSING_FACTOR, QuestionFormat.MISSING_DIVISOR, QuestionFormat.MISSING_DIVIDEND)
        }
        val random = Random(31337)
        for (level in missingFactorLevels) {
            val board = BoardGeneratorAndSolver.generateVerifiedBoard(level, random)
            for (step in board.targets) {
                assertTrue(MatchingValidator.evaluateMathMatch(step.expectedOrderedValues, step).isValid)
                if (step.operation == OperationType.DIVISION) {
                    assertTrue("Eksik bölen sorusunda bile bölen 0 olamaz", step.expectedOrderedValues[1] != 0)
                }
            }
        }
    }

    @Test
    fun test14_EveryDayHasSixStagesAndFiveDayCurriculum() {
        // Bölüm 12: 5 gün x 6 aşama (ısınma, 1. ana bulmaca, mola, 2. ana bulmaca, karışık, değerlendirme)
        for (day in 1..5) {
            val stages = CurriculumRepository.getLevelsForDay(day)
            assertEquals("Her gün 6 aşamalı olmalı (gün $day)", 6, stages.size)
            assertEquals(
                "Aşamalar 1..6 sıralı olmalı (gün $day)",
                (1..6).toList(),
                stages.map { it.stageIndex }
            )
        }
        assertEquals(30, CurriculumRepository.allLevels.size)

        // Bölüm 7: tahta boyutları 4x4 -> 10x10 aralığında ve seviyeye göre büyüyor
        val gridSizes = CurriculumRepository.allLevels.map { it.difficultyTier.gridSize }.distinct().sorted()
        assertEquals(listOf(4, 5, 6, 8, 10), gridSizes)
    }
}
