package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.LevelSpec

/**
 * İlerleme kuralları: bölümler sırayla açılır, günler birbirine bağlıdır.
 * Harita ekranı ve birim testleri aynı kaynağı kullanır.
 */
object ProgressRules {

    fun isLevelUnlocked(spec: LevelSpec, completedLevelIds: Set<String>): Boolean {
        val dayLevels = CurriculumRepository.levelsForDay(spec.dayNumber)
        val index = dayLevels.indexOfFirst { it.levelId == spec.levelId }
        if (index <= 0) return isDayUnlocked(spec.dayNumber, completedLevelIds)
        return completedLevelIds.contains(dayLevels[index - 1].levelId)
    }

    fun isDayUnlocked(day: Int, completedLevelIds: Set<String>): Boolean {
        if (day <= 1) return true
        val previous = CurriculumRepository.levelsForDay(day - 1)
        return previous.any { completedLevelIds.contains(it.levelId) } ||
            CurriculumRepository.isDayCompleted(day - 1, completedLevelIds)
    }

    fun isLevelCompleted(spec: LevelSpec, completedLevelIds: Set<String>): Boolean =
        completedLevelIds.contains(spec.levelId)

    /** Çocuğun kaldığı yer: tamamlanmamış ilk bölüm. */
    fun currentLevelId(completedLevelIds: Set<String>): String =
        CurriculumRepository.allLevels.firstOrNull { !completedLevelIds.contains(it.levelId) }?.levelId
            ?: CurriculumRepository.allLevels.last().levelId

    fun dayProgress(day: Int, completedLevelIds: Set<String>): Float {
        val levels = CurriculumRepository.levelsForDay(day)
        val done = levels.count { completedLevelIds.contains(it.levelId) }
        return if (levels.isEmpty()) 0f else done.toFloat() / levels.size.toFloat()
    }

    fun completedLevelCount(completedLevelIds: Set<String>): Int =
        CurriculumRepository.allLevels.count { completedLevelIds.contains(it.levelId) }
}
