package com.sayiavcisi.app.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.Board
import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchResult
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationType
import kotlin.random.Random

/**
 * Oyunun durum makinesi. Kurallar Compose'dan ayrı tutulur, böylece birim testleriyle doğrulanabilir.
 *
 * - Çocuk bir bloğa basılı tutar, parmağını komşu bloklara sürükler; bırakınca hamle OTOMATİK
 *   kontrol edilir. "EŞLEŞTİR" düğmesi yoktur.
 * - Doğruysa taşlar yeşil parlayıp patlar, yanlışsa kırmızı olup sarsılır ve puan azalır.
 * - 2, 3 veya 4 blok seçilebilir. 3+ blokta KOMBO bonusu ve büyük kutlama vardır.
 * - Hiç geçerli hamle kalmaza YILDIZ DESTEĞİ ücretsiz olarak taşları yeniden dizer.
 */
class GameSession(
    val spec: LevelSpec,
    private val bridge: ProgressBridge,
    seed: Int = Random.nextInt(1, Int.MAX_VALUE),
    private val timedMode: Boolean = false,
    private val extraTimeSeconds: Int = 0
) {

    /** Cihaz ilerleme deposu ile oturum arasındaki ince arayüz. */
    interface ProgressBridge {
        fun grossEarnedToday(): Int
        fun addGrossEarned(delta: Int)
        fun addJokerSpending(amount: Int)
        fun recordAttempt(
            operation: OperationType,
            isCorrect: Boolean,
            operands: List<Int>,
            prompt: String,
            responseTimeMs: Long
        )
        fun addRewards(goldDelta: Int, diamondDelta: Int)
        fun markLevelCompleted(levelId: String)
        fun saveSnapshot(encoded: String)
        fun clearSnapshot()
    }

    private val allowedSizes = BoardBuilder.allowedSizesFor(spec)

    /** Aynı anda seçilebilecek en fazla blok (toplamada 4, diğer işlemlerde 2). */
    val maxGroupSize: Int = allowedSizes.max()

    private val levelTimeLimitSeconds: Int? = if (timedMode) {
        ((spec.timeLimitSeconds ?: spec.minutes * 60) + extraTimeSeconds).coerceAtLeast(30)
    } else null

    var board: Board by mutableStateOf(BoardBuilder.build(spec, Random(seed), seed))
        internal set

    var selected: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    var hinted: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    /** Son doğru hamlenin taşları: yeşil parlama efekti için. */
    var lastMatchedCoords: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    /** Son yanlış hamlenin taşları: kırmızı sarsılma efekti için. */
    var lastWrongCoords: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    var message: String by mutableStateOf(CurriculumRepository.objectiveText(spec))
        private set

    var levelScore by mutableIntStateOf(0)
        private set

    var rewardGold by mutableIntStateOf(0)
        private set

    var rewardDiamonds by mutableIntStateOf(0)
        private set

    var streak by mutableIntStateOf(0)
        private set

    var mistakes by mutableIntStateOf(0)
        private set

    var correctCount by mutableIntStateOf(0)
        private set

    var movesMade by mutableIntStateOf(0)
        private set

    /** Efekt tetikleyicileri: her doğru/yanlış/kurtarma hamlesinde artar; animasyonlar bunu izler. */
    var correctTick by mutableIntStateOf(0)
        private set

    var wrongTick by mutableIntStateOf(0)
        private set

    var rescueTick by mutableIntStateOf(0)
        private set

    /** 3 veya 4 taşlı kombinasyonlarda büyük kutlama efektini tetikler. */
    var comboSize by mutableIntStateOf(0)
        private set

    var comboBonus by mutableIntStateOf(0)
        private set

    var comboPoints by mutableIntStateOf(0)
        private set

    var comboTick by mutableIntStateOf(0)
        private set

    var isRescueUsed by mutableStateOf(false)
        private set

    var isFinished by mutableStateOf(false)
        private set

    var isTimeUp by mutableStateOf(false)
        private set

    var doubleNextActive by mutableStateOf(false)
        private set

    var lastResult: MatchResult? by mutableStateOf(null)
        private set

    var jokers: Map<JokerType, Int> by mutableStateOf(
        mapOf(JokerType.HINT to 1, JokerType.RESHAPE to 1, JokerType.DOUBLE to 1)
    )
        private set

    var secondsLeft: Int? = levelTimeLimitSeconds
        private set

    val clearedFraction: Float
        get() {
            val total = board.cells.size
            if (total == 0) return 0f
            return (total - board.activeCells().size).toFloat() / total.toFloat()
        }

    // ---- Parmakla seçim ----------------------------------------------------------------

    /** Bir bloğa basıldı: seçimi başlatır (ya da mevcut seçime ekler). */
    fun beginSelection(coord: Pair<Int, Int>) {
        if (isFinished || isTimeUp) return
        val cell = board.cells.firstOrNull { it.coord == coord } ?: return
        if (cell.isCleared) return
        hinted = emptyList()
        if (selected.contains(coord)) return
        selected = if (selected.size >= maxGroupSize) listOf(coord) else selected + coord
    }

    /** Parmak yeni bir komşu bloğa girdi. Geri dönülürse seçim kısalır. */
    fun dragInto(coord: Pair<Int, Int>) {
        if (isFinished || isTimeUp) return
        val cell = board.cells.firstOrNull { it.coord == coord } ?: return
        if (cell.isCleared) return
        val existing = selected.indexOf(coord)
        if (existing >= 0) {
            if (existing < selected.size - 1) selected = selected.take(existing + 1)
            return
        }
        if (selected.size >= maxGroupSize) return
        selected = selected + coord
    }

    /** Parmak kalktı. Sürükleyerek seçtiyse hamle anında denenir; tek dokunuşta beklenir. */
    fun endGesture(autoSubmit: Boolean) {
        if (autoSubmit && selected.size >= 2) submit()
    }

    /** Tek dokunuşla seçim (erişilebilirlik ve tıklayarak oynayan çocuklar için). */
    fun tapCell(coord: Pair<Int, Int>) {
        if (isFinished || isTimeUp) return
        val cell = board.cells.firstOrNull { it.coord == coord } ?: return
        if (cell.isCleared) return
        hinted = emptyList()
        selected = if (selected.contains(coord)) {
            selected.filter { it != coord }
        } else {
            val next = selected + coord
            if (next.size > maxGroupSize) next.take(maxGroupSize) else next
        }
    }

    // ---- Hamle -------------------------------------------------------------------------

    fun submit(): MatchResult {
        if (selected.size < 2) {
            message = "En az iki taş seç."
            lastResult = MatchResult(false, "", message, 0)
            return lastResult!!
        }
        val values = selected.map { coord -> board.cells.first { it.coord == coord }.value }
        val prompt = CurriculumRepository.objectiveText(spec)

        val geometryProblem = RuleEngine.geometryProblem(selected, board.activeCoords(), board.ruleMode)
        if (geometryProblem != null) return reject(geometryProblem, values, prompt)

        if (!RuleEngine.mathMatches(values, spec.ruleKind, spec.target)) {
            return reject(RuleEngine.failureMessage(values, spec.ruleKind, spec.target), values, prompt)
        }

        val expression = RuleEngine.expressionOf(values, spec.ruleKind)
        val raw = ScoreEngine.matchPoints(spec, values.size, streak + 1, doubleNextActive)
        val bonus = ScoreEngine.comboBonus(values.size)
        val room = (ScoreEngine.DAILY_CAP - bridge.grossEarnedToday()).coerceAtLeast(0)
        val dailyGrant = raw.coerceAtMost(room)

        val clearedCoords = selected.toSet()
        lastMatchedCoords = selected.toList()
        board = board.copy(
            cells = board.cells.map {
                if (clearedCoords.contains(it.coord)) it.copy(isCleared = true) else it
            }
        )
        selected = emptyList()
        hinted = emptyList()
        doubleNextActive = false
        streak += 1
        correctCount += 1
        movesMade += 1
        levelScore += raw
        correctTick += 1
        comboSize = values.size
        comboBonus = bonus
        comboPoints = raw
        if (values.size >= 3) comboTick += 1
        bridge.addGrossEarned(dailyGrant)
        bridge.recordAttempt(spec.operation, true, values, prompt, elapsedMs())
        lastAttemptStartedAt = System.currentTimeMillis()
        message = if (bonus > 0) {
            "$expression • ${values.size}'lü kombo!  +$raw puan"
        } else {
            "$expression • +$raw puan"
        }

        if (board.isCleared()) {
            finishLevel()
            lastResult = MatchResult(true, expression, message, raw, comboSize = values.size)
        } else {
            bridge.saveSnapshot(encodeSnapshot())
            val rescued = BoardDoctor.hint(board, spec) == null
            if (rescued) starRescue()
            lastResult = MatchResult(true, expression, message, raw, usedRescue = rescued, comboSize = values.size)
        }
        return lastResult!!
    }

    private fun reject(reason: String, values: List<Int>, prompt: String): MatchResult {
        mistakes += 1
        streak = 0
        wrongTick += 1
        lastWrongCoords = selected.toList()
        val before = levelScore
        levelScore = (levelScore - ScoreEngine.WRONG_PENALTY).coerceAtLeast(0)
        selected = emptyList()
        message = "$reason  (−${before - levelScore} puan)"
        bridge.recordAttempt(spec.operation, false, values, prompt, elapsedMs())
        lastAttemptStartedAt = System.currentTimeMillis()
        lastResult = MatchResult(false, "", message, -(before - levelScore), comboSize = values.size)
        return lastResult!!
    }

    /**
     * YILDIZ DESTEĞİ: hiç geçerli hamle kalmadığında tahtayı yeniden dizer.
     * Gerekirse hesabın dışında yardımcı blok eklenir; çocuk hiçbir zaman tıkanmaz.
     * Bedavadır: puan, joker veya seri gitmez. Ekranda yıldız efektiyle duyurulur.
     */
    fun starRescue(): Boolean {
        val outcome = BoardDoctor.rescue(board, spec) ?: return false
        board = outcome.board
        isRescueUsed = true
        rescueTick += 1
        lastWrongCoords = emptyList()
        val spawnNote = if (outcome.spawnedCount > 0) " ${outcome.spawnedCount} yardımcı blok eklendi." else ""
        message = "⭐ YILDIZ DESTEĞİ! Kalan taşlar yeniden dizildi.$spawnNote"
        return true
    }

    /** Ücretsiz ipucu: doğru bir grubu parlatır. Puan götürmez. */
    fun showHint(): Boolean {
        if (isFinished || isTimeUp) return false
        val move = BoardDoctor.hint(board, spec)
        if (move == null) {
            starRescue()
            return false
        }
        hinted = move
        message = "İpucu: parlayan ${move.size} taşı sürükle."
        return true
    }

    fun useJoker(type: JokerType): Boolean {
        if (isFinished || isTimeUp) return false
        if ((jokers[type] ?: 0) <= 0) {
            message = "Bu jokerden kalmadı."
            return false
        }
        val ok = when (type) {
            JokerType.HINT -> showHint()
            JokerType.RESHAPE -> {
                val outcome = BoardDoctor.rescue(board, spec)
                if (outcome == null) false else {
                    board = outcome.board
                    hinted = emptyList()
                    message = "Tahta yeniden dizerildi; artık her zaman çözülebilir."
                    true
                }
            }

            JokerType.DOUBLE -> {
                doubleNextActive = true
                message = "Çifte hamle hazır: bir sonraki doğru eşleşme 2 kat puan!"
                true
            }
        }
        if (!ok) return false
        jokers = jokers.toMutableMap().apply { put(type, (get(type) ?: 0) - 1) }
        bridge.addJokerSpending(type.cost)
        return true
    }

    fun onTick() {
        val left = secondsLeft ?: return
        if (isFinished || isTimeUp) return
        secondsLeft = (left - 1).coerceAtLeast(0)
        if (secondsLeft == 0) {
            isTimeUp = true
            message = "Süre doldu! Sorun değil, bölümü baştan oynayabilirsin."
        }
    }

    private fun finishLevel() {
        isFinished = true
        val flawless = mistakes == 0 && !isRescueUsed
        val bonus = ScoreEngine.timeBonus(secondsLeft)
        if (bonus > 0) levelScore += bonus
        val (gold, diamonds) = ScoreEngine.completionRewards(spec, flawless, isRescueUsed)
        rewardGold = gold
        rewardDiamonds = diamonds
        bridge.addRewards(gold, diamonds)
        bridge.markLevelCompleted(spec.levelId)
        bridge.clearSnapshot()
        message = if (flawless) {
            "BÖLÜM BİTTİ! Hatasız oynadın, +$diamonds elmas kazandın."
        } else {
            "BÖLÜM BİTTİ! +$gold altın kazandın."
        }
    }

    private var lastAttemptStartedAt: Long = System.currentTimeMillis()

    private fun elapsedMs(): Long = (System.currentTimeMillis() - lastAttemptStartedAt).coerceIn(0L, 60000L)

    /** Kaldığı yerden devam için tahta ve sayaçlar. */
    fun encodeSnapshot(): String {
        val cellsText = board.cells.joinToString(";") {
            "${it.row}:${it.col}:${it.value}:${if (it.isCleared) 1 else 0}"
        }
        return listOf(
            "v2",
            spec.levelId,
            board.gridSize.toString(),
            board.ruleMode.name,
            movesMade.toString(),
            listOf(levelScore, streak, mistakes, correctCount, secondsLeft ?: -1).joinToString(":"),
            jokers.entries.joinToString(",") { "${it.key.name}=${it.value}" },
            if (isRescueUsed) "1" else "0",
            cellsText
        ).joinToString("|")
    }

    /** Kaydı geri yükler. Bozuk/uyumsuz kayıtta false döner ve taze tahta kullanılır. */
    fun restoreSnapshot(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val parts = raw.split("|")
        if (parts.size != 9 || parts[0] != "v2" || parts[1] != spec.levelId) return false
        val gridSize = parts[2].toIntOrNull() ?: return false
        val mode = MatchRuleMode.values().firstOrNull { it.name == parts[3] } ?: return false
        val cells = parts[8].split(";").mapNotNull { entry ->
            val f = entry.split(":")
            if (f.size != 4) return@mapNotNull null
            val r = f[0].toIntOrNull() ?: return@mapNotNull null
            val c = f[1].toIntOrNull() ?: return@mapNotNull null
            val v = f[2].toIntOrNull() ?: return@mapNotNull null
            val cleared = f[3].toIntOrNull() ?: return@mapNotNull null
            if (r !in 0 until gridSize || c !in 0 until gridSize || v !in 0..99) return@mapNotNull null
            BlockCell(id = r * gridSize + c, row = r, col = c, value = v, isCleared = cleared == 1)
        }
        if (cells.size < 2 || cells.map { it.coord }.distinct().size != cells.size) return false

        val stats = parts[5].split(":").mapNotNull { it.toIntOrNull() }
        if (stats.size != 5) return false
        val counters = parts[6].split(",").mapNotNull { entry ->
            val kv = entry.split("=")
            val type = JokerType.values().firstOrNull { it.name == kv[0] } ?: return@mapNotNull null
            type to (kv.getOrNull(1)?.toIntOrNull() ?: 0)
        }.toMap()

        board = Board(gridSize, cells, board.plan, mode, board.seed)
        movesMade = parts[4].toIntOrNull() ?: 0
        levelScore = stats[0]
        streak = stats[1].coerceAtLeast(0)
        mistakes = stats[2].coerceAtLeast(0)
        correctCount = stats[3].coerceAtLeast(0)
        secondsLeft = stats[4].takeIf { it >= 0 }
        isRescueUsed = parts[7] == "1"
        if (counters.isNotEmpty()) jokers = counters
        isFinished = cells.none { !it.isCleared }
        selected = emptyList()
        hinted = emptyList()
        lastMatchedCoords = emptyList()
        lastWrongCoords = emptyList()
        return true
    }

    /** Bölümü baştan başlatır (süre dolduğunda veya çocuk istediğinde). */
    fun restart(newSeed: Int = Random.nextInt(1, Int.MAX_VALUE)) {
        board = BoardBuilder.build(spec, Random(newSeed), newSeed)
        selected = emptyList()
        hinted = emptyList()
        lastMatchedCoords = emptyList()
        lastWrongCoords = emptyList()
        levelScore = 0
        streak = 0
        mistakes = 0
        correctCount = 0
        movesMade = 0
        comboSize = 0
        comboBonus = 0
        comboPoints = 0
        isFinished = false
        isTimeUp = false
        isRescueUsed = false
        doubleNextActive = false
        secondsLeft = levelTimeLimitSeconds
        jokers = mapOf(JokerType.HINT to 1, JokerType.RESHAPE to 1, JokerType.DOUBLE to 1)
        message = CurriculumRepository.objectiveText(spec)
        lastAttemptStartedAt = System.currentTimeMillis()
    }
}
