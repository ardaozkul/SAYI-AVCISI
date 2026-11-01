package com.sayiavcisi.app.data

import android.content.Context
import android.content.SharedPreferences
import com.sayiavcisi.app.model.OperationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cihaz içi çevrimdışı (offline) veri, ilerleme, ebeveyn istatistikleri ve yarım kalan oyun kaydı yöneticisi.
 *
 * Ebeveyn Panelinde gösterilen tüm metrikler burada kalıcı olarak saklanır:
 * - Hangi günler ve bölümler tamamlandı
 * - Toplam kaç işlem yaptı
 * - Toplam doğru ve yanlış cevap sayısı
 * - Toplama, Çıkarma, Çarpma, Bölme ayrı ayrı başarı oranları
 * - En çok zorlandığı işlemler listesi
 * - Çarpım tablosunda hangi sayılarda (0..10) zorlandığı
 * - Ortalama cevap süresi (saniye)
 * - Günlük oyun süresi (dakika/saniye)
 * - Ebeveyn ayarları: Süre sınırı modu (Süreli / Rahat Süresiz Mod, süre çarpanı) ve Ses Açık/Kapalı
 */
data class ParentStatsSnapshot(
    val completedLevelIds: Set<String>,
    val completedDays: Set<Int>,
    val dailyScore: Int,
    val totalGoldCoins: Int,
    val totalDiamonds: Int,
    val totalOperations: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val additionCorrect: Int,
    val additionTotal: Int,
    val subtractionCorrect: Int,
    val subtractionTotal: Int,
    val multiplicationCorrect: Int,
    val multiplicationTotal: Int,
    val divisionCorrect: Int,
    val divisionTotal: Int,
    val hardestOperations: List<Pair<String, Int>>,
    val multiplicationStruggleByNumber: Map<Int, Int>,
    val averageResponseTimeSeconds: Float,
    val dailyPlayTimeSeconds: Long,
    val soundEnabled: Boolean,
    val relaxedUntimedMode: Boolean,
    val customTimeAdditionSeconds: Int,
    val lastActiveLevelId: String?,
    /** Tablet / geniş ekran tahta sütunu. 0 = otomatik. */
    val boardColumns: Int = 0,
    /** Sonsuz Av'ın ayrı yeşil puan kayıtları. */
    val endlessBestScore: Int = 0,
    val endlessTotalScore: Int = 0,
    val endlessMissions: Int = 0,
    val endlessPlays: Int = 0,
    val endlessBestStreak: Int = 0
) {
    fun successRatePercent(correct: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((correct.toFloat() / total.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun hasOperationData(total: Int): Boolean = total > 0

    val additionRate: Int get() = successRatePercent(additionCorrect, additionTotal)
    val subtractionRate: Int get() = successRatePercent(subtractionCorrect, subtractionTotal)
    val multiplicationRate: Int get() = successRatePercent(multiplicationCorrect, multiplicationTotal)
    val divisionRate: Int get() = successRatePercent(divisionCorrect, divisionTotal)

    companion object {
        /** Bir günde kazanılabilecek en fazla puan. */
        const val DAILY_CAP = 100
    }
}

class LocalProgressStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sayi_avcisi_offline_prefs", Context.MODE_PRIVATE)

    /**
     * "Günlük" metrikler (puan tavanı 100 ve günlük oyun süresi) takvim günü değişince sıfırlanır.
     */
    private fun todayStamp(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun ensureDailyRollover() {
        val stored = prefs.getString("daily_stamp", null)
        val today = todayStamp()
        if (stored != today) {
            prefs.edit()
                .putString("daily_stamp", today)
                .putInt("daily_earned", 0)
                .putInt("daily_spent", 0)
                .putInt("daily_score", 0)
                .putLong("daily_play_seconds", 0L)
                .apply()
        }
    }

    /** Bugün kazanılan brüt puan; günde en fazla 100 puan birikebilir. */
    fun grossEarnedToday(): Int {
        ensureDailyRollover()
        return prefs.getInt("daily_earned", 0).coerceIn(0, ParentStatsSnapshot.DAILY_CAP)
    }

    fun addGrossEarned(delta: Int) {
        if (delta <= 0) return
        ensureDailyRollover()
        val newGross = (prefs.getInt("daily_earned", 0) + delta).coerceAtMost(ParentStatsSnapshot.DAILY_CAP)
        prefs.edit().putInt("daily_earned", newGross).apply()
    }

    /** Jokerler puandan götürür, brüt tavanı etkilemez. */
    fun addJokerSpending(amount: Int) {
        if (amount <= 0) return
        ensureDailyRollover()
        prefs.edit().putInt("daily_spent", prefs.getInt("daily_spent", 0) + amount).apply()
    }

    /** Ekranda gösterilen günlük puan: kazanılan - jokerlerde harcanan, en az 0. */
    fun dailyNetScore(): Int {
        ensureDailyRollover()
        return (grossEarnedToday() - prefs.getInt("daily_spent", 0)).coerceAtLeast(0)
    }

    fun loadSnapshot(): ParentStatsSnapshot {
        ensureDailyRollover()
        val completedLevels = prefs.getStringSet("completed_levels", emptySet()) ?: emptySet()
        val completedDays = (1..com.sayiavcisi.app.engine.CurriculumRepository.dayCount).filter { day ->
            com.sayiavcisi.app.engine.CurriculumRepository.isDayCompleted(day, completedLevels)
        }.toSet()

        val multStruggles = mutableMapOf<Int, Int>()
        for (n in 0..10) {
            val count = prefs.getInt("mult_struggle_$n", 0)
            if (count > 0) {
                multStruggles[n] = count
            }
        }

        val rawHardOps = prefs.getString("hard_ops_log", "") ?: ""
        val hardOpsMap = mutableMapOf<String, Int>()
        if (rawHardOps.isNotBlank()) {
            rawHardOps.split("|").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val expr = parts[0]
                    val cnt = parts[1].toIntOrNull() ?: 0
                    if (expr.isNotBlank() && cnt > 0) {
                        hardOpsMap[expr] = cnt
                    }
                }
            }
        }

        val totalOps = prefs.getInt("total_ops", 0)
        val totalResponseMs = prefs.getLong("total_response_ms", 0L)
        val avgSec = if (totalOps > 0) {
            (totalResponseMs.toFloat() / totalOps.toFloat()) / 1000f
        } else {
            0f
        }

        return ParentStatsSnapshot(
            completedLevelIds = completedLevels,
            completedDays = completedDays,
            dailyScore = dailyNetScore(),
            totalGoldCoins = prefs.getInt("gold_coins", 0),
            totalDiamonds = prefs.getInt("diamonds", 0),
            totalOperations = totalOps,
            totalCorrect = prefs.getInt("total_correct", 0),
            totalWrong = prefs.getInt("total_wrong", 0),
            additionCorrect = prefs.getInt("add_correct", 0),
            additionTotal = prefs.getInt("add_total", 0),
            subtractionCorrect = prefs.getInt("sub_correct", 0),
            subtractionTotal = prefs.getInt("sub_total", 0),
            multiplicationCorrect = prefs.getInt("mul_correct", 0),
            multiplicationTotal = prefs.getInt("mul_total", 0),
            divisionCorrect = prefs.getInt("div_correct", 0),
            divisionTotal = prefs.getInt("div_total", 0),
            hardestOperations = hardOpsMap.entries.sortedByDescending { it.value }.take(6).map { Pair(it.key, it.value) },
            multiplicationStruggleByNumber = multStruggles,
            averageResponseTimeSeconds = avgSec,
            dailyPlayTimeSeconds = prefs.getLong("daily_play_seconds", 0L),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            relaxedUntimedMode = prefs.getBoolean("relaxed_untimed_mode", true),
            customTimeAdditionSeconds = prefs.getInt("custom_time_addition_sec", 0),
            lastActiveLevelId = prefs.getString("last_active_level_id", "A1"),
            boardColumns = prefs.getInt("board_columns", 0).coerceIn(0, 20),
            endlessBestScore = prefs.getInt("endless_best_score", 0),
            endlessTotalScore = prefs.getInt("endless_total_score", 0),
            endlessMissions = prefs.getInt("endless_missions", 0),
            endlessPlays = prefs.getInt("endless_plays", 0),
            endlessBestStreak = prefs.getInt("endless_best_streak", 0)
        )
    }

    fun addRewards(goldDelta: Int, diamondDelta: Int) {
        val g = prefs.getInt("gold_coins", 0) + goldDelta
        val d = prefs.getInt("diamonds", 0) + diamondDelta
        prefs.edit()
            .putInt("gold_coins", g.coerceAtLeast(0))
            .putInt("diamonds", d.coerceAtLeast(0))
            .apply()
    }

    fun markLevelCompleted(levelId: String) {
        val current = (prefs.getStringSet("completed_levels", emptySet()) ?: emptySet()).toMutableSet()
        current.add(levelId)
        prefs.edit()
            .putStringSet("completed_levels", current)
            .remove("resume_$levelId")
            .apply()
    }

    /** Yarım kalan oyun tahtası kaydını (kaldığı yerden devam) saklar. */
    fun saveGameSnapshot(levelId: String, encoded: String) {
        prefs.edit().putString("resume_$levelId", encoded).apply()
    }

    fun loadGameSnapshot(levelId: String): String? = prefs.getString("resume_$levelId", null)

    fun clearGameSnapshot(levelId: String) {
        prefs.edit().remove("resume_$levelId").apply()
    }

    fun saveLastActiveLevel(levelId: String) {
        prefs.edit().putString("last_active_level_id", levelId).apply()
    }

    fun recordOperationAttempt(
        operation: OperationType,
        isCorrect: Boolean,
        operands: List<Int>,
        targetPrompt: String,
        responseTimeMs: Long
    ) {
        val editor = prefs.edit()
        val totalOps = prefs.getInt("total_ops", 0) + 1
        editor.putInt("total_ops", totalOps)
        editor.putLong("total_response_ms", prefs.getLong("total_response_ms", 0L) + responseTimeMs.coerceIn(200L, 60000L))

        if (isCorrect) {
            editor.putInt("total_correct", prefs.getInt("total_correct", 0) + 1)
        } else {
            editor.putInt("total_wrong", prefs.getInt("total_wrong", 0) + 1)
        }

        when (operation) {
            OperationType.ADDITION -> {
                editor.putInt("add_total", prefs.getInt("add_total", 0) + 1)
                if (isCorrect) editor.putInt("add_correct", prefs.getInt("add_correct", 0) + 1)
            }
            OperationType.SUBTRACTION -> {
                editor.putInt("sub_total", prefs.getInt("sub_total", 0) + 1)
                if (isCorrect) editor.putInt("sub_correct", prefs.getInt("sub_correct", 0) + 1)
            }
            OperationType.MULTIPLICATION -> {
                editor.putInt("mul_total", prefs.getInt("mul_total", 0) + 1)
                if (isCorrect) {
                    editor.putInt("mul_correct", prefs.getInt("mul_correct", 0) + 1)
                } else {
                    operands.forEach { n ->
                        if (n in 0..10) {
                            val key = "mult_struggle_$n"
                            editor.putInt(key, prefs.getInt(key, 0) + 1)
                        }
                    }
                }
            }
            OperationType.DIVISION -> {
                editor.putInt("div_total", prefs.getInt("div_total", 0) + 1)
                if (isCorrect) editor.putInt("div_correct", prefs.getInt("div_correct", 0) + 1)
            }
        }

        if (!isCorrect) {
            val label = "${operation.title} ($targetPrompt)"
            val raw = prefs.getString("hard_ops_log", "") ?: ""
            val map = mutableMapOf<String, Int>()
            if (raw.isNotBlank()) {
                raw.split("|").forEach { item ->
                    val p = item.split(":")
                    if (p.size == 2) map[p[0]] = p[1].toIntOrNull() ?: 0
                }
            }
            map[label] = (map[label] ?: 0) + 1
            val serialized = map.entries.joinToString("|") { "${it.key}:${it.value}" }
            editor.putString("hard_ops_log", serialized)
        }

        editor.apply()
    }

    fun addPlayTimeSeconds(seconds: Long) {
        if (seconds <= 0) return
        ensureDailyRollover()
        val current = prefs.getLong("daily_play_seconds", 0L)
        prefs.edit().putLong("daily_play_seconds", current + seconds).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun setRelaxedUntimedMode(enabled: Boolean) {
        prefs.edit().putBoolean("relaxed_untimed_mode", enabled).apply()
    }

    fun setCustomTimeAdditionSeconds(extraSeconds: Int) {
        prefs.edit().putInt("custom_time_addition_sec", extraSeconds.coerceIn(-15, 60)).apply()
    }

    /** 0 = otomatik; 6/9/12/15 gibi geniş ekran sütun sayısı. */
    fun setBoardColumns(columns: Int) {
        val safe = if (columns <= 0) 0 else columns.coerceIn(4, 20)
        prefs.edit().putInt("board_columns", safe).apply()
    }

    /** Sonsuz Av bitince yeşil puan kaydını ayrı tutar. */
    fun recordEndlessResult(score: Int, missions: Int, bestStreak: Int) {
        val safeScore = score.coerceAtLeast(0)
        val safeMissions = missions.coerceAtLeast(0)
        val best = prefs.getInt("endless_best_score", 0)
        prefs.edit()
            .putInt("endless_best_score", maxOf(best, safeScore))
            .putInt("endless_total_score", prefs.getInt("endless_total_score", 0) + safeScore)
            .putInt("endless_missions", prefs.getInt("endless_missions", 0) + safeMissions)
            .putInt("endless_plays", prefs.getInt("endless_plays", 0) + 1)
            .putInt(
                "endless_best_streak",
                maxOf(prefs.getInt("endless_best_streak", 0), bestStreak.coerceAtLeast(0))
            )
            .apply()
    }

    /** Ebeveyn paneli için günlük puan sayacını sıfırlar (yarın yeniden 100 puan hakkı). */
    fun resetDailyScoreOnly() {
        prefs.edit()
            .putInt("daily_earned", 0)
            .putInt("daily_spent", 0)
            .apply()
    }

    /** Tüm ilerlemeyi sıfırlar: yeni bir çocuk oyun oynayacaksa ebeveyn kullanır. */
    fun resetAllProgress() {
        val sound = prefs.getBoolean("sound_enabled", true)
        val relaxed = prefs.getBoolean("relaxed_untimed_mode", true)
        prefs.edit().clear()
            .putBoolean("sound_enabled", sound)
            .putBoolean("relaxed_untimed_mode", relaxed)
            .apply()
    }
}
