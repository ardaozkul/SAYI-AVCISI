package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import kotlin.random.Random

/**
 * Tek doğruluk kaynağı: bir seçimin geometrik ve matematiksel olarak geçerli olup olmadığı.
 *
 * Sıra bağımsızdır: çocuk blokları hangi sırada dokunursa dokunsun aynı sonuç çıkar.
 * (1.0 sürümündeki "bazen oluyor bazen olmuyor" şikâyetinin kökü sıra bağımlılığıydı.)
 */
object RuleEngine {

    /** Seçilen bloklar aynı satır/sütun/45° çapraz üzerinde mi? */
    fun areCollinear(coords: List<Pair<Int, Int>>): Boolean {
        if (coords.size < 2) return false
        val anchor = coords.first()
        var dirR = 0
        var dirC = 0
        for (p in coords.drop(1)) {
            val dr = p.first - anchor.first
            val dc = p.second - anchor.second
            if (dr == 0 && dc == 0) return false
            if (dr != 0 && dc != 0 && abs(dr) != abs(dc)) return false
            dirR = dr.sign
            dirC = dc.sign
            break
        }
        if (dirR == 0 && dirC == 0) return false
        for (p in coords.drop(1)) {
            val dr = p.first - anchor.first
            val dc = p.second - anchor.second
            if (dr * dirC != dc * dirR) return false
        }
        return true
    }

    /** Hat üzerindeki sıraya dizilmiş iki komşu seçim arasında aktif blok var mı? */
    private fun hasBlockerBetween(
        a: Pair<Int, Int>,
        b: Pair<Int, Int>,
        active: Set<Pair<Int, Int>>
    ): Boolean {
        val dr = b.first - a.first
        val dc = b.second - a.second
        val stepR = dr.sign
        val stepC = dc.sign
        val steps = max(abs(dr), abs(dc))
        for (i in 1 until steps) {
            if (active.contains(a.first + i * stepR to a.second + i * stepC)) return true
        }
        return false
    }

    /**
     * Geometri kontrolü. Dönüş: null => geçerli, değilse çocuğun anlayacağı Türkçe açıklama.
     */
    fun geometryProblem(
        selected: List<Pair<Int, Int>>,
        active: Set<Pair<Int, Int>>,
        mode: MatchRuleMode
    ): String? {
        if (selected.size < 2) return "En az iki blok seçmelisin."
        if (selected.distinct().size != selected.size) return "Aynı bloğu iki kez seçemezsin."
        if (selected.any { !active.contains(it) }) return "Seçtiğin bloklardan biri zaten temizlenmiş."
        if (mode == MatchRuleMode.FREE) return null

        if (!areCollinear(selected)) {
            return "Seçtiklerin aynı hatta olmalı: yatay, dikey veya çapraz."
        }
        val ordered = orderAlongLine(selected) ?: return "Seçtiklerin aynı hatta olmalı."
        for (i in 1 until ordered.size) {
            if (hasBlockerBetween(ordered[i - 1], ordered[i], active)) {
                return "Arada hâlâ bir blok var! Önce onu temizle ya da bitişik olanı seç."
            }
        }
        return null
    }

    /** Seçikleri hat üzerinde uçtan uca sıralar; doğru çizgi değilse null döner. */
    private fun orderAlongLine(selected: List<Pair<Int, Int>>): List<Pair<Int, Int>>? {
        if (selected.size == 2) return selected
        val a = selected[0]
        val b = selected[1]
        val dr = (b.first - a.first).sign
        val dc = (b.second - a.second).sign
        if (dr == 0 && dc == 0) return null
        return selected.sortedWith(compareBy({ it.first * dr + it.second * dc }, { it.first }))
    }

    /** Matematik kontrolü: sıra bağımsız. */
    fun mathMatches(values: List<Int>, kind: RuleKind, target: Int): Boolean = when (kind) {
        RuleKind.SUM -> values.sum() == target
        RuleKind.DIFF -> values.size == 2 && abs(values[0] - values[1]) == target
        RuleKind.PRODUCT -> values.size == 2 && values[0] * values[1] == target
        RuleKind.QUOTIENT -> values.size == 2 && quotientMatches(values[0], values[1], target)
    }

    private fun quotientMatches(x: Int, y: Int, target: Int): Boolean {
        if (y != 0 && x % y == 0 && x / y == target) return true
        if (x != 0 && y % x == 0 && y / x == target) return true
        return false
    }

    /** Doğru eşleşmenin çocuk dilinde ifadesi. */
    fun expressionOf(values: List<Int>, kind: RuleKind): String = when (kind) {
        RuleKind.SUM -> values.joinToString(" + ") + " = " + values.sum()
        RuleKind.DIFF -> {
            val hi = values.max(); val lo = values.min()
            "$hi - $lo = ${hi - lo}"
        }
        RuleKind.PRODUCT -> values.joinToString(" × ") + " = " + values.reduce { a, b -> a * b }
        RuleKind.QUOTIENT -> {
            val (dividend, divisor) = quotientPair(values)
            "$dividend ÷ $divisor = ${dividend / divisor}"
        }
    }

    /** Hangi sırayla seçilirse seçilsin geçerli bölmenin (bölünen, bölen) ikilisi. */
    private fun quotientPair(values: List<Int>): Pair<Int, Int> {
        val (a, b) = values[0] to values[1]
        return when {
            b != 0 && a != 0 && a % b == 0 -> a to b
            b != 0 && a == 0 -> 0 to b
            a != 0 && b == 0 -> b to a
            else -> a to b
        }
    }

    /** Yanlış eşleşme geri bildirimi. */
    fun failureMessage(values: List<Int>, kind: RuleKind, target: Int): String {
        val actual = when (kind) {
            RuleKind.SUM -> values.sum()
            RuleKind.DIFF -> if (values.size == 2) abs(values[0] - values[1]) else 0
            RuleKind.PRODUCT -> if (values.size == 2) values[0] * values[1] else 0
            RuleKind.QUOTIENT -> {
                val (dividend, divisor) = quotientPair(values)
                if (divisor == 0) -1 else dividend / divisor
            }
        }
        val unit = kind.unitLabel
        return if (actual < 0) {
            "Bir sayıyı 0'a bölemeyiz! Sıfır olmayan bir blok seç."
        } else {
            "Seçtiklerinin $unit $actual etti, hedef $target. Başka bir kombinasyon dene!"
        }
    }

    /** Tahtadaki tüm geçerli hamleler (geometri + matematik). */
    fun findAllMoves(
        cells: List<BlockCell>,
        kind: RuleKind,
        target: Int,
        groupSizes: List<Int>,
        mode: MatchRuleMode
    ): List<List<Pair<Int, Int>>> {
        val active = cells.filterNot { it.isCleared }
        val activeCoords = active.map { it.coord }.toSet()
        val moves = mutableListOf<List<Pair<Int, Int>>>()

        for (size in groupSizes.filter { it in 2..3 }) {
            if (active.size < size) continue
            forEachCombination(active, size) { combo ->
                val coords = combo.map { it.coord }
                if (geometryProblem(coords, activeCoords, mode) != null) return@forEachCombination
                if (!mathMatches(combo.map { it.value }, kind, target)) return@forEachCombination
                moves.add(orderAlongLine(coords) ?: coords)
            }
        }
        return moves.distinctBy { move -> move.map { it.first * 1000 + it.second }.sorted() }
    }

    private fun forEachCombination(
        items: List<BlockCell>,
        size: Int,
        action: (List<BlockCell>) -> Unit
    ) {
        val chosen = ArrayList<BlockCell>(size)
        fun walk(start: Int, left: Int) {
            if (left == 0) {
                action(chosen.toList())
                return
            }
            var i = start
            while (i <= items.size - left) {
                chosen.add(items[i])
                // Yalnızca aynı hatta olan seçimler dallanmaya devam eder: 10x10 tahtada arama
                // C(100,3) yerine yaklaşık hücre başına birkaç çizgiyle sınırlı kalır.
                if (chosen.size < 2 || areCollinear(chosen.map { it.coord })) {
                    walk(i + 1, left - 1)
                }
                chosen.removeAt(chosen.size - 1)
                i++
            }
        }
        walk(0, size)
    }

    /**
     * Hedefi sağlayan bir blok grubunun değerlerini üretir.
     * Böylece üretici tahta kurarken her grubun hedefi tutturacağı garanti altındadır.
     */
    fun operandsFor(kind: RuleKind, target: Int, groupSize: Int, random: Random): List<Int>? {
        if (groupSize == 3 && kind != RuleKind.SUM) return null
        return when (kind) {
            RuleKind.SUM -> {
                if (groupSize == 2) {
                    val a = random.nextInt(0, target + 1)
                    listOf(a, target - a)
                } else {
                    val a = random.nextInt(0, target + 1)
                    val b = random.nextInt(0, target - a + 1)
                    listOf(a, b, target - a - b)
                }
            }

            RuleKind.DIFF -> {
                val small = random.nextInt(0, max(1, minOf(target + 4, 12)))
                listOf(small + target, small)
            }

            RuleKind.PRODUCT -> factorPair(target, random)?.let { (x, y) ->
                if (random.nextBoolean()) listOf(x, y) else listOf(y, x)
            }

            RuleKind.QUOTIENT -> {
                val divisor = if (target == 0) random.nextInt(1, 11) else random.nextInt(2, 11)
                listOf(target * divisor, divisor)
            }
        }
    }

    private fun factorPair(target: Int, random: Random): Pair<Int, Int>? {
        if (target == 0) return 0 to random.nextInt(1, 11)
        val pairs = (1..12).mapNotNull { d ->
            if (target % d == 0 && target / d <= 12) d to target / d else null
        }
        if (pairs.isEmpty()) return null
        return pairs[random.nextInt(pairs.size)]
    }

    /** Çarpım hedefi tahtaya yazılabilir mi? Müfredat kurulumunda kullanılır. */
    fun isProductTargetReachable(target: Int): Boolean = factorPair(target, Random(1)) != null
}
