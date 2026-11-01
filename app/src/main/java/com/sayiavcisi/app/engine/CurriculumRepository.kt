package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind

/**
 * Yeni müfredat: dört bölüm, hepsi baştan açık.
 *
 * 1) TOPLAMA  : 1'den 30'a ardışık hedefler. 7 toplama = "toplamı 7 yapan sayıları avla".
 *               6x6 tahta; 3'lü ve 4'lü kombinasyonlar desteklenir. İlk üç hedefte kombinasyon
 *               sayısı az olduğu için tahta küçültülür (4x4).
 * 2) ÇIKARMA : 1'den 12'ye fark hedefleri, iki blok.
 * 3) ÇARPMA  : Çarpım tablosu hedefleri (2, 4, 6 ... 36), iki blok.
 * 4) BÖLME   : Kalansız bölme hedefleri, iki blok.
 *
 * Çarpım tablosu dahil her bölüm açıktır; oyunu denemek isteyen çocuk/veli istediği yerden başlar.
 */
object CurriculumRepository {

    val dayTitles: Map<Int, String> = mapOf(
        1 to "Toplama",
        2 to "Çıkarma",
        3 to "Çarpma Tablosu",
        4 to "Bölme"
    )

    val daySubtitles: Map<Int, String> = mapOf(
        1 to "Toplamı 1'den 30'a kadar avla · 3'lü ve 4'lü kombinasyonlar",
        2 to "Farkı bul · iki sayı yeter",
        3 to "Çarpım tablosu · 2'den 36'ya",
        4 to "Kalansız bölme · iki sayı yeter"
    )

    val daySymbols: Map<Int, String> = mapOf(
        1 to "+",
        2 to "−",
        3 to "×",
        4 to "÷"
    )

    private val worldKeys = mapOf(
        1 to "toplama",
        2 to "cikarma",
        3 to "carpma",
        4 to "bolme"
    )

    private fun level(
        day: Int,
        stage: Int,
        ruleKind: RuleKind,
        target: Int,
        gridSize: Int,
        groupSizes: List<Int> = listOf(2)
    ): LevelSpec {
        val symbol = ruleKind.operation.symbol
        val title = when (ruleKind) {
            RuleKind.SUM -> "$target toplama"
            RuleKind.DIFF -> "$target çıkarma"
            RuleKind.PRODUCT -> "$target çarpma"
            RuleKind.QUOTIENT -> "$target bölme"
        }
        val subtitle = when (ruleKind) {
            RuleKind.SUM -> "Toplamı $target yapan komşu sayıları avla"
            RuleKind.DIFF -> "Farkı $target olan iki sayıyı avla"
            RuleKind.PRODUCT -> "Çarpımı $target olan iki sayıyı avla"
            RuleKind.QUOTIENT -> "Bölümü $target olan iki sayıyı avla"
        }
        return LevelSpec(
            levelId = when (ruleKind) {
                RuleKind.SUM -> "A$target"
                RuleKind.DIFF -> "S$target"
                RuleKind.PRODUCT -> "M$target"
                RuleKind.QUOTIENT -> "B$target"
            },
            dayNumber = day,
            stageIndex = stage,
            title = title,
            subtitle = subtitle,
            segmentLabel = symbol,
            minutes = if (gridSize >= 6) 8 else 5,
            worldKey = worldKeys[day] ?: "toplama",
            gridSize = gridSize,
            ruleKind = ruleKind,
            target = target,
            groupSizes = groupSizes,
            ruleMode = MatchRuleMode.LINE_OF_SIGHT
        )
    }

    private fun additionLevels(): List<LevelSpec> = (1..30).map { target ->
        val grid = if (target <= 3) 4 else 6
        val sizes = when {
            target <= 3 -> listOf(2)
            target <= 7 -> listOf(2, 3)
            else -> listOf(2, 3, 4)
        }
        level(1, target, RuleKind.SUM, target, grid, sizes)
    }

    private fun subtractionLevels(): List<LevelSpec> = (1..12).map { target ->
        level(2, target, RuleKind.DIFF, target, if (target <= 3) 4 else 6)
    }

    private fun multiplicationLevels(): List<LevelSpec> {
        val targets = listOf(2, 4, 6, 8, 9, 10, 12, 14, 15, 16, 18, 20, 24, 25, 30, 36)
        return targets.mapIndexed { index, target ->
            level(3, index + 1, RuleKind.PRODUCT, target, if (target <= 6) 4 else 6)
        }
    }

    private fun divisionLevels(): List<LevelSpec> = (1..8).map { target ->
        level(4, target, RuleKind.QUOTIENT, target, if (target <= 2) 4 else 6)
    }

    val allLevels: List<LevelSpec> =
        additionLevels() + subtractionLevels() + multiplicationLevels() + divisionLevels()

    val dayCount: Int = 4

    fun levelsForDay(day: Int): List<LevelSpec> =
        allLevels.filter { it.dayNumber == day }.sortedBy { it.stageIndex }

    fun levelById(levelId: String): LevelSpec? = allLevels.firstOrNull { it.levelId == levelId }

    fun nextLevelId(afterLevelId: String?): String? {
        if (afterLevelId == null) return allLevels.first().levelId
        val index = allLevels.indexOfFirst { it.levelId == afterLevelId }
        return if (index < 0 || index + 1 >= allLevels.size) null else allLevels[index + 1].levelId
    }

    fun dayMinutes(day: Int): Int = levelsForDay(day).sumOf { it.minutes }

    fun isDayCompleted(day: Int, completedLevelIds: Set<String>): Boolean =
        levelsForDay(day).all { completedLevelIds.contains(it.levelId) }

    /** Üst köşede yazan tek satır: "7 toplama", "12 çıkarma", "24 çarpma". */
    fun topTitle(spec: LevelSpec): String = "${spec.target} ${spec.operation.title.lowercase()}"

    /** Bölümün kısa hedef cümlesi. */
    fun objectiveText(spec: LevelSpec): String {
        val sizes = spec.groupSizes.filter { it in 2..4 }.ifEmpty { listOf(2) }
        val groupText = when {
            sizes == listOf(2) -> "2 taşı"
            sizes == listOf(3) -> "3 taşı"
            sizes == listOf(4) -> "4 taşı"
            else -> sizes.joinToString(", ") + " taşı"
        }
        return when (spec.ruleKind) {
            RuleKind.SUM -> "Toplamı ${spec.target} yapan $groupText seç"
            RuleKind.DIFF -> "Farkı ${spec.target} olan 2 taşı seç"
            RuleKind.PRODUCT -> "Çarpımı ${spec.target} olan 2 taşı seç"
            RuleKind.QUOTIENT -> "Bölümü ${spec.target} olan 2 taşı seç"
        }
    }

    /** Kısa oynanış cümlesi: parmakla sürükleme kuralını anlatır. */
    fun howToPlayText(spec: LevelSpec): String = when (spec.ruleMode) {
        MatchRuleMode.LINE_OF_SIGHT ->
            "Bir sayıya basılı tut, parmağını komşu sayılara sürükle ve bırak. Yatay, dikey ve çapraz komşular olur; aralarında taş kalmayan uzak sayılar da komşudur."
        MatchRuleMode.FREE ->
            "Tahtadaki herhangi iki (veya üç) taşı seçebilirsin, yön ve mesafe önemli değil."
    }
}
