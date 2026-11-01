package com.sayiavcisi.app.model

/**
 * SAYI AVCISI 2.0 veri modeli.
 *
 * Temel ilke: BİR BÖLÜM = TEK HEDEF.
 * Tahtadaki her blok grubu aynı hedefi sağlar; çocuk hangi geçerli grubu seçerse seçsin
 * kurallar aynıdır. Böylece "bazen kabul eden bazen etmeyen" eşleşme durumu imkânsız hale gelir.
 */

enum class OperationType(val symbol: String, val title: String) {
    ADDITION("+", "Toplama"),
    SUBTRACTION("-", "Çıkarma"),
    MULTIPLICATION("×", "Çarpma"),
    DIVISION("÷", "Bölme")
}

/** Bölümün matematiksel hedef tanımı. */
enum class RuleKind(val operation: OperationType, val unitLabel: String) {
    SUM(OperationType.ADDITION, "toplam"),
    DIFF(OperationType.SUBTRACTION, "fark"),
    PRODUCT(OperationType.MULTIPLICATION, "çarpım"),
    QUOTIENT(OperationType.DIVISION, "bölüm");

    /** Bu hedef türünde sıranın önemli olup olmadığı. 2.0'da hiçbir hedef sıra gerektirmez. */
    val orderFree: Boolean get() = true
}

/**
 * Geometri kuralı:
 * - LINE_OF_SIGHT: seçilen bloklar aynı satır/sütun/çapraz üzerinde olmalı ve aralarında
 *   hâlâ temizlenmemiş bir blok bulunmamalı. Aradaki blok kalkınca arkadaki blok açılır.
 * - FREE: yön ve mesafe aranmaz, tahtadaki herhangi uygun bloklar eşleşebilir.
 */
enum class MatchRuleMode(val title: String, val description: String) {
    LINE_OF_SIGHT(
        "Doğrusal Avcı",
        "Aynı hatta (yatay, dikey, çapraz) ve arasında blok kalmamış sayıları eşleştir."
    ),
    FREE(
        "Serbest Avcı",
        "Tahtadaki herhangi uygun sayıları eşleştir; yön ve mesafe serbest."
    )
}

data class BlockCell(
    val id: Int,
    val row: Int,
    val col: Int,
    val value: Int,
    val isCleared: Boolean = false,
    val isHinted: Boolean = false
) {
    val coord: Pair<Int, Int> get() = Pair(row, col)
}

data class LevelSpec(
    val levelId: String,
    val dayNumber: Int,
    val stageIndex: Int,
    val title: String,
    val subtitle: String,
    val segmentLabel: String,
    val minutes: Int,
    val worldKey: String,
    val gridSize: Int,
    val ruleKind: RuleKind,
    val target: Int,
    /** Bir eşleşmede kullanılan blok sayıları. Çarpma/bölme için yalnızca [2]. */
    val groupSizes: List<Int>,
    val ruleMode: MatchRuleMode = MatchRuleMode.LINE_OF_SIGHT,
    val timeLimitSeconds: Int? = null,
    val focusZero: Boolean = false
) {
    val operation: OperationType get() = ruleKind.operation
    val totalCells: Int get() = gridSize * gridSize
    val plannedMoveCount: Int get() = if (totalCells % 3 == 0) totalCells / 3 else totalCells / 2
}

/** Üretilen tahta: bloklar + üreticinin bildiği garantili çözüm planı. */
data class Board(
    val gridSize: Int,
    val cells: List<BlockCell>,
    val plan: List<List<Pair<Int, Int>>>,
    val ruleMode: MatchRuleMode,
    val seed: Int
) {
    fun activeCells(): List<BlockCell> = cells.filterNot { it.isCleared }
    fun activeCoords(): Set<Pair<Int, Int>> = activeCells().map { it.coord }.toSet()
    fun isCleared(): Boolean = cells.none { !it.isCleared }
}

enum class JokerType(
    val title: String,
    /** Dar düğmelerde sığan kısa etiket; çocuk yine ne olduğunu anlar. */
    val shortTitle: String,
    val emoji: String,
    val cost: Int,
    val description: String
) {
    HINT("İPUCU", "İPUCU", "🔍", 5, "Doğru bir eşleşmeyi parlatarak gösterir (-5 puan)"),
    RESHAPE("YENİDEN DÜZENLE", "DÜZENLE", "🔄", 10, "Blokları yeniden dizer, her zaman çözülebilir (-10 puan)"),
    DOUBLE("ÇİFTE HAMLE", "ÇİFTE", "⚡", 5, "Sonraki doğru eşleşmenin puanını ikiye katlar (-5 puan)")
}

data class MatchResult(
    val accepted: Boolean,
    val expression: String,
    val message: String,
    val pointsDelta: Int,
    val usedRescue: Boolean = false
)
