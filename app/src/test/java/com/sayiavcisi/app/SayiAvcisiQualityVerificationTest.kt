package com.sayiavcisi.app

import com.sayiavcisi.app.engine.BoardGeneratorAndSolver
import com.sayiavcisi.app.engine.CurriculumRepository
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
}
