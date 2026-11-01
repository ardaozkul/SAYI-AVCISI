package com.sayiavcisi.app.data

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.MatchRuleMode

/**
 * Yarım kalan oyunun cihaz içinde saklanabilir hafif metni (Bölüm 15: "kaldığı yerden devam").
 *
 * JSON bağımlılığı kullanılmadan, satır arası karakter taşımayan alanlarla kodlanır;
 * bozuk/eksik metin çözümlendiğinde null döner (oyun temiz tahtayla başlar).
 */
data class GameResumeSnapshot(
    val levelId: String,
    val boardSeed: Int,
    val gridSize: Int,
    val matchRuleMode: MatchRuleMode,
    val remainingTargetCount: Int,
    val hintJokers: Int,
    val shuffleJokers: Int,
    val doubleScoreJokers: Int,
    val streak: Int,
    val mistakes: Int,
    val cells: List<BlockCell>
) {
    val isFinished: Boolean get() = remainingTargetCount <= 0 || cells.all { it.isCleared }
}

object GameSnapshotCodec {

    private const val VERSION = "v1"
    private const val SECTION = "|"
    private const val ITEM = ";"
    private const val FIELD = ":"

    fun encode(snapshot: GameResumeSnapshot): String {
        val cellsText = snapshot.cells.joinToString(ITEM) { cell ->
            "${cell.row}$FIELD${cell.col}$FIELD${cell.value}$FIELD${if (cell.isCleared) 1 else 0}"
        }
        return listOf(
            VERSION,
            snapshot.levelId,
            snapshot.boardSeed.toString(),
            snapshot.gridSize.toString(),
            snapshot.matchRuleMode.ordinal.toString(),
            snapshot.remainingTargetCount.toString(),
            listOf(
                snapshot.hintJokers, snapshot.shuffleJokers, snapshot.doubleScoreJokers,
                snapshot.streak, snapshot.mistakes
            ).joinToString(FIELD),
            cellsText
        ).joinToString(SECTION)
    }

    fun decode(raw: String?, expectedLevelId: String, expectedCellCount: Int): GameResumeSnapshot? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(SECTION)
        if (parts.size != 8 || parts[0] != VERSION) return null

        val levelId = parts[1]
        if (levelId != expectedLevelId) return null
        if (levelId.any { it == SECTION[0] || it == ITEM[0] || it == FIELD[0] || !it.isLetterOrDigit() && it != '_' }) return null

        val seed = parts[2].toIntOrNull() ?: return null
        val gridSize = parts[3].toIntOrNull() ?: return null
        val modeOrdinal = parts[4].toIntOrNull() ?: return null
        val remainingTargets = parts[5].toIntOrNull() ?: return null
        val counters = parts[6].split(FIELD).mapNotNull { it.toIntOrNull() }
        if (counters.size != 5 || gridSize < 2 || remainingTargets < 0) return null
        if (modeOrdinal !in MatchRuleMode.entries.indices) return null

        val cells = parts[7].split(ITEM).mapNotNull { entry ->
            val f = entry.split(FIELD)
            if (f.size != 4) return@mapNotNull null
            val r = f[0].toIntOrNull() ?: return@mapNotNull null
            val c = f[1].toIntOrNull() ?: return@mapNotNull null
            val v = f[2].toIntOrNull() ?: return@mapNotNull null
            val cleared = f[3].toIntOrNull() ?: return@mapNotNull null
            if (r !in 0 until gridSize || c !in 0 until gridSize || cleared !in 0..1) return@mapNotNull null
            BlockCell(id = r * gridSize + c, row = r, col = c, value = v, isCleared = cleared == 1)
        }

        if (cells.size != expectedCellCount) return null

        return GameResumeSnapshot(
            levelId = levelId,
            boardSeed = seed,
            gridSize = gridSize,
            matchRuleMode = MatchRuleMode.entries[modeOrdinal],
            remainingTargetCount = remainingTargets,
            hintJokers = counters[0].coerceAtLeast(0),
            shuffleJokers = counters[1].coerceAtLeast(0),
            doubleScoreJokers = counters[2].coerceAtLeast(0),
            streak = counters[3].coerceAtLeast(0),
            mistakes = counters[4].coerceAtLeast(0),
            cells = cells
        )
    }
}
