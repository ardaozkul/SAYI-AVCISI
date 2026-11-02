package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.RuleKind
import kotlin.math.max

/**
 * Puanlama.
 *
 * - Her doğru eşleşme puan getirir; üst üste doğru hamle serisi KATLANARAK büyür.
 *   Seri 3'te bir ikiye katlanır: 3 doğru ×2, 6 doğru ×4, 9 doğru ×8, 12 doğru ×16 ...
 *   Bir üst sınır yoktur; sayaç yalnızca Int taşmasına karşı doyuma ulaşır.
 * - Kombolar da sınırlanmaz: 3'ten 8 taşa kadar büyüyen kombolar giderek daha çok bonus alır.
 *   3 taş +5, 4 taş +12, 5 taş +18 ... 8 taş +36. Ekranda kocaman yazar.
 * - Yanlış eşleşme bölüm puanından 2 puan götürür (0'ın altına inmez).
 * - Bir günde gösterge için 100 puanlık tavan korunur; bölüm puanı tavandan bağımsızdır.
 */
object ScoreEngine {

    const val DAILY_CAP = 100

    /** Yanlış eşleşmenin bölüm puanından götürdüğü puan. */
    const val WRONG_PENALTY = 2

    /**
     * Ardışık doğru serisinin puan çarpanı. Her 3 doğruda bir ikiye katlanır ve
     * oyun boyunca sınırsızdır. Pratikte yalnızca Int taşmasını önlemek için doyum yapar.
     */
    fun comboMultiplier(streak: Int): Int {
        if (streak <= 1) return 1
        val doublings = streak / 3
        return if (doublings >= 30) Int.MAX_VALUE / 8 else 1 shl doublings
    }

    /** Ekranda yazan kombo adı; seri büyüdükçe daha coşkulu olur. */
    fun comboTitle(streak: Int): String = when {
        streak < 2 -> ""
        streak < 4 -> "KOMBO"
        streak < 7 -> "SÜPER KOMBO"
        streak < 10 -> "MEGA KOMBO"
        streak < 14 -> "EFSANE KOMBO"
        streak < 20 -> "İNANILMAZ KOMBO"
        else -> "SAYI AVCISI"
    }

    /** Eski seri bonusunun yerini çarpan aldı; dış çağrılar için 1 tabanlı çarpan döner. */
    fun streakBonus(streak: Int): Int = comboMultiplier(streak)

    /** Tek bir doğru eşleşmenin ham puanı. */
    fun matchPoints(spec: LevelSpec, groupSize: Int, streak: Int, doubleNext: Boolean): Int {
        val base = when (spec.ruleKind) {
            RuleKind.SUM -> 4
            RuleKind.DIFF -> 5
            RuleKind.PRODUCT -> 6
            RuleKind.QUOTIENT -> 7
        }
        val raw = base + comboBonus(groupSize) + sizeBonus(spec.totalCells)
        val multiplied = safeMultiply(raw, comboMultiplier(streak))
        return if (doubleNext) safeMultiply(multiplied, 2) else multiplied
    }

    /** Oyun puanı taşmasın: aşırı büyük seride bile makul bir tavanda durur. */
    private fun safeMultiply(value: Int, factor: Int): Int {
        if (value <= 0 || factor <= 1) return value
        val limit = Int.MAX_VALUE / 8
        return if (value > limit / factor) limit else value * factor
    }

    /** Büyüyen kombonun ekstra puanı: 3 taş +5'ten başlar, her ek taşta büyümeye devam eder. */
    fun comboBonus(groupSize: Int): Int = when {
        groupSize < 3 -> 0
        groupSize == 3 -> 5
        groupSize == 4 -> 12
        else -> 12 + (groupSize - 4) * 6
    }

    /**
     * Sonsuz Av'da kazanılan YEŞİL puan.
     * Normal bölümlerin sarı puanından ayrı tutulur; günlük tavana bağlı değildir.
     */
    fun endlessMissionPoints(kind: RuleKind, groupSize: Int, streak: Int): Int {
        val base = when (kind) {
            RuleKind.SUM -> 8
            RuleKind.DIFF -> 10
            RuleKind.PRODUCT -> 12
            RuleKind.QUOTIENT -> 9
        }
        val combo = when (groupSize) {
            3 -> 6
            4 -> 12
            5 -> 18
            6 -> 24
            7 -> 30
            8 -> 36
            else -> 0
        }
        return safeMultiply(base + combo, comboMultiplier(streak))
    }

    /** Büyük tahtalarda avlamak biraz daha değerli. */
    fun sizeBonus(totalCells: Int): Int = when {
        totalCells >= 200 -> 6
        totalCells >= 50 -> 2
        totalCells >= 36 -> 1
        else -> 0
    }

    /**
     * Günlük tavana göre ödüllendirilebilir kısmı hesaplar.
     * @return (verilecekPuan, yeniGunlukKazanilan)
     */
    fun applyDailyCap(dailyEarned: Int, rawPoints: Int): Pair<Int, Int> {
        val room = (DAILY_CAP - dailyEarned).coerceAtLeast(0)
        val granted = rawPoints.coerceAtMost(room)
        return Pair(granted, (dailyEarned + granted).coerceAtMost(DAILY_CAP))
    }

    fun jokerCost(type: JokerType): Int = type.cost

    /** Bölüm bitirme ödülü: altın ve (kusursuzsa) elmas. */
    fun completionRewards(spec: LevelSpec, flawless: Boolean, usedRescue: Boolean): Pair<Int, Int> {
        val gold = 10 + (spec.totalCells / 10) + spec.stageIndex
        val diamonds = when {
            flawless && !usedRescue -> 2
            flawless -> 1
            else -> 0
        }
        return Pair(gold, diamonds)
    }

    /** Süre baskısı olan bölümlerde kalan süreye göre ek puan. */
    fun timeBonus(remainingSeconds: Int?): Int = when {
        remainingSeconds == null -> 0
        remainingSeconds >= 120 -> 5
        remainingSeconds >= 60 -> 3
        remainingSeconds > 0 -> 1
        else -> 0
    }

    /** Günlük tavan yüzdesi: ana ekrandaki ilerleme çubuğu. */
    fun dailyProgressPercent(dailyEarned: Int): Int =
        (dailyEarned * 100 / DAILY_CAP).coerceIn(0, 100)
}
