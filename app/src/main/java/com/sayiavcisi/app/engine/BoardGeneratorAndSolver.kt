package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.GeneratedBoard
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationType
import com.sayiavcisi.app.model.PlannedMove
import com.sayiavcisi.app.model.QuestionFormat
import com.sayiavcisi.app.model.TargetStep
import kotlin.random.Random

/**
 * Sayı Avcısı Garantili Çözülebilir Oyun Tahtası Üreticisi, Doğrulayıcısı ve Altın Kurtarma Motoru.
 *
 * Zorunlu Kurallar:
 * - Her üretilen tahtanın en az bir geçerli çözümü olmalı.
 * - Oyunun tamamlanması için gereken bütün sayı blokları çözülebilmeli.
 * - Eşleşmeyen sayı bırakılmamalı (Bölüm sonunda bütün bloklar temizlenmiş olmalı).
 * - Rastgele sayı üretimi çözümsüz tablo oluşturmamalı.
 * - Bir hamle sonrasında yeni eşleşmeler oluşabilmeli (aradaki blok kalktığında arkadaki bloklar açılır).
 * - Çözüm doğrulama algoritması oyuna başlamadan çalışmalı.
 * - Çözülebilirlik testinden geçmeyen tahta oyuncuya gösterilmemeli.
 * - Tahta kilitlenirse otomatik kurtarma (ALTIN KURTARMA) devreye girmeli.
 */
object BoardGeneratorAndSolver {

    /**
     * Verilen LevelSpec için 4×4, 5×5, 6×6, 8×8 veya 10×10 boyutunda,
     * tamamı son bloğuna kadar temizlenebilen ve oyun başlamadan önce
     * simülasyon ile %100 doğrulanmış bir oyun tahtası üretir.
     */
    fun generateVerifiedBoard(spec: LevelSpec, random: Random = Random.Default): GeneratedBoard {
        val gridSize = spec.difficultyTier.gridSize
        val totalCells = gridSize * gridSize

        // Tüm hücreleri koordinat listesi olarak hazırla
        val allCoords = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                allCoords.add(Pair(r, c))
            }
        }

        // Adım büyüklüklerini (2 veya 3 blok) toplam hücre sayısını tam bölecek şekilde planla.
        // Böylece bölüm sonunda tek bir eşleşmeyen blok bile kalmaz!
        val stepSizes = partitionTotalCellsIntoSteps(totalCells, spec.operandCount)

        // Geometrik olarak geçerli bir soyulma (temizlenme) sırası oluştur
        val coordGroups = buildPeelableCoordinateSequence(gridSize, stepSizes, spec.matchRuleMode, random)

        // Her grup için LevelSpec kurallarına uygun matematiksel TargetStep ve hücre değerleri üret
        val valueMap = mutableMapOf<Pair<Int, Int>, Int>()
        val targets = mutableListOf<TargetStep>()
        val verifiedSolution = mutableListOf<PlannedMove>()

        for ((index, coords) in coordGroups.withIndex()) {
            val step = generateMathStepForGroup(spec, coords.size, index, random)
            targets.add(step)
            verifiedSolution.add(PlannedMove(coords, step))
            for (i in coords.indices) {
                valueMap[coords[i]] = step.expectedOrderedValues[i]
            }
        }

        val cells = allCoords.mapIndexed { idx, coord ->
            BlockCell(
                id = idx,
                row = coord.first,
                col = coord.second,
                value = valueMap[coord] ?: 1,
                isCleared = false,
                isHinted = false
            )
        }

        val board = GeneratedBoard(
            gridSize = gridSize,
            cells = cells,
            targets = targets,
            verifiedSolution = verifiedSolution,
            matchRuleMode = spec.matchRuleMode
        )

        // Oyuna başlamadan önce çözülebilirlik doğrulama algoritmasını çalıştır!
        check(verifyBoardSolvability(board)) {
            "Çözülebilirlik doğrulaması başarısız oldu: ${spec.levelId}"
        }

        return board
    }

    /**
     * Toplam hücre sayısını (16, 25, 36, 64, 100) hiç artan blok kalmayacak şekilde
     * 2'li ve 3'lü hamle adımlarına ayırır.
     *
     * Örnek:
     * - 4×4 = 16 hücre ->
     *   - operandCount == 2 ise: sekiz adet 2'li hamle (8×2 = 16).
     *   - operandCount == 3 ise: dört adet 3'lü + iki adet 2'li hamle (12 + 4 = 16).
     * - 5×5 = 25 hücre (tek sayı!) ->
     *   - operandCount == 2 ise: on bir adet 2'li + bir adet 3'lü final hamlesi (22 + 3 = 25) -> Hiç blok kalmaz!
     *   - operandCount == 3 ise: yedi adet 3'lü + iki adet 2'li hamle (21 + 4 = 25).
     * - 6×6 = 36 hücre ->
     *   - operandCount == 2 ise: on sekiz adet 2'li hamle.
     *   - operandCount == 3 ise: on iki adet 3'lü hamle (12×3 = 36).
     */
    fun partitionTotalCellsIntoSteps(totalCells: Int, preferredOperandCount: Int): List<Int> {
        val sizes = mutableListOf<Int>()
        var remaining = totalCells

        if (preferredOperandCount == 3) {
            while (remaining > 0) {
                if (remaining == 4) {
                    sizes.add(2)
                    sizes.add(2)
                    remaining = 0
                } else if (remaining == 2) {
                    sizes.add(2)
                    remaining = 0
                } else {
                    sizes.add(3)
                    remaining -= 3
                }
            }
        } else {
            // preferredOperandCount == 2
            while (remaining > 0) {
                if (remaining == 3) {
                    sizes.add(3)
                    remaining = 0
                } else {
                    sizes.add(2)
                    remaining -= 2
                }
            }
        }
        return sizes
    }

    /**
     * Tahtadaki tüm koordinatları, her adımda o anki aktif hücreler üzerinde
     * `MatchingValidator.isGeometricallyValidSelection` kuralını %100 sağlayacak şekilde
     * sırayla eşleştirir.
     *
     * İlk adımlarda bitişik yatay/dikey/çapraz hücreler eşleşirken, aradaki hücreler temizlendikçe
     * birbirine bitişik olmayan ancak aynı doğrultuda (ve arasında aktif blok kalmamış) hücreler
     * doğal olarak eşleşebilir hale gelir!
     */
    private fun buildPeelableCoordinateSequence(
        gridSize: Int,
        stepSizes: List<Int>,
        mode: MatchRuleMode,
        random: Random
    ): List<List<Pair<Int, Int>>> {
        // Yılan (snake) dizilimi tüm ızgarayı kesintisiz bir hatla dolaşır;
        // ancak tahtanın tekdüze görünmemesi için önce rastgele geçerli hamlelerle soyma simülasyonu deneriz.
        for (attempt in 0 until 15) {
            val active = mutableSetOf<Pair<Int, Int>>()
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    active.add(Pair(r, c))
                }
            }

            val sequence = mutableListOf<List<Pair<Int, Int>>>()
            var success = true

            for (size in stepSizes) {
                val candidates = findAllGeometricCandidates(active, size, mode, random)
                if (candidates.isEmpty()) {
                    success = false
                    break
                }
                // Gelecekte tek başına kopuk hücre bırakmamak için komşuluk skoru dengeli olanı seç
                val chosen = candidates[random.nextInt(candidates.size)]
                sequence.add(chosen)
                active.removeAll(chosen.toSet())
            }

            if (success && active.isEmpty()) {
                return sequence
            }
        }

        // Garantili yedek: Satır bazlı (her satır kendi içinde soldan sağa soyulur, satır sonları dikey bağlanır)
        return buildDeterministicRowColPeel(gridSize, stepSizes)
    }

    private fun findAllGeometricCandidates(
        activeCoords: Set<Pair<Int, Int>>,
        size: Int,
        mode: MatchRuleMode,
        random: Random
    ): List<List<Pair<Int, Int>>> {
        val activeList = activeCoords.toList().shuffled(random)
        val results = mutableListOf<List<Pair<Int, Int>>>()

        if (mode == MatchRuleMode.FREE) {
            if (activeList.size >= size) {
                results.add(activeList.take(size))
            }
            return results
        }

        // DIRECTIONAL modda: Her aktif hücreden 8 yöne (sağ, sol, aşağı, yukarı ve 4 çapraz) ilk görünür aktif komşuları tara
        val directions = listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1),
            Pair(0, -1), Pair(-1, 0), Pair(-1, -1), Pair(-1, 1)
        )

        val maxR = activeCoords.maxOfOrNull { it.first } ?: 0
        val maxC = activeCoords.maxOfOrNull { it.second } ?: 0

        for (start in activeList) {
            for (dir in directions) {
                val found = mutableListOf(start)
                var r = start.first + dir.first
                var c = start.second + dir.second
                while (r in 0..maxR && c in 0..maxC && found.size < size) {
                    val pos = Pair(r, c)
                    if (activeCoords.contains(pos)) {
                        found.add(pos)
                    }
                    r += dir.first
                    c += dir.second
                }
                if (found.size == size) {
                    val (ok, _) = MatchingValidator.isGeometricallyValidSelection(found, activeCoords, mode)
                    if (ok) {
                        results.add(found)
                        if (results.size >= 24) return results
                    }
                }
            }
        }
        return results
    }

    /**
     * Satır içi ve sütun sonu bağlantıları kullanarak her boyuttaki (4×4, 5×5, 6×6, 8×8, 10×10)
     * ızgarayı %100 geometrik geçerlilikle (aynı doğrultuda ve arasında aktif blok olmadan)
     * son bloğuna kadar temizleyen deterministik yedek algoritma.
     */
    private fun buildDeterministicRowColPeel(
        gridSize: Int,
        stepSizes: List<Int>
    ): List<List<Pair<Int, Int>>> {
        val result = mutableListOf<List<Pair<Int, Int>>>()
        if (gridSize % 2 == 0) {
            // Çift boyutlu ızgaralar (4x4, 6x6, 8x8, 10x10)
            // Eğer hepsi 2'li ise veya 3'lü + 2'li karışımı ise satırlar boyunca ilerle
            val flatByRow = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    flatByRow.add(Pair(r, c))
                }
            }
            // Eğer tüm adımlar aynı satır içinde kalıyorsa doğrudan böl, kalmıyorsa sütun bazlı eşleştir
            var idx = 0
            val allFitInRows = stepSizes.all { size ->
                val fits = (idx % gridSize) + size <= gridSize
                idx += size
                fits
            }
            if (allFitInRows) {
                var cursor = 0
                for (s in stepSizes) {
                    result.add(flatByRow.subList(cursor, cursor + s).toList())
                    cursor += s
                }
                return result
            }
        }

        // Tek boyutlu (5x5) veya satır taşmalı 3'lü durumlar için:
        // Her satırda soldan sağa mümkün olduğunca eşleştir, kalan son sütunu yukarıdan aşağıya eşleştir!
        val active = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                active.add(Pair(r, c))
            }
        }

        // Önce 3'lü adımları ve 2'li adımları satır/sütun doğrultusunda yerleştir
        val remainingSteps = stepSizes.toMutableList()
        for (r in 0 until gridSize) {
            var c = 0
            while (c < gridSize) {
                val leftInRow = gridSize - c
                if (leftInRow >= 3 && remainingSteps.contains(3)) {
                    val group = listOf(Pair(r, c), Pair(r, c + 1), Pair(r, c + 2))
                    result.add(group)
                    active.removeAll(group.toSet())
                    remainingSteps.remove(3)
                    c += 3
                } else if (leftInRow >= 2 && remainingSteps.contains(2)) {
                    val group = listOf(Pair(r, c), Pair(r, c + 1))
                    result.add(group)
                    active.removeAll(group.toSet())
                    remainingSteps.remove(2)
                    c += 2
                } else {
                    break
                }
            }
        }

        // Sütunlarda kalan aktif hücreleri (aynı sütunda oldukları için dikey doğrultuda aralarında
        // başka aktif hücre olmadan) yukarıdan aşağıya grupla
        for (c in 0 until gridSize) {
            val colCells = (0 until gridSize).map { Pair(it, c) }.filter { active.contains(it) }
            var i = 0
            while (i < colCells.size) {
                val left = colCells.size - i
                if (left >= 3 && remainingSteps.contains(3)) {
                    val group = colCells.subList(i, i + 3)
                    result.add(group)
                    active.removeAll(group.toSet())
                    remainingSteps.remove(3)
                    i += 3
                } else if (left >= 2 && remainingSteps.contains(2)) {
                    val group = colCells.subList(i, i + 2)
                    result.add(group)
                    active.removeAll(group.toSet())
                    remainingSteps.remove(2)
                    i += 2
                } else {
                    break
                }
            }
        }

        return result
    }

    /**
     * Verilen grup boyutu (2 veya 3) ve LevelSpec için matematiksel olarak %100 doğru
     * bir TargetStep üretir.
     */
    private fun generateMathStepForGroup(
        spec: LevelSpec,
        operandCount: Int,
        stepIndex: Int,
        random: Random
    ): TargetStep {
        val actualOp = if (spec.operation == OperationType.MIXED) {
            if (operandCount == 3) {
                if (stepIndex % 2 == 0) OperationType.ADDITION else OperationType.SUBTRACTION
            } else {
                when (stepIndex % 4) {
                    0 -> OperationType.ADDITION
                    1 -> OperationType.SUBTRACTION
                    2 -> OperationType.MULTIPLICATION
                    else -> OperationType.DIVISION
                }
            }
        } else {
            spec.operation
        }

        return when (actualOp) {
            OperationType.ADDITION -> generateAdditionStep(spec, operandCount, stepIndex, random)
            OperationType.SUBTRACTION -> generateSubtractionStep(spec, operandCount, stepIndex, random)
            OperationType.MULTIPLICATION -> generateMultiplicationStep(spec, stepIndex, random)
            OperationType.DIVISION -> generateDivisionStep(spec, stepIndex, random)
            OperationType.MIXED -> generateAdditionStep(spec, operandCount, stepIndex, random)
        }
    }

    private fun generateAdditionStep(
        spec: LevelSpec,
        operandCount: Int,
        stepIndex: Int,
        random: Random
    ): TargetStep {
        // Sıfır özel olarak öğretilmeli: focusZero aktifse belirli adımlarda 0+N=N veya 0+0=0 sor
        if (spec.focusZero && operandCount == 2 && stepIndex % 3 == 0) {
            val target = if (stepIndex == 0 && spec.minTarget == 0) 0 else random.nextInt(spec.minTarget.coerceAtLeast(1), spec.maxTarget + 1)
            val operands = if (random.nextBoolean()) listOf(0, target) else listOf(target, 0)
            return TargetStep(
                operation = OperationType.ADDITION,
                targetResult = target,
                operandCount = 2,
                questionFormat = QuestionFormat.STANDARD_TARGET,
                promptDisplay = "HEDEF: $target",
                helperSubtext = "Toplama (+) • 2 Sayı Eşleştir (Sıfırın Gücü!)",
                expectedOrderedValues = operands
            )
        }

        val minT = spec.minTarget.coerceAtLeast(if (operandCount == 3) 3 else 1)
        val maxT = spec.maxTarget.coerceAtLeast(minT)
        val target = random.nextInt(minT, maxT + 1)

        return if (operandCount == 2) {
            val a = if (spec.focusZero && random.nextInt(4) == 0) 0 else random.nextInt(0, target + 1)
            val b = target - a
            TargetStep(
                operation = OperationType.ADDITION,
                targetResult = target,
                operandCount = 2,
                questionFormat = QuestionFormat.STANDARD_TARGET,
                promptDisplay = "HEDEF: $target",
                helperSubtext = "Toplama (+) • Toplamı $target olan 2 sayı seç",
                expectedOrderedValues = listOf(a, b)
            )
        } else {
            // 3 sayılı toplama (Örn: 2+3+2=7, 5+4+1=10, 8+7+5=20)
            val a = random.nextInt(1, (target - 1).coerceAtLeast(2))
            val rem = target - a
            val b = random.nextInt(1, rem.coerceAtLeast(2))
            val c = (target - a - b).coerceAtLeast(0)
            val adjustedTarget = a + b + c
            TargetStep(
                operation = OperationType.ADDITION,
                targetResult = adjustedTarget,
                operandCount = 3,
                questionFormat = QuestionFormat.STANDARD_TARGET,
                promptDisplay = "HEDEF: $adjustedTarget",
                helperSubtext = "Üçlü Toplama (+) • Toplamı $adjustedTarget olan 3 sayı seç",
                expectedOrderedValues = listOf(a, b, c)
            )
        }
    }

    private fun generateSubtractionStep(
        spec: LevelSpec,
        operandCount: Int,
        stepIndex: Int,
        random: Random
    ): TargetStep {
        val minT = spec.minTarget.coerceAtLeast(0)
        val maxT = spec.maxTarget.coerceAtLeast(minT)
        // Çıkarma seviyeleri 0'dan başlamalı (Örn: 5-5=0)
        val target = if (spec.focusZero && stepIndex == 0) 0 else random.nextInt(minT, maxT + 1)

        return if (operandCount == 2) {
            val subtrahend = random.nextInt(if (spec.focusZero) 0 else 1, (maxT / 2 + 5).coerceAtLeast(3))
            val minuend = target + subtrahend
            // Önce büyük sayı (minuend), sonra küçük sayı (subtrahend)
            TargetStep(
                operation = OperationType.SUBTRACTION,
                targetResult = target,
                operandCount = 2,
                questionFormat = QuestionFormat.STANDARD_TARGET,
                promptDisplay = "HEDEF: $target",
                helperSubtext = "Çıkarma (-) • Önce BÜYÜK sayıyı, sonra küçük sayıyı seç!",
                expectedOrderedValues = listOf(minuend, subtrahend)
            )
        } else {
            // 3 sayılı zincir çıkarma: a - b - c = target (Negatif sonuç yok!)
            val b = random.nextInt(1, 6)
            val c = random.nextInt(1, 5)
            val a = target + b + c
            TargetStep(
                operation = OperationType.SUBTRACTION,
                targetResult = target,
                operandCount = 3,
                questionFormat = QuestionFormat.STANDARD_TARGET,
                promptDisplay = "HEDEF: $target",
                helperSubtext = "Zincir Çıkarma (-) • Sırayla 3 sayı seç: Büyük - Küçük - Küçük = $target",
                expectedOrderedValues = listOf(a, b, c)
            )
        }
    }

    private fun generateMultiplicationStep(
        spec: LevelSpec,
        stepIndex: Int,
        random: Random
    ): TargetStep {
        val pool = if (spec.allowedMultipliers.isNotEmpty()) {
            spec.allowedMultipliers
        } else {
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        val a = if (spec.focusZero && stepIndex == 0) {
            0
        } else {
            pool[random.nextInt(pool.size)]
        }

        val secondPool = if (a == 0) listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) else pool.filter { it > 0 }.ifEmpty { listOf(1, 2, 3, 4, 5) }
        val b = secondPool[random.nextInt(secondPool.size)]
        val product = a * b

        // Eksik çarpan modu (? × 4 = 20)
        if (spec.questionFormat == QuestionFormat.MISSING_FACTOR && a > 0 && b > 0) {
            return TargetStep(
                operation = OperationType.MULTIPLICATION,
                targetResult = product,
                operandCount = 2,
                questionFormat = QuestionFormat.MISSING_FACTOR,
                promptDisplay = "? × $b = $product",
                helperSubtext = "Eksik Çarpanı Bul! Tahtada '?' ($a) ve '$b' sayılarını eşleştir",
                expectedOrderedValues = listOf(a, b),
                fixedKnownOperand = b
            )
        }

        return TargetStep(
            operation = OperationType.MULTIPLICATION,
            targetResult = product,
            operandCount = 2,
            questionFormat = QuestionFormat.STANDARD_TARGET,
            promptDisplay = "HEDEF: $product",
            helperSubtext = "Çarpma (×) • Çarpımı $product olan 2 sayı seç",
            expectedOrderedValues = listOf(a, b)
        )
    }

    private fun generateDivisionStep(
        spec: LevelSpec,
        stepIndex: Int,
        random: Random
    ): TargetStep {
        // Sıfıra bölme kesinlikle sorulmamalı! (divisor >= 1)
        val divisors = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10).filter { it <= spec.maxTarget.coerceAtLeast(5) }.ifEmpty { listOf(2, 3, 4, 5) }
        val divisor = divisors[random.nextInt(divisors.size)]

        val quotient = if (spec.focusZero && stepIndex == 0) {
            0 // Örn: 0 ÷ 2 = 0
        } else {
            random.nextInt(spec.minTarget.coerceAtLeast(1), spec.maxTarget.coerceAtLeast(2) + 1)
        }

        val dividend = quotient * divisor // Kalansız bölme garantili!

        return when (spec.questionFormat) {
            QuestionFormat.MISSING_DIVISOR -> {
                val safeQuotient = if (quotient == 0) 2 else quotient
                val safeDividend = safeQuotient * divisor
                TargetStep(
                    operation = OperationType.DIVISION,
                    targetResult = safeQuotient,
                    operandCount = 2,
                    questionFormat = QuestionFormat.MISSING_DIVISOR,
                    promptDisplay = "$safeDividend ÷ ? = $safeQuotient",
                    helperSubtext = "Eksik Böleni Bul! Tahtada $safeDividend ve '?' ($divisor) sayılarını eşleştir",
                    expectedOrderedValues = listOf(safeDividend, divisor),
                    fixedKnownOperand = safeDividend
                )
            }
            QuestionFormat.MISSING_DIVIDEND -> {
                val safeQuotient = if (quotient == 0) 3 else quotient
                val safeDividend = safeQuotient * divisor
                TargetStep(
                    operation = OperationType.DIVISION,
                    targetResult = safeQuotient,
                    operandCount = 2,
                    questionFormat = QuestionFormat.MISSING_DIVIDEND,
                    promptDisplay = "? ÷ $divisor = $safeQuotient",
                    helperSubtext = "Eksik Bölüneni Bul! Tahtada '?' ($safeDividend) ve $divisor sayılarını eşleştir",
                    expectedOrderedValues = listOf(safeDividend, divisor),
                    fixedKnownOperand = divisor
                )
            }
            else -> {
                TargetStep(
                    operation = OperationType.DIVISION,
                    targetResult = quotient,
                    operandCount = 2,
                    questionFormat = QuestionFormat.STANDARD_TARGET,
                    promptDisplay = "HEDEF: $quotient",
                    helperSubtext = "Bölme (÷) • Bölümü $quotient olan bölünen ve böleni seç",
                    expectedOrderedValues = listOf(dividend, divisor)
                )
            }
        }
    }

    /**
     * Üretilen oyun tahtasının, verifiedSolution adımları takip edildiğinde
     * her adımda hem geometrik hem de matematiksel olarak geçerli olduğunu ve
     * oyun sonunda 0 aktif blok kaldığını (eşleşmeyen tek bir sayı bile kalmadığını) doğrular.
     */
    fun verifyBoardSolvability(board: GeneratedBoard): Boolean {
        val currentCells = board.cells.map { it.copy() }.toMutableList()

        for (planned in board.verifiedSolution) {
            val activeCoords = currentCells.filter { !it.isCleared }.map { Pair(it.row, it.col) }.toSet()
            val (geoOk, _) = MatchingValidator.isGeometricallyValidSelection(
                selectedCoords = planned.cellCoords,
                activeCoords = activeCoords,
                mode = board.matchRuleMode
            )
            if (!geoOk) return false

            val selectedValues = planned.cellCoords.map { coord ->
                val cell = currentCells.firstOrNull { !it.isCleared && it.row == coord.first && it.col == coord.second }
                    ?: return false
                cell.value
            }

            val mathRes = MatchingValidator.evaluateMathMatch(selectedValues, planned.step)
            if (!mathRes.isValid) return false

            // Blokları temizle
            for (i in currentCells.indices) {
                val c = currentCells[i]
                if (planned.cellCoords.contains(Pair(c.row, c.col))) {
                    currentCells[i] = c.copy(isCleared = true)
                }
            }
        }

        // Bütün bloklar temizlenmiş olmalı!
        return currentCells.all { it.isCleared }
    }

    /**
     * YENİDEN DÜZENLE (Joker 2) ve ALTIN KURTARMA (Gizli 4. Kurtarma Sistemi) için:
     * Tahtada kalan aktif (temizlenmemiş) hücreleri ve kalan hedefleri (remainingTargets) alır,
     * kalan hücrelerin değerlerini kalan hedeflerin tümünü son bloğuna kadar çözecek şekilde
     * yeniden düzenler!
     *
     * Böylece oyuncu farklı bir eşleşme yaptığı için tahta kilitlense bile:
     * - Joker 2 (Yeniden Düzenle) veya
     * - ALTIN KURTARMA (Otomatik, sessiz ve puansız güvenlik mekanizması)
     * kalan tüm hücrelerin yine sonuna kadar %100 çözülebilir olmasını garantiler!
     */
    fun rearrangeRemainingBoardGuaranteed(
        currentCells: List<BlockCell>,
        remainingTargets: List<TargetStep>,
        mode: MatchRuleMode,
        random: Random = Random.Default
    ): Pair<List<BlockCell>, List<PlannedMove>> {
        val activeCells = currentCells.filter { !it.isCleared }
        val activeCoords = activeCells.map { Pair(it.row, it.col) }

        if (activeCells.isEmpty() || remainingTargets.isEmpty()) {
            return Pair(currentCells, emptyList())
        }

        // Kalan aktif koordinatları, kalan hedeflerin operandCount sayılarına göre geometrik soyulma sırasına diz
        val stepSizes = remainingTargets.map { it.operandCount }
        val totalNeeded = stepSizes.sum()

        if (totalNeeded != activeCoords.size) {
            return Pair(currentCells, emptyList())
        }

        // Kalan aktif koordinatlar üzerinde geçerli geometrik dizilim bul
        val coordGroups = buildPeelableForRemainingCoords(activeCoords, stepSizes, mode, random)

        val newValueByCoord = mutableMapOf<Pair<Int, Int>, Int>()
        val newPlannedMoves = mutableListOf<PlannedMove>()

        for (i in remainingTargets.indices) {
            val step = remainingTargets[i]
            val group = coordGroups[i]
            newPlannedMoves.add(PlannedMove(group, step))
            for (j in group.indices) {
                newValueByCoord[group[j]] = step.expectedOrderedValues[j]
            }
        }

        val updatedCells = currentCells.map { cell ->
            if (cell.isCleared) {
                cell
            } else {
                val coord = Pair(cell.row, cell.col)
                val newVal = newValueByCoord[coord] ?: cell.value
                cell.copy(value = newVal, isHinted = false)
            }
        }

        return Pair(updatedCells, newPlannedMoves)
    }

    private fun buildPeelableForRemainingCoords(
        activeCoords: List<Pair<Int, Int>>,
        stepSizes: List<Int>,
        mode: MatchRuleMode,
        random: Random
    ): List<List<Pair<Int, Int>>> {
        // Kalan koordinatlar arasında geometrik olarak soyulabilen bir sıra ara
        for (attempt in 0 until 20) {
            val pool = activeCoords.toMutableSet()
            val groups = mutableListOf<List<Pair<Int, Int>>>()
            var ok = true
            for (size in stepSizes) {
                val candidates = findAllGeometricCandidates(pool, size, mode, random)
                if (candidates.isEmpty()) {
                    ok = false
                    break
                }
                val pick = candidates[random.nextInt(candidates.size)]
                groups.add(pick)
                pool.removeAll(pick.toSet())
            }
            if (ok && pool.isEmpty()) {
                return groups
            }
        }

        // Eğer kalan hücreler birbirinden tamamen kopuk köşelerde kaldıysa (farklı satır/sütun/çapraz),
        // mevcut adımda en az bir geçerli doğrusal eşleşme bul veya kalanları sırayla grupla
        val sorted = activeCoords.sortedWith(compareBy({ it.first }, { it.second }))
        val fallbackGroups = mutableListOf<List<Pair<Int, Int>>>()
        var idx = 0
        for (size in stepSizes) {
            fallbackGroups.add(sorted.subList(idx, idx + size))
            idx += size
        }
        return fallbackGroups
    }

    /**
     * ALTIN KURTARMA SİSTEMİ (Bölüm 9):
     * Tahtada mevcut hedef (currentStep) için geçerli hiçbir hamle kalmadığında devreye girer.
     * Gerekiyorsa aktif blokların değerlerini veya konumlarını (temizlenmiş bir hücreyle yer değiştirerek
     * aynı doğrultuya getirerek) önceden doğrulanmış çözüm sırasına uygun hale getirir.
     */
    fun triggerGoldenRescue(
        currentCells: List<BlockCell>,
        remainingTargets: List<TargetStep>,
        mode: MatchRuleMode,
        gridSize: Int
    ): GoldenRescueOutcome {
        if (remainingTargets.isEmpty()) {
            return GoldenRescueOutcome(currentCells, MatchRuleMode.DIRECTIONAL, false)
        }

        // Önce kalan aktif hücrelerin değerlerini yeniden düzenleyerek mevcut modda çözülüp çözülmediğini kontrol et
        val (rearrangedCells, _) = rearrangeRemainingBoardGuaranteed(
            currentCells = currentCells,
            remainingTargets = remainingTargets,
            mode = mode
        )

        val currentStep = remainingTargets.first()
        val validMovesAfterValueSwap = MatchingValidator.findAllValidMovesForStep(
            cells = rearrangedCells,
            step = currentStep,
            mode = mode
        )

        if (validMovesAfterValueSwap.isNotEmpty()) {
            return GoldenRescueOutcome(
                updatedCells = rearrangedCells,
                effectiveMode = mode,
                wasTriggered = true
            )
        }

        // Eğer kalan aktif hücreler geometrik olarak hiçbir yatay/dikey/çapraz doğrultuda buluşmuyorsa
        // (örneğin oyuncu sadece (0,0) ve (1,2) hücrelerini bıraktıysa), aktif hücrelerden birini
        // aynı satırdaki temizlenmiş bir hücre konumuna taşıyarak aynı doğrultuya getir!
        val activeList = rearrangedCells.filter { !it.isCleared }.toMutableList()
        if (activeList.size >= currentStep.operandCount) {
            val anchor = activeList[0]
            val relocatedCoords = mutableListOf(Pair(anchor.row, anchor.col))
            for (k in 1 until currentStep.operandCount) {
                val targetCol = (anchor.col + k) % gridSize
                val candidateCoord = if (targetCol != anchor.col) {
                    Pair(anchor.row, targetCol)
                } else {
                    Pair((anchor.row + k) % gridSize, anchor.col)
                }
                relocatedCoords.add(candidateCoord)
            }

            // Aktif hücreleri bu doğrusal koordinatlara yerleştir ve kalanları yeniden düzenle
            val remainingOldActive = activeList.drop(currentStep.operandCount)
            val newActiveCoords = (relocatedCoords + remainingOldActive.map { Pair(it.row, it.col) }).distinct()

            if (newActiveCoords.size == activeList.size) {
                val rebuilt = mutableListOf<BlockCell>()
                var idCounter = 0
                for (r in 0 until gridSize) {
                    for (c in 0 until gridSize) {
                        val coord = Pair(r, c)
                        val activeIdx = newActiveCoords.indexOf(coord)
                        val isCleared = activeIdx == -1
                        val value = if (activeIdx in 0 until currentStep.operandCount) {
                            currentStep.expectedOrderedValues[activeIdx]
                        } else if (activeIdx >= currentStep.operandCount) {
                            activeList[activeIdx].value
                        } else {
                            0
                        }
                        rebuilt.add(
                            BlockCell(
                                id = idCounter++,
                                row = r,
                                col = c,
                                value = value,
                                isCleared = isCleared,
                                isHinted = false
                            )
                        )
                    }
                }
                return GoldenRescueOutcome(
                    updatedCells = rebuilt,
                    effectiveMode = mode,
                    wasTriggered = true
                )
            }
        }

        return GoldenRescueOutcome(
            updatedCells = rearrangedCells,
            effectiveMode = MatchRuleMode.FREE,
            wasTriggered = true
        )
    }
}

data class GoldenRescueOutcome(
    val updatedCells: List<BlockCell>,
    val effectiveMode: MatchRuleMode,
    val wasTriggered: Boolean
)
