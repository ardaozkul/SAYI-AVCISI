package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.DailySegmentType
import com.sayiavcisi.app.model.DifficultyTier
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationType
import com.sayiavcisi.app.model.QuestionFormat
import com.sayiavcisi.app.model.ThemeWorld

/**
 * 5 Günlük Macera ve Eğitim Programı + Özel Pratik Modu Katalogu.
 *
 * Her gün 60 dakikalık eğitim planına uygun 6 aşamadan oluşur:
 * 1. Isınma (10 dk)
 * 2. 1. Ana Bulmaca (15 dk)
 * 3. Enerji Molası & Eğlenceli Mini Bulmaca (5 dk)
 * 4. 2. Ana Bulmaca (15 dk)
 * 5. Karışık İşlemler (10 dk)
 * 6. Değerlendirme ve Ödül (5 dk)
 *
 * Ayrıca Toplama Modu'ndaki tüm hedef aralıkları (1..10, 11-15, 16-20, 21-30, 31-50, 51-100)
 * hem 5 günlük akışta hem de Özel Seviye Seçici'de eksiksiz yer alır.
 */
object CurriculumRepository {

    val allLevels: List<LevelSpec> = listOf(
        // ==========================================
        // 1. GÜN: SAYILARIN GÜCÜ (Sayı Ormanı)
        // - 0 ve 1 kavramı, 1-10 toplama, 1-10 çıkarma, 2 sayılı eşleştirme, 4x4 ve 5x5 tablolar
        // ==========================================
        LevelSpec(
            levelId = "D1_S1",
            dayNumber = 1,
            stageIndex = 1,
            title = "Sıfır ve Bir'in Sırrı",
            subtitle = "0+1=1, 0+5=5, 1'den 5'e Toplama • 4×4 Başlangıç",
            segmentType = DailySegmentType.WARMUP,
            themeWorld = ThemeWorld.SAYI_ORMANI,
            difficultyTier = DifficultyTier.STARTER,
            operation = OperationType.ADDITION,
            operandCount = 2,
            minTarget = 1,
            maxTarget = 5,
            focusZero = true,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D1_S2",
            dayNumber = 1,
            stageIndex = 2,
            title = "Orman Patikası: 6'dan 10'a",
            subtitle = "6, 7, 8, 9 ve 10'a Ulaşma • 4×4 Ana Bulmaca",
            segmentType = DailySegmentType.MAIN_PUZZLE_1,
            themeWorld = ThemeWorld.SAYI_ORMANI,
            difficultyTier = DifficultyTier.STARTER,
            operation = OperationType.ADDITION,
            operandCount = 2,
            minTarget = 6,
            maxTarget = 10,
            focusZero = true,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D1_S3",
            dayNumber = 1,
            stageIndex = 3,
            title = "Yaprak Molası: Sıfıra İniş",
            subtitle = "5-5=0, 7-6=1 • Kolay Çıkarma ve Dinlenme Turu",
            segmentType = DailySegmentType.BREAK_TIME,
            themeWorld = ThemeWorld.SAYI_ORMANI,
            difficultyTier = DifficultyTier.STARTER,
            operation = OperationType.SUBTRACTION,
            operandCount = 2,
            minTarget = 0,
            maxTarget = 5,
            focusZero = true,
            matchRuleMode = MatchRuleMode.FREE
        ),
        LevelSpec(
            levelId = "D1_S4",
            dayNumber = 1,
            stageIndex = 4,
            title = "Meşe Ağacı: 1-10 Çıkarma",
            subtitle = "9-4=5, 10-3=7 • Büyükten Küçüğü Çıkar • 5×5 Kolay",
            segmentType = DailySegmentType.MAIN_PUZZLE_2,
            themeWorld = ThemeWorld.SAYI_ORMANI,
            difficultyTier = DifficultyTier.EASY,
            operation = OperationType.SUBTRACTION,
            operandCount = 2,
            minTarget = 0,
            maxTarget = 10,
            focusZero = true,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D1_S5",
            dayNumber = 1,
            stageIndex = 5,
            title = "Orman Karışımı (1-10)",
            subtitle = "1-10 Arası Toplama ve Çıkarma Bir Arada • 5×5",
            segmentType = DailySegmentType.MIXED_PRACTICE,
            themeWorld = ThemeWorld.SAYI_ORMANI,
            difficultyTier = DifficultyTier.EASY,
            operation = OperationType.ADDITION,
            operandCount = 2,
            minTarget = 1,
            maxTarget = 10,
            focusZero = true,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D1_S6",
            dayNumber = 1,
            stageIndex = 6,
            title = "1. Gün Ödül Sandığı",
            subtitle = "Sayıların Gücü Değerlendirme ve Altın Rozet • 5×5",
            segmentType = DailySegmentType.EVALUATION_REWARD,
            themeWorld = ThemeWorld.SAYI_ORMANI,
            difficultyTier = DifficultyTier.EASY,
            operation = OperationType.SUBTRACTION,
            operandCount = 2,
            minTarget = 1,
            maxTarget = 10,
            focusZero = false,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),

        // ==========================================
        // 2. GÜN: ONLUK AVCISI (Hazine Mağarası)
        // - 10-20 toplama, 10-20 çıkarma, 3 sayılı toplama, 2 sayılı çıkarma, 5x5 ve 6x6 tablolar
        // - Ayrıca 21-30, 31-50, 51-100 ileri toplama hedefleri
        // ==========================================
        LevelSpec(
            levelId = "D2_S1",
            dayNumber = 2,
            stageIndex = 1,
            title = "Kristal Giriş: 11-15 Toplama",
            subtitle = "6+7=13, 8+7=15 • Onlukları Aşalım • 5×5",
            segmentType = DailySegmentType.WARMUP,
            themeWorld = ThemeWorld.HAZINE_MAGARASI,
            difficultyTier = DifficultyTier.EASY,
            operation = OperationType.ADDITION,
            operandCount = 2,
            minTarget = 11,
            maxTarget = 15,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D2_S2",
            dayNumber = 2,
            stageIndex = 2,
            title = "Altın Damar: 16-20 ve Üçlü Toplama",
            subtitle = "5+4+1=10, 8+7+5=20 • Üç Sayıyı Eşleştir • 6×6 Orta",
            segmentType = DailySegmentType.MAIN_PUZZLE_1,
            themeWorld = ThemeWorld.HAZINE_MAGARASI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.ADDITION,
            operandCount = 3,
            minTarget = 10,
            maxTarget = 20,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D2_S3",
            dayNumber = 2,
            stageIndex = 3,
            title = "Zümrüt Molası: Serbest Toplama (21-30)",
            subtitle = "21-30 Arası Hedefler • Serbest Eşleştirme Modu • 5×5",
            segmentType = DailySegmentType.BREAK_TIME,
            themeWorld = ThemeWorld.HAZINE_MAGARASI,
            difficultyTier = DifficultyTier.EASY,
            operation = OperationType.ADDITION,
            operandCount = 2,
            minTarget = 21,
            maxTarget = 30,
            matchRuleMode = MatchRuleMode.FREE
        ),
        LevelSpec(
            levelId = "D2_S4",
            dayNumber = 2,
            stageIndex = 4,
            title = "Yakut Salon: 10-20 Çıkarma",
            subtitle = "15-8=7, 20-12=8 • Onluk Bozarak Çıkarma • 6×6 Orta",
            segmentType = DailySegmentType.MAIN_PUZZLE_2,
            themeWorld = ThemeWorld.HAZINE_MAGARASI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.SUBTRACTION,
            operandCount = 2,
            minTarget = 5,
            maxTarget = 20,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D2_S5",
            dayNumber = 2,
            stageIndex = 5,
            title = "Derin Hazine: Zincir Çıkarma & 31-50",
            subtitle = "Üç Sayılı Zincir Çıkarma • 6×6 Orta",
            segmentType = DailySegmentType.MIXED_PRACTICE,
            themeWorld = ThemeWorld.HAZINE_MAGARASI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.SUBTRACTION,
            operandCount = 3,
            minTarget = 2,
            maxTarget = 15,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D2_S6",
            dayNumber = 2,
            stageIndex = 6,
            title = "2. Gün Büyük Hazine (51-100)",
            subtitle = "51-100 Arası Dev Toplamlar ve Elmas Ödülü • 6×6",
            segmentType = DailySegmentType.EVALUATION_REWARD,
            themeWorld = ThemeWorld.HAZINE_MAGARASI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.ADDITION,
            operandCount = 2,
            minTarget = 51,
            maxTarget = 100,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),

        // ==========================================
        // 3. GÜN: ÇARPIM ORMANI (Çarpım Kalesi)
        // - 0, 1, 2, 3, 4, 5 ve 10 çarpımları, daha sonra 6, 7, 8, 9 çarpımları
        // - Görsel gruplama, Eksik çarpan bulma (? × 4 = 20), 6x6 tablolar
        // ==========================================
        LevelSpec(
            levelId = "D3_S1",
            dayNumber = 3,
            stageIndex = 1,
            title = "Kale Kapısı: 0, 1, 2 ve 10 Çarpımları",
            subtitle = "Sıfır Yutan Eleman & İkişer Ritmik • 6×6 Orta",
            segmentType = DailySegmentType.WARMUP,
            themeWorld = ThemeWorld.CARPIM_KALESI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.MULTIPLICATION,
            operandCount = 2,
            minTarget = 0,
            maxTarget = 20,
            focusZero = true,
            allowedMultipliers = listOf(0, 1, 2, 10),
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D3_S2",
            dayNumber = 3,
            stageIndex = 2,
            title = "Şövalye Salonu: 3, 4 ve 5 Çarpımları",
            subtitle = "2×3=6, 4×5=20 • Görsel Gruplama ile Çarpma • 6×6",
            segmentType = DailySegmentType.MAIN_PUZZLE_1,
            themeWorld = ThemeWorld.CARPIM_KALESI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.MULTIPLICATION,
            operandCount = 2,
            minTarget = 3,
            maxTarget = 50,
            allowedMultipliers = listOf(2, 3, 4, 5, 10),
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D3_S3",
            dayNumber = 3,
            stageIndex = 3,
            title = "Kule Molası: Serbest Çarpım Avı",
            subtitle = "0, 1, 2, 3, 4, 5, 10 Serbest Eşleştirme • 6×6",
            segmentType = DailySegmentType.BREAK_TIME,
            themeWorld = ThemeWorld.CARPIM_KALESI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.MULTIPLICATION,
            operandCount = 2,
            minTarget = 0,
            maxTarget = 50,
            allowedMultipliers = listOf(1, 2, 3, 4, 5, 10),
            matchRuleMode = MatchRuleMode.FREE
        ),
        LevelSpec(
            levelId = "D3_S4",
            dayNumber = 3,
            stageIndex = 4,
            title = "Ejderha Kulesi: 6, 7, 8 ve 9 Çarpımları",
            subtitle = "3×7=21, 6×6=36, 8×7=56, 9×9=81 • 6×6 Orta",
            segmentType = DailySegmentType.MAIN_PUZZLE_2,
            themeWorld = ThemeWorld.CARPIM_KALESI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.MULTIPLICATION,
            operandCount = 2,
            minTarget = 12,
            maxTarget = 81,
            allowedMultipliers = listOf(6, 7, 8, 9),
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D3_S5",
            dayNumber = 3,
            stageIndex = 5,
            title = "Gizemli Çarpan: ? × 4 = 20",
            subtitle = "Eksik Çarpanı Bul ve Eşleştir • 6×6 Orta",
            segmentType = DailySegmentType.MIXED_PRACTICE,
            themeWorld = ThemeWorld.CARPIM_KALESI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.MULTIPLICATION,
            operandCount = 2,
            minTarget = 4,
            maxTarget = 64,
            allowedMultipliers = listOf(2, 3, 4, 5, 6, 7, 8),
            questionFormat = QuestionFormat.MISSING_FACTOR,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D3_S6",
            dayNumber = 3,
            stageIndex = 6,
            title = "3. Gün Çarpım Tacı",
            subtitle = "Tüm Çarpım Tablosu Değerlendirme ve Ödül • 6×6",
            segmentType = DailySegmentType.EVALUATION_REWARD,
            themeWorld = ThemeWorld.CARPIM_KALESI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.MULTIPLICATION,
            operandCount = 2,
            minTarget = 6,
            maxTarget = 81,
            allowedMultipliers = listOf(2, 3, 4, 5, 6, 7, 8, 9),
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),

        // ==========================================
        // 4. GÜN: BÖLME MAĞARASI (Bölme Uzayı)
        // - Kalansız bölme, Çarpma ve bölme ilişkisi, Eksik bölen (20÷?=5) ve bölünen, 6x6 ve 8x8 tablolar
        // ==========================================
        LevelSpec(
            levelId = "D4_S1",
            dayNumber = 4,
            stageIndex = 1,
            title = "Yörünge: Kalansız Bölme Başlangıcı",
            subtitle = "0÷2=0, 6÷2=3, 10÷5=2 • Sıfıra Bölme Yok! • 6×6",
            segmentType = DailySegmentType.WARMUP,
            themeWorld = ThemeWorld.BOLME_UZAYI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.DIVISION,
            operandCount = 2,
            minTarget = 0,
            maxTarget = 5,
            focusZero = true,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D4_S2",
            dayNumber = 4,
            stageIndex = 2,
            title = "Yıldız Üssü: Çarpmanın Tersi Bölme",
            subtitle = "12÷3=4, 20÷4=5, 36÷6=6 • 6×6 Ana Bulmaca",
            segmentType = DailySegmentType.MAIN_PUZZLE_1,
            themeWorld = ThemeWorld.BOLME_UZAYI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.DIVISION,
            operandCount = 2,
            minTarget = 2,
            maxTarget = 7,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D4_S3",
            dayNumber = 4,
            stageIndex = 3,
            title = "Yerçekimsiz Mola: Serbest Bölme",
            subtitle = "Bölünen ve Böleni Serbest Eşleştir • 6×6",
            segmentType = DailySegmentType.BREAK_TIME,
            themeWorld = ThemeWorld.BOLME_UZAYI,
            difficultyTier = DifficultyTier.MEDIUM,
            operation = OperationType.DIVISION,
            operandCount = 2,
            minTarget = 1,
            maxTarget = 8,
            matchRuleMode = MatchRuleMode.FREE
        ),
        LevelSpec(
            levelId = "D4_S4",
            dayNumber = 4,
            stageIndex = 4,
            title = "Galaksi Kapısı: 56÷7=8 ve 81÷9=9",
            subtitle = "İleri Kalansız Bölme • 8×8 Zor Tahta",
            segmentType = DailySegmentType.MAIN_PUZZLE_2,
            themeWorld = ThemeWorld.BOLME_UZAYI,
            difficultyTier = DifficultyTier.HARD,
            operation = OperationType.DIVISION,
            operandCount = 2,
            minTarget = 3,
            maxTarget = 9,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D4_S5",
            dayNumber = 4,
            stageIndex = 5,
            title = "Kayıp Bölen Keşfi: 20 ÷ ? = 5",
            subtitle = "Eksik Böleni Bul! • 8×8 Zor Tahta",
            segmentType = DailySegmentType.MIXED_PRACTICE,
            themeWorld = ThemeWorld.BOLME_UZAYI,
            difficultyTier = DifficultyTier.HARD,
            operation = OperationType.DIVISION,
            operandCount = 2,
            minTarget = 2,
            maxTarget = 9,
            questionFormat = QuestionFormat.MISSING_DIVISOR,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D4_S6",
            dayNumber = 4,
            stageIndex = 6,
            title = "4. Gün Uzay Madalyası: ? ÷ 4 = 5",
            subtitle = "Eksik Bölünen ve Bölme Ustası Ödülü • 8×8",
            segmentType = DailySegmentType.EVALUATION_REWARD,
            themeWorld = ThemeWorld.BOLME_UZAYI,
            difficultyTier = DifficultyTier.HARD,
            operation = OperationType.DIVISION,
            operandCount = 2,
            minTarget = 2,
            maxTarget = 9,
            questionFormat = QuestionFormat.MISSING_DIVIDEND,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),

        // ==========================================
        // 5. GÜN: BÜYÜK MATEMATİK FİNALİ (Matematik Adası)
        // - Dört işlem karışık, 20'ye kadar toplama/çıkarma, Çarpım tablosu, Kalansız bölme
        // - 8x8 ve 10x10 tablolar, Büyük final ve ödül
        // ==========================================
        LevelSpec(
            levelId = "D5_S1",
            dayNumber = 5,
            stageIndex = 1,
            title = "Ada Sahili: Dört İşlem Isınma",
            subtitle = "Toplama, Çıkarma, Çarpma, Bölme Karışık • 8×8 Zor",
            segmentType = DailySegmentType.WARMUP,
            themeWorld = ThemeWorld.MATEMATIK_ADASI,
            difficultyTier = DifficultyTier.HARD,
            operation = OperationType.MIXED,
            operandCount = 2,
            minTarget = 2,
            maxTarget = 20,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D5_S2",
            dayNumber = 5,
            stageIndex = 2,
            title = "Volkan Geçidi: Üçlü Karışık Macera",
            subtitle = "Üç Sayılı Toplama ve Zincir Çıkarma • 8×8 Zor",
            segmentType = DailySegmentType.MAIN_PUZZLE_1,
            themeWorld = ThemeWorld.MATEMATIK_ADASI,
            difficultyTier = DifficultyTier.HARD,
            operation = OperationType.MIXED,
            operandCount = 3,
            minTarget = 6,
            maxTarget = 20,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D5_S3",
            dayNumber = 5,
            stageIndex = 3,
            title = "Palmiye Molası: Serbest Dört İşlem",
            subtitle = "Serbest Eşleştirme ile Hızlı Tekrar • 8×8",
            segmentType = DailySegmentType.BREAK_TIME,
            themeWorld = ThemeWorld.MATEMATIK_ADASI,
            difficultyTier = DifficultyTier.HARD,
            operation = OperationType.MIXED,
            operandCount = 2,
            minTarget = 2,
            maxTarget = 20,
            matchRuleMode = MatchRuleMode.FREE
        ),
        LevelSpec(
            levelId = "D5_S4",
            dayNumber = 5,
            stageIndex = 4,
            title = "Efsane Tapınak: 10×10 Uzman Meydan Okuması",
            subtitle = "100 Blokluk Dev Tahta! • 10×10 Uzman",
            segmentType = DailySegmentType.MAIN_PUZZLE_2,
            themeWorld = ThemeWorld.MATEMATIK_ADASI,
            difficultyTier = DifficultyTier.EXPERT,
            operation = OperationType.MIXED,
            operandCount = 2,
            minTarget = 2,
            maxTarget = 20,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D5_S5",
            dayNumber = 5,
            stageIndex = 5,
            title = "Bilgelik Köprüsü: 10×10 Çarpma & Bölme",
            subtitle = "Çarpım Tablosu ve Kalansız Bölme Zirvesi • 10×10 Uzman",
            segmentType = DailySegmentType.MIXED_PRACTICE,
            themeWorld = ThemeWorld.MATEMATIK_ADASI,
            difficultyTier = DifficultyTier.EXPERT,
            operation = OperationType.MULTIPLICATION,
            operandCount = 2,
            minTarget = 4,
            maxTarget = 81,
            allowedMultipliers = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10),
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        ),
        LevelSpec(
            levelId = "D5_S6",
            dayNumber = 5,
            stageIndex = 6,
            title = "BÜYÜK MATEMATİK FİNALİ",
            subtitle = "10×10 Büyük Final ve Şampiyonluk Kupası!",
            segmentType = DailySegmentType.EVALUATION_REWARD,
            themeWorld = ThemeWorld.MATEMATIK_ADASI,
            difficultyTier = DifficultyTier.EXPERT,
            operation = OperationType.MIXED,
            operandCount = 2,
            minTarget = 2,
            maxTarget = 20,
            matchRuleMode = MatchRuleMode.DIRECTIONAL
        )
    )

    fun getLevelsForDay(day: Int): List<LevelSpec> = allLevels.filter { it.dayNumber == day }

    fun getLevelById(id: String): LevelSpec = allLevels.firstOrNull { it.levelId == id } ?: allLevels.first()

    /**
     * Toplama modundaki tüm özel hedef basamaklarını (1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
     * 11-15, 16-20, 21-30, 31-50, 51-100) doğrudan deneyimlemek isteyen çocuklar için
     * hızlı özel seviye oluşturucu.
     */
    fun createCustomAdditionLevel(
        minTarget: Int,
        maxTarget: Int,
        operandCount: Int = 2,
        tier: DifficultyTier = DifficultyTier.STARTER,
        ruleMode: MatchRuleMode = MatchRuleMode.DIRECTIONAL
    ): LevelSpec {
        val label = if (minTarget == maxTarget) "$minTarget'e Ulaşma" else "$minTarget-$maxTarget Arası"
        return LevelSpec(
            levelId = "CUSTOM_ADD_${minTarget}_${maxTarget}_${operandCount}_${tier.name}",
            dayNumber = 1,
            stageIndex = 1,
            title = "Toplama Avı: $label",
            subtitle = "${tier.label} • $operandCount Sayılı Eşleştirme",
            segmentType = DailySegmentType.MAIN_PUZZLE_1,
            themeWorld = ThemeWorld.SAYI_ORMANI,
            difficultyTier = tier,
            operation = OperationType.ADDITION,
            operandCount = operandCount,
            minTarget = minTarget,
            maxTarget = maxTarget,
            focusZero = minTarget <= 5,
            matchRuleMode = ruleMode
        )
    }
}
