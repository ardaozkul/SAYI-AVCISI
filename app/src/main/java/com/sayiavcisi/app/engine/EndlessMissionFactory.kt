package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.EndlessMission
import com.sayiavcisi.app.model.RuleKind
import kotlin.random.Random

/**
 * Sonsuz Av'ın anlık görev üretecisi.
 *
 * Görevler DENGELİ TORBA ile gelir: her dört görevde toplama, çıkarma, çarpma ve bölme
 * tam olarak birer kez çıkar. Torba her dolduğunda karıştırılır; böylece uzun oyunda
 * hiçbir işlem diğerinden daha sık görünmez.
 *
 * Her görevde tahtaya yerleştirilecek bir örnek kombinasyon da üretilir; böylece
 * 8×120 tahtada çocuk "görev var ama yapılacak sayı yok" durumuna düşmez.
 *
 * Görev başlıkları kelime yerine açık formüldür: "a × b × c = 21", "a ÷ b = 4".
 * Komşuluk aranmaz; görevdeki taşlar tahtanın her yerinden seçilebilir.
 */
object EndlessMissionFactory {

    private val order = listOf(
        RuleKind.SUM,
        RuleKind.DIFF,
        RuleKind.PRODUCT,
        RuleKind.QUOTIENT
    )

    /**
     * Dengeli görev torbası: her turda dört işlem birer kez, rastgele sırayla.
     * Aynı işlem iki tur üst üste gelebilir ama tur içinde sayılar eşittir.
     */
    class MissionBag(private val random: Random) {
        private val pool = ArrayDeque<RuleKind>()

        fun draw(): RuleKind {
            if (pool.isEmpty()) pool.addAll(order.shuffled(random))
            return pool.removeFirst()
        }
    }

    private val productTargets = listOf(
        12, 18, 20, 24, 27, 30, 32, 36, 40, 42, 45, 48, 50,
        54, 56, 60, 63, 64, 70, 72, 75, 80, 81, 84, 90, 96, 100
    )

    /** Tek seferlik rastgele görev; testler ve eski çağrılar için korunur. */
    fun first(random: Random = Random.Default): EndlessMission =
        missionFor(order[random.nextInt(order.size)], random)

    /** Dengeli torbadan yeni görev üretir. */
    fun next(bag: MissionBag, random: Random = Random.Default): EndlessMission =
        missionFor(bag.draw(), random)

    fun missionFor(kind: RuleKind, random: Random): EndlessMission = when (kind) {
        RuleKind.SUM -> {
            val target = random.nextInt(12, 61)
            val safe = RuleEngine.combosFor(RuleKind.SUM, target, 4)
                .filter { combo -> combo.all { it in 1..100 } }
            val seed = safe.randomOrNull(random) ?: listOf(1, 2, 3, target - 6)
            EndlessMission(
                kind = kind,
                target = target,
                groupSizes = listOf(4),
                title = formula(kind, 4, target),
                seedOperands = seed.shuffled(random)
            )
        }

        RuleKind.DIFF -> {
            val target = random.nextInt(2, 16)
            val safe = RuleEngine.combosFor(RuleKind.DIFF, target, 3)
                .filter { combo -> combo.all { it in 1..100 } }
            val seed = safe.randomOrNull(random) ?: listOf(target + 2, 1, 1)
            EndlessMission(
                kind = kind,
                target = target,
                groupSizes = listOf(3),
                // Çıkarmada büyük sayı önce seçilmeli; tahtaya da büyükten küçüğe dizilir.
                title = formula(kind, 3, target),
                seedOperands = seed.sortedDescending()
            )
        }

        RuleKind.PRODUCT -> {
            val target = productTargets.random(random)
            val safe = RuleEngine.combosFor(RuleKind.PRODUCT, target, 3)
                .filter { combo -> combo.all { it in 1..100 } }
            val seed = safe.randomOrNull(random) ?: listOf(2, 2, 3)
            EndlessMission(
                kind = kind,
                target = target,
                groupSizes = listOf(3),
                title = formula(kind, 3, target),
                seedOperands = seed.shuffled(random)
            )
        }

        RuleKind.QUOTIENT -> {
            val target = random.nextInt(2, 13)
            val safe = RuleEngine.combosFor(RuleKind.QUOTIENT, target, 2)
                .filter { combo -> combo.all { it in 1..100 } }
            val seed = safe.randomOrNull(random) ?: listOf(target * 2, 2)
            EndlessMission(
                kind = kind,
                target = target,
                groupSizes = listOf(2),
                // Bölmede bölünen önce seçilmeli; tahtaya da bölünen solda gelir.
                title = formula(kind, 2, target),
                seedOperands = seed.sortedDescending()
            )
        }
    }

    /** "a + b + c + d = 57" gibi açık görev formülü. */
    private fun formula(kind: RuleKind, size: Int, target: Int): String {
        val letters = ('a'..'d').take(size.coerceIn(2, 4))
        val operator = when (kind) {
            RuleKind.SUM -> " + "
            RuleKind.DIFF -> " − "
            RuleKind.PRODUCT -> " × "
            RuleKind.QUOTIENT -> " ÷ "
        }
        return letters.joinToString(operator) + " = $target"
    }
}
