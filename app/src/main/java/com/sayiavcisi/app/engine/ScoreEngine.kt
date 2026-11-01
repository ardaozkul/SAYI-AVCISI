package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.JokerType

/**
 * Sayı Avcısı Puanlama ve Günlük Sınır Motoru (Bölüm 8, 9 ve 10).
 *
 * Kurallar:
 * - Bir günde en fazla 100 puan kazanılabilmeli (MAX_DAILY_SCORE = 100).
 * - Doğru iki sayılı eşleşme: +2
 * - Doğru üç sayılı eşleşme: +3
 * - Arka arkaya 5 doğru işlem: +5
 * - Arka arkaya 10 doğru işlem: +10
 * - Hatasız bölüm bitirme: +5
 * - Süre bonusu: +3
 * - Günün görevini tamamlama: +10
 * - Toplam günlük kazanım kesinlikle 100 puanı geçmemeli.
 * - Bonusların (Çifte hamle vb.) kullanımı günlük puan sınırını (100) değiştirmemeli.
 * - Yanlış eşleşme: -1 puan.
 * - Joker kullanımı ilgili puan kadar düşüş sağlamalı (İpucu: -5, Yeniden Düzenle: -10, Çifte Hamle: -5).
 * - Puan sıfırın altına düşmemeli (min 0).
 * - ALTIN KURTARMA için çocuk ekstra puan kaybetmemeli (0 puan düşüş).
 */
object ScoreEngine {

    const val MAX_DAILY_SCORE = 100
    const val MIN_DAILY_SCORE = 0

    const val POINTS_TWO_NUMBER_MATCH = 2
    const val POINTS_THREE_NUMBER_MATCH = 3
    const val BONUS_STREAK_5 = 5
    const val BONUS_STREAK_10 = 10
    const val BONUS_FLAWLESS_LEVEL = 5
    const val BONUS_TIME = 3
    const val BONUS_DAILY_QUEST_COMPLETE = 10
    const val PENALTY_WRONG_MATCH = 1

    data class ScoreUpdateResult(
        val newDailyScore: Int,
        val appliedDelta: Int,
        val streakBonusAwarded: Int,
        val newStreak: Int
    )

    /**
     * Doğru eşleşme sonrasında puanı hesaplar ve [0, 100] aralığına kenetler.
     */
    fun onCorrectMatch(
        currentDailyScore: Int,
        currentStreak: Int,
        operandCount: Int,
        isDoubleScoreActive: Boolean
    ): ScoreUpdateResult {
        val nextStreak = currentStreak + 1
        val basePoints = if (operandCount == 3) POINTS_THREE_NUMBER_MATCH else POINTS_TWO_NUMBER_MATCH
        val matchPoints = if (isDoubleScoreActive) basePoints * 2 else basePoints

        val streakBonus = when {
            nextStreak % 10 == 0 -> BONUS_STREAK_10
            nextStreak % 5 == 0 -> BONUS_STREAK_5
            else -> 0
        }

        val rawTargetScore = currentDailyScore + matchPoints + streakBonus
        val clampedScore = rawTargetScore.coerceIn(MIN_DAILY_SCORE, MAX_DAILY_SCORE)
        val actualDelta = clampedScore - currentDailyScore

        return ScoreUpdateResult(
            newDailyScore = clampedScore,
            appliedDelta = actualDelta,
            streakBonusAwarded = streakBonus,
            newStreak = nextStreak
        )
    }

    /**
     * Yanlış eşleşme sonrasında puanı 1 düşürür (0'ın altına inmez) ve seriyi sıfırlar.
     */
    fun onWrongMatch(currentDailyScore: Int): ScoreUpdateResult {
        val clampedScore = (currentDailyScore - PENALTY_WRONG_MATCH).coerceIn(MIN_DAILY_SCORE, MAX_DAILY_SCORE)
        val actualDelta = clampedScore - currentDailyScore
        return ScoreUpdateResult(
            newDailyScore = clampedScore,
            appliedDelta = actualDelta,
            streakBonusAwarded = 0,
            newStreak = 0
        )
    }

    /**
     * Joker kullanımında ilgili puanı düşürür (İpucu -5, Yeniden Düzenle -10, Çifte Hamle -5).
     * Puan sıfırın altına düşmez.
     */
    fun onJokerUsed(currentDailyScore: Int, jokerType: JokerType): Pair<Int, Int> {
        val clampedScore = (currentDailyScore - jokerType.cost).coerceIn(MIN_DAILY_SCORE, MAX_DAILY_SCORE)
        val delta = clampedScore - currentDailyScore
        return Pair(clampedScore, delta)
    }

    /**
     * Bölüm sonu bonuslarını (Hatasız bitirme +5, Süre bonusu +3, Günün görevini tamamlama +10)
     * günlük 100 puan sınırını aşmayacak şekilde uygular.
     */
    fun onLevelCompleted(
        currentDailyScore: Int,
        hadZeroMistakes: Boolean,
        finishedWithinTimeLimit: Boolean,
        completedFullDayQuest: Boolean
    ): Pair<Int, Int> {
        var bonus = 0
        if (hadZeroMistakes) bonus += BONUS_FLAWLESS_LEVEL
        if (finishedWithinTimeLimit) bonus += BONUS_TIME
        if (completedFullDayQuest) bonus += BONUS_DAILY_QUEST_COMPLETE

        val clampedScore = (currentDailyScore + bonus).coerceIn(MIN_DAILY_SCORE, MAX_DAILY_SCORE)
        val actualDelta = clampedScore - currentDailyScore
        return Pair(clampedScore, actualDelta)
    }
}
