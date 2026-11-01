package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.Board
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind
import kotlin.math.abs
import kotlin.random.Random

/**
 * Tahta kurucu.
 *
 * Tahta 6 sütun ve bölümün uzunluğuna göre 6, 10 veya 50 satırdır. Plan, tahtadaki HER
 * bloğu gruplara böler; hiçbir blok eşleşmesiz kalmaz. Gruplar satır içinde yan yana
 * bloklardır; çocuk isterse düz, çapraz veya dönüşlü komşuluklarla da oynayabilir.
 *
 * Önemli yenilik: değerler rastgele değil, hedefin TÜM 2'li kombinasyonlarını EŞİT sayıda
 * içeren karıştırılmış bir desteden dağıtılır. 4 toplamında 0+4, 1+3 ve 2+2 aynı sayıda
 * gelir. Destede tam bölünme yoksa 3'lü/4'lü gruplar hesaba katılarak grup sayısı buna
 * göre seçilir.
 */
object BoardBuilder {

    /**
     * Sonsuz Av tahtası: 6×100 (veya tablette 15×100). Sabit bir çözüm planı yoktur;
     * her kare 1..100 arasında bir sayıyla dolar ve temizlenen kare anında yenilenir.
     */
    fun buildEndless(
        spec: LevelSpec,
        random: Random = Random.Default
    ): Board {
        val cells = ArrayList<BlockCell>(spec.totalCells)
        for (r in 0 until spec.rows) {
            for (c in 0 until spec.cols) {
                cells.add(
                    BlockCell(
                        id = cells.size,
                        row = r,
                        col = c,
                        value = random.nextInt(1, 101)
                    )
                )
            }
        }
        return Board(
            rows = spec.rows,
            cols = spec.cols,
            cells = cells,
            plan = emptyList(),
            ruleMode = spec.ruleMode,
            seed = random.nextInt(1, Int.MAX_VALUE)
        )
    }

    fun build(spec: LevelSpec, random: Random = Random.Default, seed: Int = random.nextInt()): Board {
        val sizes = allowedSizesFor(spec)
        val plan = buildPlan(spec, sizes, random)
        val twoGroups = plan.count { it.size == 2 }

        val decks = mutableMapOf<Int, List<List<Int>>>()
        for (size in sizes) {
            val count = plan.count { it.size == size }
            val combos = RuleEngine.balancedCombos(spec.ruleKind, spec.target, size, count)
            decks[size] = deckFor(combos, count, random)
        }

        val valuesByCoord = mutableMapOf<Pair<Int, Int>, Int>()
        val cursor = mutableMapOf<Int, Int>()
        for (group in plan) {
            val size = group.size
            val deck = decks[size].orEmpty()
            val index = cursor[size] ?: 0
            cursor[size] = index + 1
            val combo = when {
                deck.isEmpty() -> fallbackOperands(spec, size, random)
                else -> deck[index % deck.size]
            }
            val ordered = combo.shuffled(random)
            group.forEachIndexed { i, coord ->
                valuesByCoord[coord] = ordered.getOrElse(i) { 0 }
            }
        }

        val cells = ArrayList<BlockCell>(spec.totalCells)
        for (r in 0 until spec.rows) {
            for (c in 0 until spec.cols) {
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
            rows = spec.rows,
            cols = spec.cols,
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
        // Tek sayıda sütunda her satırı tamamen kapatmak için en az bir 3'lü grup gerekir.
        return if (spec.cols % 2 == 1 && !base.contains(3)) base + 3 else base
    }

    /**
     * Satırları, istenen sayıda 2'li grup oluşturacak biçimde seçer.
     *
     * 6 sütunda satır seçenekleri: üç ikili, bir ikili + bir dörtlü, iki üçlü.
     * Böylece 2'li grup sayısı, kombinasyon çeşidi sayısına tam bölünecek şekilde
     * ayarlanabilir. Amaç, tahtanın yarısından çoğunu 2'li gruplara ayırıp kalanı
     * 3'lü/4'lü kombolar için bırakmaktır.
     */
    private fun buildPlan(
        spec: LevelSpec,
        allowed: List<Int>,
        random: Random
    ): List<List<Pair<Int, Int>>> {
        val cols = spec.cols.coerceAtLeast(2)
        val rows = spec.rows.coerceAtLeast(1)
        val choices = if (spec.ruleKind == RuleKind.SUM) {
            chooseRowChoices(rows, cols, allowed, spec.target, random)
        } else {
            // Diğer işlemler yalnızca iki sayıyla oynanır: her satır ikili gruplara ayrılır.
            List(rows) { rowParts(cols, allowed) }
        }

        val groups = mutableListOf<List<Pair<Int, Int>>>()
        val orderedRows = choices.shuffled(random)
        orderedRows.forEachIndexed { r, parts ->
            var c = 0
            for (size in parts) {
                if (c + size > cols) break
                groups.add((0 until size).map { r to (c + it) })
                c += size
            }
        }
        return groups
    }

    private fun chooseRowChoices(
        rows: Int,
        cols: Int,
        allowed: List<Int>,
        target: Int,
        random: Random
    ): List<List<Int>> {
        val options = rowOptions(cols, allowed).shuffled(random)
        if (options.isEmpty()) return List(rows) { rowParts(cols, allowed) }

        val maxTwos = options.maxOf { option -> option.count { it == 2 } } * rows
        val reachable = Array(rows + 1) { BooleanArray(maxTwos + 1) }
        val choice = Array(rows + 1) { IntArray(maxTwos + 1) { -1 } }
        reachable[0][0] = true

        for (r in 1..rows) {
            for (twos in 0..maxTwos) {
                if (!reachable[r - 1][twos]) continue
                options.forEachIndexed { index, option ->
                    val add = option.count { it == 2 }
                    val next = twos + add
                    if (next <= maxTwos && !reachable[r][next]) {
                        reachable[r][next] = true
                        choice[r][next] = index
                    }
                }
            }
        }

        val achievable = (0..maxTwos).filter { reachable[rows][it] }
        if (achievable.isEmpty()) return List(rows) { rowParts(cols, allowed) }

        val maxTwosForBoard = achievable.max()
        val variety = RuleEngine.combosFor(RuleKind.SUM, target, 2).size.coerceAtLeast(1)
        // Hedef, tahtanın çoğu 2'li olsun ama 3'lü/4'lü kombolar da kalsın diye ~%60.
        val desired = (maxTwosForBoard * 0.6).toInt()
        val hasLargerGroups = options.any { option -> option.any { it != 2 } }
        val divisible = achievable.filter { it % variety == 0 }
        val candidates = divisible
            .filter { !hasLargerGroups || it < maxTwosForBoard }
            .sortedBy { abs(it - desired) }

        // Hedeflenen sayı yoksa (örneğin 6×6'da 16 çeşit) en yakın bölünebilir değere düş.
        val targetTwos = candidates.firstOrNull()
            ?: divisible.maxOrNull()
            ?: maxTwosForBoard

        var twos = targetTwos
        val picked = ArrayList<List<Int>>(rows)
        for (r in rows downTo 1) {
            val index = choice[r][twos]
            if (index < 0) break
            val option = options[index]
            picked.add(option)
            twos -= option.count { it == 2 }
        }
        while (picked.size < rows) {
            picked.add(rowParts(cols, allowed))
        }
        return picked
    }

    /** Bir satırı izin verilen blok boyutlarına böler; her zaman 2'li/3'lü/4'lü parçalar. */
    private fun rowParts(cols: Int, allowed: List<Int>): List<Int> {
        val sizes = allowed.filter { it in 2..4 }.distinct().sorted().ifEmpty { listOf(2) }
        val result = ArrayList<Int>()
        var left = cols
        while (left > 0) {
            val size = sizes.lastOrNull { it <= left }
                ?: sizes.first().coerceAtMost(left).coerceAtLeast(2)
            if (size < 2) break
            result.add(size)
            left -= size
            if (left == 1) {
                // Tek hücre kalmaz: son iki grubu birleştir.
                if (result.size >= 2) {
                    val last = result.removeAt(result.size - 1)
                    val prev = result.removeAt(result.size - 1)
                    result.add(prev + last)
                }
                left = 0
            }
        }
        return result
    }

    /** Bir satırın izin verilen boyutlarla tüm bölünmeleri (6 sütunda en fazla 4 seçenek). */
    private fun rowOptions(cols: Int, allowed: List<Int>): List<List<Int>> {
        val sizes = allowed.filter { it in 2..4 }.distinct().sorted()
        if (sizes.isEmpty()) return emptyList()
        val out = mutableListOf<List<Int>>()
        val current = mutableListOf<Int>()
        fun walk(left: Int) {
            if (left == 0) {
                out.add(current.toList())
                return
            }
            for (size in sizes) {
                if (size <= left) {
                    current.add(size)
                    walk(left - size)
                    current.removeAt(current.size - 1)
                }
            }
        }
        walk(cols)
        return out.distinct()
    }

    private fun deckFor(
        combos: List<List<Int>>,
        count: Int,
        random: Random
    ): List<List<Int>> {
        if (combos.isEmpty() || count <= 0) return emptyList()
        val base = combos.shuffled(random)
        val out = ArrayList<List<Int>>(count)
        repeat(count) { out.add(base[it % base.size]) }
        return out.shuffled(random)
    }

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
