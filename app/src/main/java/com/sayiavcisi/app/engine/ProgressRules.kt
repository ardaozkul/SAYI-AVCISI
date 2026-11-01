package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.LevelSpec

/**
 * İlerleme kuralları.
 *
 * Bütün bölümler baştan açıktır: çocuk toplama, çıkarma, çarpım tablosu ve bölme arasında
 * serbestçe gezebilir; oyunu denemek isteyen veli de istediği bölüme girebilir.
 * Tamamlanan bölümler listede ✓ olarak işaretlenir.
 */
object ProgressRules {

    fun isLevelUnlocked(spec: LevelSpec, completedLevelIds: Set<String>): Boolean = true

    fun isDayUnlocked(day: Int, completedLevelIds: Set<String>): Boolean = true

    fun isLevelCompleted(spec: LevelSpec, completedLevelIds: Set<String>): Boolean =
        completedLevelIds.contains(spec.levelId)

    /** Çocuğun kaldığı yer: tamamlanmamış ilk klasik bölüm. */
    fun currentLevelId(completedLevelIds: Set<String>): String =
        CurriculumRepository.standardLevels.firstOrNull { !completedLevelIds.contains(it.levelId) }?.levelId
            ?: CurriculumRepository.standardLevels.last().levelId

    fun dayProgress(day: Int, completedLevelIds: Set<String>): Float {
        if (day == 5) return 0f
        val levels = CurriculumRepository.levelsForDay(day)
        val done = levels.count { completedLevelIds.contains(it.levelId) }
        return if (levels.isEmpty()) 0f else done.toFloat() / levels.size.toFloat()
    }

    fun completedLevelCount(completedLevelIds: Set<String>): Int =
        CurriculumRepository.standardLevels.count { completedLevelIds.contains(it.levelId) }
}
