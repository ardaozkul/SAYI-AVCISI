package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.GeneratedBoard
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import kotlin.random.Random

/**
 * Oyuncuyu asla çözümsüz veya crashed bir durumla baş başa bırakmayan güvenlik ağları.
 */

/**
 * Garantili çözülebilir tahtayı tohum (seed) tabanlı üreten fabrika.
 * Tohum verildiğinde üretim tamamen deterministiktir; böylece yarım kalan oyun
 * aynı tahtaya geri yüklenabilir.
 */
object BoardFactory {

    const val MAX_GENERATION_ATTEMPTS = 8

    data class PreparedBoard(val board: GeneratedBoard, val seedUsed: Int)

    /**
     * Doğrulaması başarısız olan üretim denemesini farklı tohumla tekrar dener,
     * tüm denemeler düşerse serbest eşleştirme moduna (daha gevşek geometrik kural) geçer.
     */
    fun createReliably(spec: LevelSpec, seed: Int, attempts: Int = MAX_GENERATION_ATTEMPTS): PreparedBoard {
        var lastError: Throwable? = null
        for (attempt in 0 until attempts) {
            val candidateSeed = seed + attempt * 7919
            val candidateSpec = if (attempt < attempts / 2) spec
            else spec.copy(matchRuleMode = MatchRuleMode.FREE)
            try {
                return PreparedBoard(
                    BoardGeneratorAndSolver.generateVerifiedBoard(candidateSpec, Random(candidateSeed)),
                    candidateSeed
                )
            } catch (t: Throwable) {
                lastError = t
            }
        }
        error("Çözülebilir tahta üretilemedi: ${spec.levelId} (${lastError?.message})")
    }
}

/**
 * ALTIN KURTARMA'nın ne zaman devreye gireceğini belirleyen saf karar fonksiyonu (Bölüm 9).
 *
 * Kural: Üç joker de tükenmişse ve tahtada geçerli hamle kalmadıysa sistem sessizce otomatik çalışır.
 * Joker hala varsa önce çocuğu joker kullanmaya yönlendirir; çocuk kısa bir süre hamle yapamazsa
 * yine mahsur kalmaması için kurtarma devreye girer.
 */
object GoldenRescuePolicy {

    const val GRACE_SECONDS_WITH_JOKER = 25

    enum class Decision {
        NOTHING_TO_DO,
        USE_GOLDEN_RESCUE_NOW,
        GUIDE_TO_JOKER,
        WAIT
    }

    fun decide(
        hasValidMove: Boolean,
        targetsRemaining: Int,
        jokersRemaining: Int,
        secondsSinceStuck: Int
    ): Decision {
        if (targetsRemaining <= 0 || hasValidMove) return Decision.NOTHING_TO_DO
        if (jokersRemaining <= 0) return Decision.USE_GOLDEN_RESCUE_NOW
        return if (secondsSinceStuck >= GRACE_SECONDS_WITH_JOKER) {
            Decision.USE_GOLDEN_RESCUE_NOW
        } else {
            Decision.GUIDE_TO_JOKER
        }
    }

    fun jokersRemaining(hint: Int, shuffle: Int, doubleScore: Int): Int =
        (hint + shuffle + doubleScore).coerceAtLeast(0)
}
