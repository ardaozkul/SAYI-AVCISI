package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.EndlessMission
import com.sayiavcisi.app.model.RuleKind
import kotlin.random.Random

/**
 * Sonsuz Av'ın anlık görev üretecisi.
 *
 * Görev sırası sabit bir başlık değil, sırayla değişen dört av türüdür:
 * 1) Toplama — en az 4 sayı
 * 2) Çıkarma — 3 sayı
 * 3) Çarpma — 3 sayı
 * 4) Bölme — 2 sayı
 *
 * Her görevde tahtaya yerleştirilecek bir örnek kombinasyon da üretilir; böylece
 * 6×100 tahtada çocuk "görev var ama yapılacak sayı yok" durumuna düşmez.
 */
object EndlessMissionFactory {

    private val order = listOf(
        RuleKind.SUM,
        RuleKind.DIFF,
        RuleKind.PRODUCT,
        RuleKind.QUOTIENT
    )

    private val productTargets = listOf(
        12, 18, 20, 24, 27, 30, 32, 36, 40, 42, 45, 48, 50,
        54, 56, 60, 63, 64, 70, 72, 75, 80, 81, 84, 90, 96, 100
    )

    fun first(random: Random = Random.Default): EndlessMission =
        missionFor(order[random.nextInt(order.size)], random)

    fun next(previous: EndlessMission?, random: Random = Random.Default): EndlessMission {
        if (previous == null) return first(random)
        val index = order.indexOf(previous.kind)
        val kind = order[(index + 1) % order.size]
        return missionFor(kind, random)
    }

    private fun missionFor(kind: RuleKind, random: Random): EndlessMission = when (kind) {
        RuleKind.SUM -> {
            val target = random.nextInt(12, 61)
            val safe = RuleEngine.combosFor(RuleKind.SUM, target, 4)
                .filter { combo -> combo.all { it in 1..100 } }
            val seed = safe.randomOrNull(random) ?: listOf(1, 2, 3, target - 6)
            EndlessMission(
                kind = kind,
                target = target,
                groupSizes = listOf(4),
                title = "4 sayıyı topla · $target",
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
                title = "3 sayıyı çıkar · $target",
                seedOperands = seed.shuffled(random)
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
                title = "3 sayıyı çarp · $target",
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
                title = "2 sayıyı böl · $target",
                seedOperands = seed.shuffled(random)
            )
        }
    }
}
