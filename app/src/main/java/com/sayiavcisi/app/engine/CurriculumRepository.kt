package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind

/**
 * 2.0 müfredatı: 5 gün x 6 bölüm, her gün yaklaşık 60 dakika.
 *
 * Her bölümde TEK bir hedef vardır (Toplamı X / Farkı X / Çarpımı X / Bölümü X).
 * Zorluk kolaydan zora: hedef 1'den başlar, 0'ı da içerir, 30+ hedeflere ve 10x10 tahtaya kadar yükselir.
 * 5. günün 5. bölümü 10x10 tahtasıyla ~15 dakikalık büyük bir mesaidir.
 */
object CurriculumRepository {

    val dayTitles: Map<Int, String> = mapOf(
        1 to "Sayı Ormanı",
        2 to "Hazine Mağarası",
        3 to "Çarpım Kalesi",
        4 to "Bölme Uzayı",
        5 to "Matematik Adası"
    )

    val daySubtitles: Map<Int, String> = mapOf(
        1 to "Toplama 1'den başla, sıfırı keşfet",
        2 to "Büyük toplamlar ve çıkarma başlangıcı",
        3 to "Çıkarmada ustalık, çarpım tablosu",
        4 to "Çarpmanın tersi: bölme",
        5 to "Büyük final: 10x10 tahta"
    )

    private fun level(
        day: Int,
        stage: Int,
        title: String,
        subtitle: String,
        segmentLabel: String,
        minutes: Int,
        worldKey: String,
        gridSize: Int,
        ruleKind: RuleKind,
        target: Int,
        groupSizes: List<Int> = listOf(2),
        ruleMode: MatchRuleMode = MatchRuleMode.LINE_OF_SIGHT,
        timeLimitSeconds: Int? = null,
        focusZero: Boolean = false
    ) = LevelSpec(
        levelId = "D${day}_S${stage}",
        dayNumber = day,
        stageIndex = stage,
        title = title,
        subtitle = subtitle,
        segmentLabel = segmentLabel,
        minutes = minutes,
        worldKey = worldKey,
        gridSize = gridSize,
        ruleKind = ruleKind,
        target = target,
        groupSizes = groupSizes,
        ruleMode = ruleMode,
        timeLimitSeconds = timeLimitSeconds,
        focusZero = focusZero
    )

    val allLevels: List<LevelSpec> = listOf(
        // 1. GÜN - TOPLAMA: hedef 1'den başlar, 0 devrede
        level(1, 1, "Isınma: Hedef 1", "Toplamı 1 olan iki taşı bul (0 da var!)", "Isınma", 6, "orman", 4,
            RuleKind.SUM, 1, listOf(2), focusZero = true),
        level(1, 2, "Hedef 2", "Toplamı 2 olan taşları eşleştir", "Alıştırma", 8, "orman", 4,
            RuleKind.SUM, 2, listOf(2), focusZero = true),
        level(1, 3, "Hedef 5", "Beşe giden iki veya üç sayı", "Ana Bulmaca", 10, "orman", 5,
            RuleKind.SUM, 5, listOf(2, 3)),
        level(1, 4, "Hedef 7", "Yediye ulaşan komşu sayılar", "Ana Bulmaca", 12, "orman", 5,
            RuleKind.SUM, 7, listOf(2, 3)),
        level(1, 5, "Onluk Avcısı", "Toplamı 10 olan grupları yok et", "Ana Bulmaca", 14, "orman", 6,
            RuleKind.SUM, 10, listOf(2, 3)),
        level(1, 6, "Serbest Orman", "Yön serbest: toplamı 10", "Pekiştirme", 10, "orman", 6,
            RuleKind.SUM, 10, listOf(2, 3), ruleMode = MatchRuleMode.FREE),

        // 2. GÜN - BÜYÜK TOPLAMA + ÇIKARMA BAŞLANGICI
        level(2, 1, "Hazine Sayıları", "Toplamı 13 olan taşlar", "Isınma", 8, "magara", 6,
            RuleKind.SUM, 13, listOf(2, 3)),
        level(2, 2, "Hedef 16", "On altıya üç sayıyla da ulaşabilirsin", "Alıştırma", 10, "magara", 6,
            RuleKind.SUM, 16, listOf(2, 3)),
        level(2, 3, "Büyük Mağara", "Toplamı 20 • 8x8 tahta", "Ana Bulmaca", 14, "magara", 8,
            RuleKind.SUM, 20, listOf(2, 3)),
        level(2, 4, "Çıkarmaya İlk Adım", "Farkı 2 olan iki taş", "Çıkarma", 8, "magara", 4,
            RuleKind.DIFF, 2),
        level(2, 5, "Sıfırı Bul", "Farkı 0 olan eşit taşlar (ikiz sayılar!)", "Çıkarma", 10, "magara", 6,
            RuleKind.DIFF, 0, focusZero = true),
        level(2, 6, "Hazine Avı", "Farkı 5 olan taşlar", "Pekiştirme", 10, "magara", 6,
            RuleKind.DIFF, 5),

        // 3. GÜN - ÇIKARMA USTALIĞI + ÇARPMA
        level(3, 1, "Kale Bekçileri", "Farkı 4", "Isınma", 8, "kale", 6, RuleKind.DIFF, 4),
        level(3, 2, "Farkı 7", "Büyük taştan küçüğü çıkar", "Alıştırma", 10, "kale", 6, RuleKind.DIFF, 7),
        level(3, 3, "Kale Kuşatması", "Farkı 10 • 8x8", "Ana Bulmaca", 12, "kale", 8, RuleKind.DIFF, 10),
        level(3, 4, "Çarpmanın Sırrı", "Çarpımı 6 olan iki taş", "Çarpma", 8, "kale", 4,
            RuleKind.PRODUCT, 6),
        level(3, 5, "Çarpım Odaları", "Çarpımı 12", "Çarpma", 10, "kale", 6, RuleKind.PRODUCT, 12),
        level(3, 6, "Zindan Zinciri", "Çarpımı 24", "Pekiştirme", 12, "kale", 6, RuleKind.PRODUCT, 24),

        // 4. GÜN - ÇARPMA İLERİ + BÖLME
        level(4, 1, "Uzay Tozu", "Çarpımı 36", "Isınma", 10, "uzay", 6, RuleKind.PRODUCT, 36),
        level(4, 2, "Sıfır Gezegeni", "Bölümü 0 (0 ÷ her sayı = 0)", "Bölme", 6, "uzay", 4,
            RuleKind.QUOTIENT, 0, focusZero = true),
        level(4, 3, "Hedef Bölüm 2", "Kalansız bölme bul", "Bölme", 10, "uzay", 6, RuleKind.QUOTIENT, 2),
        level(4, 4, "Yörünge 5", "Bölümü 5 olan çiftler", "Ana Bulmaca", 12, "uzay", 6, RuleKind.QUOTIENT, 5),
        level(4, 5, "Galaksi Bahçesi", "Çarpımı 48 • 8x8", "Çarpma", 10, "uzay", 8, RuleKind.PRODUCT, 48),
        level(4, 6, "Kara Delik", "Bölümü 6 • 8x8", "Pekiştirme", 12, "uzay", 8, RuleKind.QUOTIENT, 6),

        // 5. GÜN - BÜYÜK FİNAL
        level(5, 1, "Ada Rüzgârı", "Toplamı 25", "Isınma", 6, "ada", 8, RuleKind.SUM, 25, listOf(2, 3)),
        level(5, 2, "Volkan Yolu", "Farkı 15", "Alıştırma", 8, "ada", 8, RuleKind.DIFF, 15),
        level(5, 3, "İncir Mağazası", "Çarpımı 54", "Ana Bulmaca", 8, "ada", 8, RuleKind.PRODUCT, 54),
        level(5, 4, "Fener Kulübesi", "Bölümü 8 • süre baskısı", "Ana Bulmaca", 11, "ada", 8,
            RuleKind.QUOTIENT, 8, timeLimitSeconds = 660),
        level(5, 5, "BÜYÜK FİNAL 10x10", "Toplamı 30 • 100 blok, ~15 dakika", "Final", 15, "ada", 10,
            RuleKind.SUM, 30, listOf(2, 3)),
        level(5, 6, "Serbest Final", "Yön serbest • çarpımı 30", "Ödül", 12, "ada", 10,
            RuleKind.PRODUCT, 30, ruleMode = MatchRuleMode.FREE)
    )

    val stagesPerDay: Int = 6
    val dayCount: Int = 5

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

    /** Bölümün kısa hedef cümlesi: oyun ekranındaki HEDEF kartında gösterilir. */
    fun objectiveText(spec: LevelSpec): String {
        val sizes = spec.groupSizes.filter { it in 2..3 }.ifEmpty { listOf(2) }
        val groupText = when {
            sizes == listOf(2) -> "2 taşı"
            sizes == listOf(3) -> "3 taşı"
            else -> "2 veya 3 taşı"
        }
        val ruleText = when (spec.ruleKind) {
            RuleKind.SUM -> "Toplamı ${spec.target} olan $groupText seç"
            RuleKind.DIFF -> "Farkı ${spec.target} olan 2 taşı seç"
            RuleKind.PRODUCT -> "Çarpımı ${spec.target} olan 2 taşı seç"
            RuleKind.QUOTIENT -> "Bölümü ${spec.target} olan 2 taşı seç"
        }
        return ruleText
    }

    /** Çocuğa bölüme girerken gösterilen "nasıl oynanır" metni. */
    fun howToPlayText(spec: LevelSpec): String {
        val geometry = when (spec.ruleMode) {
            MatchRuleMode.LINE_OF_SIGHT ->
                "Taşlar aynı hatta olmalı: yatay, dikey veya çapraz. Aralarında taş kalmışsa önce onu temizle. Yön ters de olabilir (sağdan sola, aşağıdan yukarıya)."
            MatchRuleMode.FREE ->
                "Bu bölüm serbest: tahtadaki herhangi iki (veya üç) taşı seçebilirsin, yön ve mesafe önemli değil."
        }
        val zero = if (spec.focusZero) " Sıfır da bir taş olarak kullanılabilir." else ""
        return "$geometry$zero Sıra senin: hangi taşa önce dokunduğun fark etmez."
    }
}
