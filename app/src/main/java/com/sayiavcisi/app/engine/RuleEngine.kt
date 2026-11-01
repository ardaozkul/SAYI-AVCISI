package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.random.Random

/**
 * Tek doğruluk kaynağı: bir seçimin geometrik ve matematiksel olarak geçerli olup olmadığı.
 *
 * Yeni kural çocuğun parmağını sürüklemesine göre yazıldı:
 * - Aynı hatta (yatay, dikey, çapraz) ve aralarında taş kalmamış bloklar komşudur. "2 + 5" gibi
 *   aralarında boşluk olan taşlar da bu yüzden eşleşir.
 * - Parmağı komşu kareye çekerek 3 veya 4 blok seçilebilir; sıra ve yön serbesttir.
 * - Matematik sıra gözetmez: 1+4 ile 4+1, 9-4 ile 4-9 aynı kabul edilir.
 *
 * Sıra bağımsızlığı 1.x'teki "bazen oluyor bazen olmuyor" şikâyetinin kökünü kurutur.
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

    /** Hat üzerindeki iki seçim arasında aktif blok var mı? */
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

    /** İki blok yan yana mı (8 yön, köşe dahil)? */
    private fun neighbours(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean {
        val dr = abs(a.first - b.first)
        val dc = abs(a.second - b.second)
        return dr <= 1 && dc <= 1 && (dr + dc) > 0
    }

    /**
     * Geometri kontrolü. Dönüş: null => geçerli, değilse çocuğun anlayacağı Türkçe açıklama.
     *
     * Kabul edilen iki yol:
     * 1) Hepsi aynı hatta ve aralarında aktif blok yok (uzaktaki 2 + 5 dahil).
     * 2) Seçim sırasıyla her adım bir komşu kareye geçiyor (düz giderken çapraza dönebilir).
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

        if (isCleanLine(selected, active)) return null
        if (isNeighbourChain(selected)) return null

        return "Bloklar yan yana olmalı ya da aynı hatta ilerlemeli. Çapraz ve dönüş serbest."
    }

    /** Aynı hatta, aralarında aktif blok yok. */
    private fun isCleanLine(selected: List<Pair<Int, Int>>, active: Set<Pair<Int, Int>>): Boolean {
        if (!areCollinear(selected)) return false
        val ordered = orderAlongLine(selected) ?: return false
        for (i in 1 until ordered.size) {
            if (hasBlockerBetween(ordered[i - 1], ordered[i], active)) return false
        }
        return true
    }

    /** Parmağın izlediği yol: her seçim bir öncekinin komşusu. */
    private fun isNeighbourChain(selected: List<Pair<Int, Int>>): Boolean {
        for (i in 1 until selected.size) {
            if (!neighbours(selected[i - 1], selected[i])) return false
        }
        return true
    }

    /** Seçimleri hat üzerinde uçtan uca sıralar; doğru çizgi değilse null döner. */
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
        RuleKind.SUM -> values.size in 2..4 && values.sum() == target
        RuleKind.DIFF -> when (values.size) {
            2 -> abs(values[0] - values[1]) == target
            3 -> {
                val ordered = values.sortedDescending()
                ordered[0] - ordered[1] - ordered[2] == target
            }
            else -> false
        }
        RuleKind.PRODUCT -> when (values.size) {
            2 -> values[0] * values[1] == target
            3 -> values[0] * values[1] * values[2] == target
            else -> false
        }
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
            if (values.size == 2) {
                val hi = values.max(); val lo = values.min()
                "$hi - $lo = ${hi - lo}"
            } else {
                val ordered = values.sortedDescending()
                "${ordered[0]} - ${ordered[1]} - ${ordered[2]} = " +
                    "${ordered[0] - ordered[1] - ordered[2]}"
            }
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
            RuleKind.DIFF -> when (values.size) {
                2 -> abs(values[0] - values[1])
                3 -> values.sortedDescending().let { it[0] - it[1] - it[2] }
                else -> 0
            }
            RuleKind.PRODUCT -> when (values.size) {
                2 -> values[0] * values[1]
                3 -> values[0] * values[1] * values[2]
                else -> 0
            }
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

    /**
     * Tahtadaki tüm geçerli hamleler (geometri + matematik).
     * Arama, aynı hat üzerindeki seçimlerle sınırlandığı için 8x8 tahtada bile hızlıdır.
     */
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

        for (size in groupSizes.filter { it in 2..4 }) {
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
                // Yalnızca aynı hatta olan seçimler dallanmaya devam eder: 8x8 tahtada arama
                // kombinasyon patlaması yaşamaz, ipucu anında bulunur.
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
     * Tahta kurucu, hedefin BÜTÜN kombinasyonlarını eşit sıklıkta dağıtmak için
     * [combosFor] kullanır; bu fonksiyon ipucu/kurtarma için tek grup üretir.
     */
    fun operandsFor(kind: RuleKind, target: Int, groupSize: Int, random: Random): List<Int>? {
        if (groupSize !in 2..4) return null
        if (kind == RuleKind.SUM && groupSize !in 2..4) return null
        if (kind == RuleKind.DIFF && groupSize !in 2..3) return null
        if (kind == RuleKind.PRODUCT && groupSize !in 2..3) return null
        if (kind == RuleKind.QUOTIENT && groupSize != 2) return null
        val all = combosFor(kind, target, groupSize)
        if (all.isEmpty()) return null
        return all[random.nextInt(all.size)].shuffled(random)
    }

    /**
     * Bir tahtaya eşit sayıda yerleştirilebilecek kombinasyon seçkisi.
     *
     * 2'li gruplar için grup sayısı (groupCount), kombinasyon çeşidi sayısına tam bölünmelidir.
     * Böylece 4 toplamında 0+4, 1+3 ve 2+2 aynı sayıda bulunur; 1+4 gibi tek bir çift tahtayı
     * kaplamaz. Tam bölünmüyorsa çeşit sayısı, bölünebilen en büyük değere indirilir.
     */
    fun balancedCombos(
        kind: RuleKind,
        target: Int,
        groupSize: Int,
        groupCount: Int
    ): List<List<Int>> {
        val all = combosFor(kind, target, groupSize)
        if (all.isEmpty() || groupCount <= 0 || groupCount % all.size == 0) return all
        val usable = (minOf(all.size, groupCount) downTo 1).firstOrNull { groupCount % it == 0 }
            ?: return all
        if (usable == all.size) return all
        // Destenin başından değil, tüm listeden eşit aralıklarla seç: büyük/küçük
        // kombinasyonlar da tahtada görünsün.
        return (0 until usable).map { i -> all[i * all.size / usable] }
    }

    /**
     * Hedefi sağlayan TÜM kombinasyonlar (sırasız). Tahta üretiminde deste olarak kullanılır:
     * böylece 1+4, 2+3, 0+5 ... hepsi aynı oranda tahtaya gelir.
     */
    fun combosFor(kind: RuleKind, target: Int, groupSize: Int): List<List<Int>> {
        if (kind == RuleKind.SUM) return sumCombos(target, groupSize)
        if (groupSize !in 2..3) return emptyList()
        return when (kind) {
            RuleKind.DIFF -> if (groupSize == 2) diffCombos(target) else diffTripleCombos(target)
            RuleKind.PRODUCT -> if (groupSize == 2) productCombos(target) else productTripleCombos(target)
            RuleKind.QUOTIENT -> if (groupSize == 2) quotientCombos(target) else emptyList()
            RuleKind.SUM -> emptyList()
        }
    }

    /** Farkı hedefe eşit, 0..100 aralığındaki ikililer. */
    private fun diffCombos(target: Int): List<List<Int>> =
        (0..max(0, min(target + 8, 20))).map { small -> listOf(small + target, small) }

    /** Üç sayıyla zincir çıkarma: büyük sayı − iki küçük sayı = hedef. */
    private fun diffTripleCombos(target: Int): List<List<Int>> {
        if (target < 0) return emptyList()
        val out = mutableListOf<List<Int>>()
        for (b in 1..20) {
            for (c in 1..20) {
                val a = target + b + c
                if (a in 1..100) out.add(listOf(a, b, c).sorted())
            }
        }
        return out.distinctBy { it }
    }

    /** Çarpımı hedefe eşit, 1..12 aralığındaki ikililer. */
    private fun productCombos(target: Int): List<List<Int>> =
        (1..12).mapNotNull { d ->
            if (target % d == 0 && target / d <= 12) listOf(d, target / d) else null
        }.distinctBy { it.sorted() }

    /** Üç sayıyla çarpma: a × b × c = hedef. Sayılar 100'ü geçmez. */
    private fun productTripleCombos(target: Int): List<List<Int>> {
        if (target <= 0) return emptyList()
        val out = mutableListOf<List<Int>>()
        for (a in 1..12) {
            if (target % a != 0) continue
            val rest = target / a
            for (b in 1..12) {
                if (rest % b != 0) continue
                val c = rest / b
                if (c in 1..100) out.add(listOf(a, b, c).sorted())
            }
        }
        return out.distinctBy { it }
    }

    /** Kalansız bölme ikilileri; bölünen 100'ü geçmez. */
    private fun quotientCombos(target: Int): List<List<Int>> =
        (1..10).mapNotNull { divisor ->
            val dividend = target * divisor
            if (dividend <= 100 && divisor > 0) listOf(dividend, divisor) else null
        }.distinctBy { it.sorted() }

    /** Toplamı hedefe eşit, azalmayan tüm sayı üçlüleri/dörtlüleri. */
    private fun sumCombos(target: Int, size: Int): List<List<Int>> {
        if (target < 0 || size !in 2..4) return emptyList()
        val out = mutableListOf<List<Int>>()
        val current = ArrayList<Int>(size)
        fun walk(minValue: Int, left: Int) {
            val slots = size - current.size
            if (slots == 1) {
                if (left >= minValue) {
                    current.add(left)
                    out.add(current.toList())
                    current.removeAt(current.size - 1)
                }
                return
            }
            var v = minValue
            while (v <= left) {
                current.add(v)
                walk(v, left - v)
                current.removeAt(current.size - 1)
                v++
            }
        }
        walk(0, target)
        return out
    }

    /** Çarpım hedefi tahtaya yazılabilir mi? Müfredat kurulumunda kullanılır. */
    fun isProductTargetReachable(target: Int): Boolean = combosFor(RuleKind.PRODUCT, target, 2).isNotEmpty()
}
