package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.RuleKind
import kotlin.math.max

/**
 * Puanlama.
 *
 * - Her doğru eşleşme puan getirir; üst üste doğru hamle serisi bonusu büyütür.
 * - 3'lü ve 4'lü kombinasyonlar KOMBO bonusu alır: 3 taş +5, 4 taş +12. Ekranda kocaman yazar.
 * - Yanlış eşleşme bölüm puanından 2 puan götürür (0'ın altına inmez).
 * - Bir günde gösterge için 100 puanlık tavan korunur; bölüm puanı tavandan bağımsızdır.
 */
object ScoreEngine {

    const val DAILY_CAP = 100

    /** Yanlış eşleşmenin bölüm puanından götürdüğü puan. */
    const val WRONG_PENALTY = 2

    /** Üst üste doğrular için eklenen puan; en fazla +8. */
    fun streakBonus(streak: Int): Int = max(0, streak - 1).coerceAtMost(8)

    /** Tek bir doğru eşleşmenin ham puanı. */
    fun matchPoints(spec: LevelSpec, groupSize: Int, streak: Int, doubleNext: Boolean): Int {
        val base = when (spec.ruleKind) {
            RuleKind.SUM -> 4
            RuleKind.DIFF -> 5
            RuleKind.PRODUCT -> 6
            RuleKind.QUOTIENT -> 7
        }
        val raw = base + comboBonus(groupSize) + sizeBonus(spec.totalCells) + streakBonus(streak)
        return if (doubleNext) raw * 2 else raw
    }

    /** 3'lü ve 4'lü kombinasyonun ekstra puanı. */
    fun comboBonus(groupSize: Int): Int = when (groupSize) {
        3 -> 5
        4 -> 12
        else -> 0
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
            else -> 0
        }
        return base + combo + streakBonus(streak)
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
