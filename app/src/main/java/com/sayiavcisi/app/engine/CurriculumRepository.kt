package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind

/**
 * Müfredat: dört bölüm, hepsi baştan açık.
 *
 * Tahta her zaman 8 sütundur. Satır sayısı bölümün uzunluğunu belirler:
 * - 8 satır  : genel bölümler (8×8 = 64 blok)
 * - 12 satır : kolay çıkarma/çarpma/bölme hedefleri (8×12 = 96 blok)
 * - 60 satır : toplamanın TÜM hedefleri ve bölüm finalleri (8×60 = 480 blok)
 *
 * 1) TOPLAMA  : 1'den 30'a ardışık hedefler; her biri en az 8×60. 2-4'lü kombolar var.
 * 2) ÇIKARMA : 1'den 12'ye fark hedefleri.
 * 3) ÇARPMA  : Çarpım tablosu hedefleri (2, 4, 6 ... 36).
 * 4) BÖLME   : Kalansız bölme hedefleri.
 *
 * Çarpım tablosu dahil her bölüm açıktır; oyunu denemek isteyen çocuk/veli istediği yerden başlar.
 */
object CurriculumRepository {

    val dayTitles: Map<Int, String> = mapOf(
        1 to "Toplama",
        2 to "Çıkarma",
        3 to "Çarpma Tablosu",
        4 to "Bölme",
        5 to "Sonsuz Av"
    )

    val daySubtitles: Map<Int, String> = mapOf(
        1 to "Toplamı 1'den 30'a kadar avla · kombolar sınırsız (en fazla 8 taş)",
        2 to "Farkı bul · önce büyük sayı, kombolar büyüyebilir",
        3 to "Çarpım tablosu · 2'den 36'ya",
        4 to "Kalansız bölme · önce bölünen, sonra bölen",
        5 to "Anlık görevler · 8×120 · yeşil puan"
    )

    val daySymbols: Map<Int, String> = mapOf(
        1 to "+",
        2 to "−",
        3 to "×",
        4 to "÷",
        5 to "∞"
    )

    private val worldKeys = mapOf(
        1 to "toplama",
        2 to "cikarma",
        3 to "carpma",
        4 to "bolme",
        5 to "sonsuz"
    )

    private fun level(
        day: Int,
        stage: Int,
        ruleKind: RuleKind,
        target: Int,
        rows: Int,
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
        val minutes = when {
            rows >= 20 -> 20
            rows >= 10 -> 10
            else -> 8
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
            minutes = minutes,
            worldKey = worldKeys[day] ?: "toplama",
            cols = 8,
            rows = rows,
            ruleKind = ruleKind,
            target = target,
            groupSizes = groupSizes,
            ruleMode = MatchRuleMode.LINE_OF_SIGHT
        )
    }

    /** Toplama 1–30 boyunca hep 8×60; diğerlerinde kolay hedef 8×12, final 8×60. */
    private fun rowsFor(day: Int, target: Int, index: Int): Int {
        if (day == 1) return 60

        val long = when (day) {
            2 -> target == 12
            3 -> target == 36
            4 -> target == 8
            else -> false
        }
        if (long) return 60

        val easy = when (day) {
            2 -> target <= 3
            3 -> index <= 2
            4 -> target <= 3
            else -> false
        }
        return if (easy) 12 else 8
    }

    private fun additionLevels(): List<LevelSpec> = (1..30).map { target ->
        val sizes = when {
            target <= 3 -> listOf(2)
            target <= 7 -> listOf(2, 3)
            else -> listOf(2, 3, 4)
        }
        level(1, target, RuleKind.SUM, target, rowsFor(1, target, target), sizes)
    }

    private fun subtractionLevels(): List<LevelSpec> = (1..12).map { target ->
        level(2, target, RuleKind.DIFF, target, rowsFor(2, target, target))
    }

    private fun multiplicationLevels(): List<LevelSpec> {
        val targets = listOf(2, 4, 6, 8, 9, 10, 12, 14, 15, 16, 18, 20, 24, 25, 30, 36)
        return targets.mapIndexed { index, target ->
            level(3, index + 1, RuleKind.PRODUCT, target, rowsFor(3, target, index))
        }
    }

    private fun divisionLevels(): List<LevelSpec> = (1..8).map { target ->
        level(4, target, RuleKind.QUOTIENT, target, rowsFor(4, target, target))
    }

    /**
     * SONSUZ AV: tek bir sabit hedefi yoktur. 8×120 (tablette 20×120) tahtada temizlenen
     * her kareye hemen yeni bir sayı gelir; her doğru hamlede yeni bir anlık görev açılır.
     * Puanı normal bölümlerin sarı puanından ayrı, yeşil puandır.
     */
    val endlessLevel: LevelSpec = LevelSpec(
        levelId = "SONSUZ",
        dayNumber = 5,
        stageIndex = 1,
        title = "Sonsuz Av",
        subtitle = "Anlık görev · 8×120 · yeşil puan",
        segmentLabel = "∞",
        minutes = 0,
        worldKey = "sonsuz",
        cols = 8,
        rows = 120,
        ruleKind = RuleKind.SUM,
        target = 0,
        groupSizes = listOf(2),
        // Sonsuz Av'da komşuluk/hat aranmaz: görevdeki taşlar tahtanın her yerinden seçilebilir.
        ruleMode = MatchRuleMode.FREE,
        endless = true
    )

    /** Klasik 66 bölüm. Sonsuz Av ayrı bir kayıt ve akış olarak durur. */
    val standardLevels: List<LevelSpec> =
        additionLevels() + subtractionLevels() + multiplicationLevels() + divisionLevels()

    val allLevels: List<LevelSpec> = standardLevels + endlessLevel

    val dayCount: Int = 5

    fun levelsForDay(day: Int): List<LevelSpec> =
        if (day == 5) listOf(endlessLevel)
        else allLevels.filter { it.dayNumber == day }.sortedBy { it.stageIndex }

    fun levelById(levelId: String): LevelSpec? = allLevels.firstOrNull { it.levelId == levelId }

    fun nextLevelId(afterLevelId: String?): String? {
        if (afterLevelId == null) return allLevels.first().levelId
        val index = allLevels.indexOfFirst { it.levelId == afterLevelId }
        return if (index < 0 || index + 1 >= allLevels.size) null else allLevels[index + 1].levelId
    }

    fun dayMinutes(day: Int): Int =
        if (day == 5) 0 else levelsForDay(day).sumOf { it.minutes }

    fun isDayCompleted(day: Int, completedLevelIds: Set<String>): Boolean {
        if (day == 5) return false
        return levelsForDay(day).all { completedLevelIds.contains(it.levelId) }
    }

    /** Üst köşede yazan tek satır: "7 toplama", "a / b = 4", "24 çarpma". */
    fun topTitle(spec: LevelSpec): String = when {
        spec.endless -> "Sonsuz Av"
        // Bölmede "4 bölme" yerine işlemin kendisi görünür.
        spec.ruleKind == RuleKind.QUOTIENT -> "a / b = ${spec.target}"
        else -> "${spec.target} ${spec.operation.title.lowercase()}"
    }

    /** Bölümün kısa hedef cümlesi. */
    fun objectiveText(spec: LevelSpec): String {
        if (spec.endless) {
            return "Anlık görevi tamamla. Temizlenen her kareye hemen yeni bir sayı gelir."
        }
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
    fun howToPlayText(spec: LevelSpec): String = when {
        spec.endless ->
            "Görevdeki formülü tamamla. Komşuluk aranmaz; tahtanın her yerinden seçebilirsin. Doğru hamleden sonra taşlar yenilenir."
        spec.ruleMode == MatchRuleMode.LINE_OF_SIGHT ->
            "Bir sayıya basılı tut, parmağını komşu sayılara sürükle ve bırak. Yatay, dikey ve çapraz komşular olur; aralarında taş kalmayan uzak sayılar da komşudur."
        else ->
            "Tahtadaki herhangi iki (veya üç) taşı seçebilirsin, yön ve mesafe önemli değil."
    }

    /** Haritada/oyun başlığında gösterilen tahta etiketi: 8×8, 8×12, 20×120. */
    fun boardSizeLabel(spec: LevelSpec): String = "${spec.cols} × ${spec.rows}"

    /** Uzun finaller ve sonsuz av için kısa rozet metni. */
    fun boardBadge(spec: LevelSpec): String? = when {
        spec.endless -> "SONSUZ"
        spec.isLong -> "UZUN"
        spec.isEasyBoard -> "KOLAY"
        else -> null
    }

    /** Tablette tahtayı yatay büyütmek için kopya; levelId aynı kalır. */
    fun withColumns(spec: LevelSpec, columns: Int): LevelSpec {
        val safe = columns.coerceIn(4, 24)
        // Çıkarma/çarpma/bölme normal bölümlerde ikili gruplarla oynanır; tek sütun
        // son satırda eşsiz taş bırakmasın diye en yakın çift genişliğe inilir.
        val adjusted = if (!spec.endless && spec.ruleKind != RuleKind.SUM && safe % 2 == 1) {
            (safe - 1).coerceAtLeast(4)
        } else {
            safe
        }
        return if (adjusted == spec.cols) spec else spec.copy(cols = adjusted)
    }
}
