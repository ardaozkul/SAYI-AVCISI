package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.Board
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind
import kotlin.random.Random

/**
 * Tahta kurucu.
 *
 * Garanti: üretilen plan, ızgaradaki HER bloğu gruplara böler; hiçbir blok eşleşmesiz kalmaz.
 * Her grup aynı hatta (yatay/dikey/çapraz) bitişik hücrelerden oluşur, bu yüzden grup
 * aralarında hiçbir zaman blok kalmaz ve plan her sırayla uygulanabilir.
 * Grup değerleri RuleEngine.operandsFor ile üretilir, yani her grup bölümün TEK hedefini sağlar.
 */
object BoardBuilder {

    private val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    fun build(spec: LevelSpec, random: Random = Random.Default, seed: Int = random.nextInt()): Board {
        val sizes = allowedSizesFor(spec)
        val plan = plan(spec.gridSize, sizes, random)
        val valuesByCoord = mutableMapOf<Pair<Int, Int>, Int>()

        for (group in plan) {
            val operands = operandsOrFallback(spec.ruleKind, spec.target, group.size, random)
            group.forEachIndexed { i, coord -> valuesByCoord[coord] = operands[i] }
        }

        val cells = ArrayList<BlockCell>(spec.gridSize * spec.gridSize)
        for (r in 0 until spec.gridSize) {
            for (c in 0 until spec.gridSize) {
                val value = valuesByCoord[Pair(r, c)]
                cells.add(
                    BlockCell(
                        id = cells.size,
                        row = r,
                        col = c,
                        // Plan dışı kalan kare (yalnızca hatalı bölüm tanımında olur) boş bırakılır:
                        // asla eşleşmeyen bir blok üretmez.
                        value = value ?: 0,
                        isCleared = value == null
                    )
                )
            }
        }
        return Board(
            gridSize = spec.gridSize,
            cells = cells,
            plan = plan,
            ruleMode = spec.ruleMode,
            seed = seed
        )
    }

    /** Bölüm hedefinin blok grubu boyutları. Çarpma/bölme/çıkarma için yalnızca 2 kullanılır. */
    fun allowedSizesFor(spec: LevelSpec): List<Int> {
        if (spec.ruleKind != RuleKind.SUM) return listOf(2)
        val requested = spec.groupSizes.filter { it == 2 || it == 3 }
        val base = if (requested.isEmpty()) listOf(2) else requested
        val total = spec.gridSize * spec.gridSize
        // Toplam blok sayısı tek ise yalnızca ikili gruplarla tam temizleme imkânsız: üçlü gruba izin ver.
        return if (total % 2 == 1 && !base.contains(3)) base + 3 else base
    }

    /**
     * Izgarayı bitişik, aynı hatta olan gruplarla tamamen kaplar.
     * Rastgele diziliş denenir (oyun çeşitliliği için yatay/dikey/çapraz karışımı),
     * tutmazsa her zaman çalışan satır dizilişine düşülür.
     */
    fun plan(gridSize: Int, sizes: List<Int>, random: Random): List<List<Pair<Int, Int>>> {
        val allowed = sizes.filter { it == 2 || it == 3 }.ifEmpty { listOf(2) }
        // Tek hücre sayılı ızgara ikili gruplarla kaplanamaz; üçlü grup ekle.
        val usable = if (gridSize * gridSize % 2 == 1 && !allowed.contains(3)) allowed + 3 else allowed

        repeat(40) {
            val attempt = tryRandomTiling(gridSize, usable, random)
            if (attempt != null) return attempt
        }
        return rowTiling(gridSize, usable)
    }

    private fun tryRandomTiling(
        gridSize: Int,
        allowed: List<Int>,
        random: Random
    ): List<List<Pair<Int, Int>>>? {
        val taken = Array(gridSize) { BooleanArray(gridSize) }
        val groups = mutableListOf<List<Pair<Int, Int>>>()
        while (true) {
            val free = findFreeCell(gridSize, taken) ?: return groups
            val candidate = freePlacement(free, gridSize, allowed, taken, random) ?: return null
            groups.add(candidate)
            candidate.forEach { (r, c) -> taken[r][c] = true }
        }
    }

    private fun findFreeCell(gridSize: Int, taken: Array<BooleanArray>): Pair<Int, Int>? {
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!taken[r][c]) return r to c
            }
        }
        return null
    }

    /** Boştaki ilk hücreyi bir ucu olarak kullanan rastgele geçerli segment. */
    private fun freePlacement(
        anchor: Pair<Int, Int>,
        gridSize: Int,
        allowed: List<Int>,
        taken: Array<BooleanArray>,
        random: Random
    ): List<Pair<Int, Int>>? {
        val dirs = DIRECTIONS.shuffled(random)
        val sizes = allowed.shuffled(random)
        for ((dr, dc) in dirs) {
            for (size in sizes) {
                val cells = ArrayList<Pair<Int, Int>>(size)
                var ok = true
                for (i in 0 until size) {
                    val r = anchor.first + dr * i
                    val c = anchor.second + dc * i
                    if (r !in 0 until gridSize || c !in 0 until gridSize || taken[r][c]) {
                        ok = false
                        break
                    }
                    cells.add(r to c)
                }
                if (ok) return cells
            }
        }
        return null
    }

    /**
     * Her zaman çalışan garantili diziliş: satırlar boyunca bitişik ikili/üçlü gruplar.
     * Satır uzunluğu tekse son blok, bitişik olduğu önceki ikili grubun üçlüye büyütülmesinde kullanılır.
     */
    fun rowTiling(gridSize: Int, allowed: List<Int>): List<List<Pair<Int, Int>>> {
        val groups = mutableListOf<List<Pair<Int, Int>>>()
        for (r in 0 until gridSize) {
            val rowGroups = mutableListOf<List<Pair<Int, Int>>>()
            var c = 0
            while (gridSize - c >= 2) {
                val remaining = gridSize - c
                val size = when {
                    remaining % 2 == 1 && allowed.contains(3) -> 3
                    allowed.contains(2) -> 2
                    allowed.contains(3) -> 3
                    else -> remaining
                }.coerceAtMost(remaining)
                rowGroups.add((0 until size).map { r to (c + it) })
                c += size
            }
            if (c < gridSize) {
                val last = rowGroups.lastOrNull()
                if (last != null && last.size == 2 && last.last().second == c - 1) {
                    rowGroups[rowGroups.lastIndex] = last + (r to c)
                } else {
                    rowGroups.add(listOf(r to c))
                }
            }
            groups.addAll(rowGroups)
        }
        return groups.filter { it.size >= 2 }
    }

    /** operandsFor null dönerse (müfredat dışı target) hedefi bozmayan güvenli değer kümesi. */
    private fun operandsOrFallback(
        kind: RuleKind,
        target: Int,
        groupSize: Int,
        random: Random
    ): List<Int> {
        val direct = if (groupSize == 3 && kind != RuleKind.SUM) null
        else RuleEngine.operandsFor(kind, target, groupSize, random)
        if (direct != null && direct.size == groupSize && direct.all { it in 0..99 }) return direct

        return when (kind) {
            RuleKind.SUM -> {
                if (groupSize == 2) {
                    val a = random.nextInt(0, target.coerceAtMost(50) + 1)
                    listOf(a, target - a)
                } else {
                    val a = random.nextInt(0, target.coerceAtMost(33) + 1)
                    val b = random.nextInt(0, (target - a).coerceAtMost(33) + 1)
                    listOf(a, b, target - a - b)
                }
            }

            RuleKind.DIFF -> {
                val small = random.nextInt(0, 8)
                listOf(small + target, small)
            }

            RuleKind.PRODUCT -> productOperands(target, random) ?: listOf(target.coerceIn(0, 99), 1)

            RuleKind.QUOTIENT -> {
                val divisor = if (target > 9) 2 else random.nextInt(2, 10)
                val dividend = target * divisor
                if (dividend <= 99) listOf(dividend, divisor) else listOf(divisor, 1)
            }
        }
    }

    private fun productOperands(target: Int, random: Random): List<Int>? =
        RuleEngine.operandsFor(RuleKind.PRODUCT, target, 2, random)

    /** Mod değişirse (LINE_OF_SIGHT <-> FREE) tahtayı olduğu gibi korur, kuralı yorumlama değişir. */
    fun withMode(board: Board, mode: MatchRuleMode): Board = board.copy(ruleMode = mode)
}
