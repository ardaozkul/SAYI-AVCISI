package com.sayiavcisi.app.model

enum class OperationType(val symbol: String, val title: String) {
    ADDITION("+", "Toplama"),
    SUBTRACTION("-", "Çıkarma"),
    MULTIPLICATION("×", "Çarpma"),
    DIVISION("÷", "Bölme"),
    MIXED("±×÷", "Karışık Dört İşlem")
}

enum class MatchRuleMode(val title: String, val description: String) {
    DIRECTIONAL(
        "Doğrusal Eşleştirme",
        "Yatay, dikey veya çapraz doğrultuda, aralarında başka aktif blok bulunmayan sayıları eşleştir."
    ),
    FREE(
        "Serbest Eşleştirme Modu",
        "Tablodaki herhangi bir satır veya sütundaki uygun sayıları serbestçe eşleştir."
    )
}

enum class QuestionFormat {
    STANDARD_TARGET,      // Örn: HEDEF: 10 (Toplama, Çıkarma, Çarpma, Bölme)
    MISSING_FACTOR,       // Örn: ? × 4 = 20 (Eksik çarpanı ve 4'ü tahtada bulup eşleştir)
    MISSING_DIVISOR,      // Örn: 20 ÷ ? = 5 (20 ve eksik bölen 4'ü tahtada bulup eşleştir)
    MISSING_DIVIDEND      // Örn: ? ÷ 4 = 5 (Eksik bölünen 20 ve 4'ü tahtada bulup eşleştir)
}

enum class ThemeWorld(
    val title: String,
    val subtitle: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val bgTopHex: Long,
    val bgBottomHex: Long,
    val badgeEmoji: String
) {
    SAYI_ORMANI(
        "Sayı Ormanı",
        "1. Gün: Sayıların Gücü",
        0xFF10B981,
        0xFF059669,
        0xFF064E3B,
        0xFF022C22,
        "🌲"
    ),
    HAZINE_MAGARASI(
        "Hazine Mağarası",
        "2. Gün: Onluk Avcısı",
        0xFFF59E0B,
        0xFFD97706,
        0xFF451A03,
        0xFF270E02,
        "💎"
    ),
    CARPIM_KALESI(
        "Çarpım Kalesi",
        "3. Gün: Çarpım Ormanı & Kalesi",
        0xFF8B5CF6,
        0xFF6D28D9,
        0xFF2E1065,
        0xFF170736,
        "🏰"
    ),
    BOLME_UZAYI(
        "Bölme Uzayı",
        "4. Gün: Bölme Mağarası & Uzayı",
        0xFF06B6D4,
        0xFF0284C7,
        0xFF083344,
        0xFF041A24,
        "🚀"
    ),
    MATEMATIK_ADASI(
        "Matematik Adası",
        "5. Gün: Büyük Matematik Finali",
        0xFFF43F5E,
        0xFFE11D48,
        0xFF4C0519,
        0xFF26020D,
        "🏝️"
    )
}

enum class DifficultyTier(val label: String, val gridSize: Int, val defaultTimeSeconds: Int?) {
    STARTER("Başlangıç (4×4)", 4, null),
    EASY("Kolay (5×5)", 5, null),
    MEDIUM("Orta (6×6)", 6, 90),
    HARD("Zor (8×8)", 8, 60),
    EXPERT("Uzman (10×10)", 10, 45)
}

enum class DailySegmentType(val label: String, val recommendedMinutes: Int) {
    WARMUP("Isınma Turu", 10),
    MAIN_PUZZLE_1("1. Ana Bulmaca", 15),
    BREAK_TIME("Enerji Molası", 5),
    MAIN_PUZZLE_2("2. Ana Bulmaca", 15),
    MIXED_PRACTICE("Karışık Pekiştirme", 10),
    EVALUATION_REWARD("Değerlendirme ve Ödül", 5)
}

data class BlockCell(
    val id: Int,
    val row: Int,
    val col: Int,
    val value: Int,
    val isCleared: Boolean = false,
    val isHinted: Boolean = false
)

data class TargetStep(
    val operation: OperationType,
    val targetResult: Int,
    val operandCount: Int, // 2 veya 3
    val questionFormat: QuestionFormat = QuestionFormat.STANDARD_TARGET,
    val promptDisplay: String,
    val helperSubtext: String,
    val expectedOrderedValues: List<Int>,
    val fixedKnownOperand: Int? = null
)

data class PlannedMove(
    val cellCoords: List<Pair<Int, Int>>,
    val step: TargetStep
)

data class GeneratedBoard(
    val gridSize: Int,
    val cells: List<BlockCell>,
    val targets: List<TargetStep>,
    val verifiedSolution: List<PlannedMove>,
    val matchRuleMode: MatchRuleMode
)

data class LevelSpec(
    val levelId: String,
    val dayNumber: Int, // 1..5
    val stageIndex: Int, // 1..6
    val title: String,
    val subtitle: String,
    val segmentType: DailySegmentType,
    val themeWorld: ThemeWorld,
    val difficultyTier: DifficultyTier,
    val operation: OperationType,
    val operandCount: Int, // 2 veya 3
    val minTarget: Int,
    val maxTarget: Int,
    val focusZero: Boolean = false,
    val allowedMultipliers: List<Int> = emptyList(),
    val questionFormat: QuestionFormat = QuestionFormat.STANDARD_TARGET,
    val matchRuleMode: MatchRuleMode = MatchRuleMode.DIRECTIONAL
)

enum class JokerType(val title: String, val cost: Int, val description: String) {
    HINT("İPUCU", 5, "Doğru eşleşmenin hangi sayılardan oluştuğunu gösterir (-5 Puan)"),
    SHUFFLE("YENİDEN DÜZENLE", 10, "Tahtayı çözülebilir yeni bir düzene sokar (-10 Puan)"),
    DOUBLE_SCORE("ÇİFTE HAMLE", 5, "Bir sonraki doğru eşleşmenin puanını ikiye katlar (-5 Puan)")
}

data class OperationFeedback(
    val isCorrect: Boolean,
    val expressionText: String,
    val explanationText: String,
    val operation: OperationType,
    val operands: List<Int>,
    val result: Int,
    val pointsDelta: Int,
    val isGoldenRescue: Boolean = false
)
