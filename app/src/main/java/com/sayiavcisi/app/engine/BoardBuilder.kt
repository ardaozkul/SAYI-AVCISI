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
 * Her grup aynı hatta (yatay/dikey/çapraz) bitişik hücrelerden oluşur.
 *
 * Önemli yenilik: değerler rastgele değil, hedefin TÜM kombinasyonlarını içeren karıştırılmış
 * bir desteden sırayla dağıtılır. Böylece "4 toplamı" bölümünde 0+4, 1+3 ve 2+2 eşit sıklıkta
 * gelir; sadece 1+3 çıkması gibi bir dengesizlik olmaz.
 */
object BoardBuilder {

    private val DIRECTIONS = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    fun build(spec: LevelSpec, random: Random = Random.Default, seed: Int = random.nextInt()): Board {
        val sizes = allowedSizesFor(spec)
        val plan = plan(spec.gridSize, sizes, random)
        val valuesByCoord = mutableMapOf<Pair<Int, Int>, Int>()
        val cursor = mutableMapOf<Int, Int>()

        for (group in plan) {
            val size = group.size
            val deck = combosFor(spec, size)
            val index = cursor[size] ?: 0
            cursor[size] = index + 1
            val combo = when {
                deck.isEmpty() -> fallbackOperands(spec, size, random)
                else -> deck[index % deck.size]
            }
            val ordered = combo.shuffled(random)
            group.forEachIndexed { i, coord -> valuesByCoord[coord] = ordered[i] }
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
                        // Plan dışı kalan kare (yalnızca hatalı bölüm tanımında olur) boş bırakılır.
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

    /** Bölüm hedefinin blok grubu boyutları. Toplamada 2, 3 ve 4'lü kombinasyonlar serbest. */
    fun allowedSizesFor(spec: LevelSpec): List<Int> {
        if (spec.ruleKind != RuleKind.SUM) return listOf(2)
        val requested = spec.groupSizes.filter { it in 2..4 }.distinct()
        val base = if (requested.isEmpty()) listOf(2) else requested
        val total = spec.gridSize * spec.gridSize
        // Tek hücreli ızgara yalnızca ikili/üçlü ile kaplanamaz; üçlü ekle.
        return if (total % 2 == 1 && !base.contains(3)) base + 3 else base
    }

    /**
     * Izgarayı bitişik, aynı hatta olan gruplarla tamamen kaplar.
     * Rastgele diziliş denenir (yatay/dikey/çapraz karışımı), tutmazsa her zaman
     * çalışan satır dizilişine düşülür.
     */
    fun plan(gridSize: Int, sizes: List<Int>, random: Random): List<List<Pair<Int, Int>>> {
        val allowed = sizes.filter { it in 2..4 }.ifEmpty { listOf(2) }
        val usable = if (gridSize * gridSize % 2 == 1 && !allowed.contains(3)) allowed + 3 else allowed

        repeat(60) {
            val attempt = tryRandomTiling(gridSize, usable, random)
            if (attempt != null) return attempt
        }
        return rowTiling(gridSize, usable, random)
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
     * Her zaman çalışan garantili diziliş: her satır, izin verilen boyutların geçerli bir
     * bölünmesiyle gruplara ayrılır. 4x4, 6x6 ve 8x8 çift boyutlu olduğu için tam kaplama olur.
     */
    fun rowTiling(
        gridSize: Int,
        allowed: List<Int>,
        random: Random = Random(1)
    ): List<List<Pair<Int, Int>>> {
        val allowedSizes = allowed.filter { it in 2..4 }.ifEmpty { listOf(2) }
        val groups = mutableListOf<List<Pair<Int, Int>>>()
        for (r in 0 until gridSize) {
            val parts = rowPartitions(gridSize, allowedSizes)
            val choice = if (parts.isEmpty()) listOf(gridSize) else parts[random.nextInt(parts.size)]
            var c = 0
            for (size in choice) {
                groups.add((0 until size).map { r to (c + it) })
                c += size
            }
        }
        return groups.filter { it.size >= 2 }
    }

    /** Satır uzunluğunun izin verilen boyutlara tüm bölünmeleri (küçük liste). */
    private fun rowPartitions(length: Int, allowed: List<Int>): List<List<Int>> {
        val out = mutableListOf<List<Int>>()
        val current = mutableListOf<Int>()
        fun walk(left: Int) {
            if (left == 0) {
                out.add(current.toList())
                return
            }
            for (size in allowed) {
                if (size <= left) {
                    current.add(size)
                    walk(left - size)
                    current.removeAt(current.size - 1)
                }
            }
        }
        walk(length)
        return out.filter { it.isNotEmpty() }.ifEmpty { listOf(listOf(length)) }
    }

    /** Hedefin tüm kombinasyonlarından karıştırılmış tek grup boyu destesi. */
    private fun combosFor(spec: LevelSpec, groupSize: Int): List<List<Int>> =
        RuleEngine.combosFor(spec.ruleKind, spec.target, groupSize)

    /** Deste boşsa (müfredat dışı hedef) hedefi bozmayan güvenli değerler. */
    private fun fallbackOperands(spec: LevelSpec, groupSize: Int, random: Random): List<Int> {
        val direct = RuleEngine.operandsFor(spec.ruleKind, spec.target, groupSize, random)
        if (direct != null && direct.size == groupSize && direct.all { it in 0..99 }) return direct
        return when (spec.ruleKind) {
            RuleKind.SUM -> {
                val a = random.nextInt(0, spec.target.coerceAtMost(50) + 1)
                val rest = spec.target - a
                if (groupSize == 2) listOf(a, rest)
                else listOf(a, rest.coerceAtMost(33), (rest - rest.coerceAtMost(33)).coerceAtLeast(0))
            }

            RuleKind.DIFF -> listOf(spec.target.coerceIn(0, 99), 0)
            RuleKind.PRODUCT -> listOf(spec.target.coerceIn(0, 99), 1)
            RuleKind.QUOTIENT -> listOf(spec.target.coerceIn(0, 99), 1)
        }
    }

    /** Mod değişirse (LINE_OF_SIGHT <-> FREE) tahtayı olduğu gibi korur, kuralı yorumlama değişir. */
    fun withMode(board: Board, mode: MatchRuleMode): Board = board.copy(ruleMode = mode)
}
