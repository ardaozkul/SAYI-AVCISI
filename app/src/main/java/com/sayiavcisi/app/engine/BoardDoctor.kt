package com.sayiavcisi.app.engine

import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.Board
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.RuleKind
import kotlin.random.Random

/**
 * Oyunun "asla kilitlenme" sigortası.
 *
 * Her hamleden sonra geçerli hamle aranır. Hiç kalmadıysa ALTIN KURTARMA devreye girer:
 * kalan bloklar baştan dizilir, değerleri bölümün tek hedefini sağlayacak şekilde yeniden yazılır.
 * Şekil tek başına gruplanamıyorsa tahtaya sihirli (hesapta olmayan) yardımcı blok eklenir;
 * böylece çocuk hiçbir zaman "eşleşmeyen sayı kaldı, oyun bozuldu" durumunu görmez.
 */
object BoardDoctor {

    data class Rescue(val board: Board, val spawnedCount: Int, val groupCount: Int)

    fun validMoves(board: Board, spec: LevelSpec): List<List<Pair<Int, Int>>> =
        RuleEngine.findAllMoves(
            cells = board.cells,
            kind = spec.ruleKind,
            target = spec.target,
            groupSizes = BoardBuilder.allowedSizesFor(spec),
            mode = board.ruleMode
        )

    fun hint(board: Board, spec: LevelSpec): List<Pair<Int, Int>>? =
        validMoves(board, spec).firstOrNull()

    fun hasAnyMove(board: Board, spec: LevelSpec): Boolean =
        board.isCleared() || hint(board, spec) != null

    fun isStuck(board: Board, spec: LevelSpec): Boolean = !board.isCleared() && hint(board, spec) == null

    /** Kalan tahtayı yeniden kurar. Her zaman en az bir geçerli hamle olan bir tahta üretir. */
    fun rescue(board: Board, spec: LevelSpec, random: Random = Random.Default): Rescue? {
        val active = board.activeCoords()
        if (active.isEmpty()) return Rescue(board, 0, 0)

        val allowed = BoardBuilder.allowedSizesFor(spec)
        val groups = planTiling(board, active, allowed, random) ?: return null

        val values = mutableMapOf<Pair<Int, Int>, Int>()
        for (group in groups) {
            val operands = operandsFor(spec.ruleKind, spec.target, group.size, random) ?: return null
            group.forEachIndexed { i, coord -> values[coord] = operands[i] }
        }

        // Sihirli blok daha önce temizlenmiş bir kareye oturabilir; o kareyi "temizli" listesinde
        // bırakmak aynı koordinatta iki blok yaratırdı ve çocuk o bloğa dokunamaz hale gelirdi.
        val newActiveCoords = values.keys
        val clearedCells = board.cells.filter { it.isCleared && !newActiveCoords.contains(it.coord) }
        val newActive = values.entries.mapIndexed { index, (coord, value) ->
            BlockCell(id = clearedCells.size + index, row = coord.first, col = coord.second, value = value)
        }
        val rebuilt = Board(
            gridSize = board.gridSize,
            cells = clearedCells + newActive,
            plan = groups,
            ruleMode = board.ruleMode,
            seed = board.seed
        )
        return Rescue(rebuilt, spawnedCount = newActive.count { !active.contains(it.coord) }, groupCount = groups.size)
    }

    private fun planTiling(
        board: Board,
        active: Set<Pair<Int, Int>>,
        allowed: List<Int>,
        random: Random
    ): List<List<Pair<Int, Int>>>? {
        // 1) Bitişik, aynı hatta segmentlerle tam kaplama (en sağlam çözüm).
        repeat(60) { attempt ->
            val tiling = contiguousTiling(active, allowed, random, allowDiagonal = attempt % 2 == 0)
            if (tiling != null) return tiling
        }
        // 2) Satır bazlı gruplama + arta kalanlar için eş/spark blok.
        return rowBasedTiling(board, active, allowed)
    }

    private fun contiguousTiling(
        active: Set<Pair<Int, Int>>,
        allowed: List<Int>,
        random: Random,
        allowDiagonal: Boolean
    ): List<List<Pair<Int, Int>>>? {
        val dirs = if (allowDiagonal)
            listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        else listOf(0 to 1, 1 to 0)

        val remaining = active.toMutableSet()
        val groups = mutableListOf<List<Pair<Int, Int>>>()
        while (remaining.isNotEmpty()) {
            val anchor = remaining.sortedWith(compareBy({ it.first }, { it.second })).first()
            var placed: List<Pair<Int, Int>>? = null
            for ((dr, dc) in dirs.shuffled(random)) {
                if (placed != null) break
                for (size in allowed.shuffled(random)) {
                    if (size < 2) continue
                    val cells = (0 until size).map { anchor.first + dr * it to anchor.second + dc * it }
                    if (cells.all { remaining.contains(it) }) {
                        placed = cells
                        break
                    }
                }
            }
            val group = placed ?: return null
            groups.add(group)
            group.forEach { remaining.remove(it) }
        }
        return groups
    }

    /**
     * Satırdaki aktif blokları soldan sağa gruplar; gruplanamayan tek blok kalırsa
     * başka bir artayla veya boş bir kareye eklenen sihirli blokla eşleştirilir.
     */
    private fun rowBasedTiling(
        board: Board,
        active: Set<Pair<Int, Int>>,
        allowed: List<Int>
    ): List<List<Pair<Int, Int>>>? {
        val groups = mutableListOf<List<Pair<Int, Int>>>()
        val leftovers = mutableListOf<Pair<Int, Int>>()

        for (r in 0 until board.gridSize) {
            val rowCells = active.filter { it.first == r }.sortedBy { it.second }
            var i = 0
            while (i < rowCells.size) {
                val size = groupSize(rowCells.size - i, allowed)
                if (size < 2) {
                    leftovers.add(rowCells[i])
                    break
                }
                groups.add(rowCells.subList(i, i + size))
                i += size
            }
        }

        // Artakalanları birbirleriyle, olmazsa boş karelerden eklenen sihirli bloklarla eşleştir.
        val pending = leftovers.toMutableList()
        val occupied = active.toMutableSet()
        while (pending.isNotEmpty()) {
            val cell = pending.removeAt(0)
            val partnerIndex = pending.indices.firstOrNull { i ->
                geometryClear(listOf(cell, pending[i]), occupied)
            }
            if (partnerIndex != null) {
                val partner = pending.removeAt(partnerIndex)
                groups.add(listOf(cell, partner))
                continue
            }
            val free = freeNeighbour(board, cell, occupied) ?: return null
            groups.add(listOf(cell, free))
            occupied.add(free)
        }
        return groups.filter { it.size >= 2 }.ifEmpty { null }
    }

    private fun groupSize(remaining: Int, allowed: List<Int>): Int = when {
        remaining >= 4 && allowed.contains(4) && remaining % 4 == 0 -> 4
        remaining >= 3 && allowed.contains(3) && (remaining % 2 == 1 || !allowed.contains(2)) -> 3
        remaining >= 2 && allowed.contains(2) -> 2
        remaining >= 3 && allowed.contains(3) -> 3
        remaining >= 4 && allowed.contains(4) -> 4
        else -> remaining
    }

    private fun geometryClear(
        coords: List<Pair<Int, Int>>,
        withThemActive: Set<Pair<Int, Int>>
    ): Boolean = RuleEngine.geometryProblem(coords, withThemActive, com.sayiavcisi.app.model.MatchRuleMode.LINE_OF_SIGHT) == null

    /** Verilen bloğun yanına eş olabilecek boş kare (önce aynı satır, sonra sütun/çapraz). */
    private fun freeNeighbour(
        board: Board,
        cell: Pair<Int, Int>,
        occupied: Set<Pair<Int, Int>>
    ): Pair<Int, Int>? {
        val candidates = ArrayList<Pair<Int, Int>>()
        for (dc in listOf(1, -1)) {
            for (dist in 1 until board.gridSize) {
                val c = cell.second + dc * dist
                if (c in 0 until board.gridSize) candidates.add(cell.first to c)
            }
        }
        for (dr in listOf(1, -1)) {
            for (dist in 1 until board.gridSize) {
                val r = cell.first + dr * dist
                if (r in 0 until board.gridSize) candidates.add(r to cell.second)
            }
        }
        for (d in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) {
            for (dist in 1 until board.gridSize) {
                val r = cell.first + d.first * dist
                val c = cell.second + d.second * dist
                if (r in 0 until board.gridSize && c in 0 until board.gridSize) candidates.add(r to c)
            }
        }
        return candidates.firstOrNull { !occupied.contains(it) && geometryClear(listOf(cell, it), occupied + it) }
    }

    private fun operandsFor(kind: RuleKind, target: Int, size: Int, random: Random): List<Int>? {
        val operand = RuleEngine.operandsFor(kind, target, size, random)
        return if (operand != null && operand.size == size && operand.all { it in 0..99 }) operand else null
    }
}
