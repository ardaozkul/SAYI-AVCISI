package com.sayiavcisi.app.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.Board
import com.sayiavcisi.app.model.EndlessMission
import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchResult
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationType
import com.sayiavcisi.app.model.RuleKind
import kotlin.random.Random

/**
 * Oyunun durum makinesi. Kurallar Compose'dan ayrı tutulur, böylece birim testleriyle doğrulanabilir.
 *
 * - Çocuk bir bloğa basılı tutar, parmağını komşu bloklara sürükler; bırakınca hamle OTOMATİK
 *   kontrol edilir. "EŞLEŞTİR" düğmesi yoktur.
 * - Doğruysa taşlar yeşil parlayıp patlar, yanlışsa kırmızı olup sarsılır ve puan azalır.
 * - Kombolar sınırlanmaz: çocuk şartı sağladığı kadar taş seçebilir (en fazla
 *   RuleEngine.MAX_COMPOSE); 3'ten büyük her kombine büyük kutlama vardır.
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

        /** Sonsuz Av bitince ayrı yeşil puan kaydı. Varsayılan: hiçbir şey yapma. */
        fun recordEndlessResult(score: Int, missions: Int, bestStreak: Int) {}
    }

    private val rng = Random(seed)
    val isEndless: Boolean get() = spec.endless

    /** Sonsuz Av'ın o anki görevi; normal bölümlerde null. */
    var mission: EndlessMission? by mutableStateOf(
        if (spec.endless) EndlessMissionFactory.first(rng) else null
    )
        private set

    /** Yeni görevin tahtaya yerleştirildiği koordinatlar (ipucu için). */
    var missionCoords: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    /**
     * Aynı anda seçilebilecek en fazla blok. Kombolar sınırlanmaz: çocuk şartı sağladığı
     * kadar taş toplayabilir (pratik tavan RuleEngine.MAX_COMPOSE). Bölme hep ikilidir.
     */
    val maxGroupSize: Int
        get() = if (spec.endless) (mission?.groupSize ?: 4)
        else if (spec.ruleKind == RuleKind.QUOTIENT) 2 else RuleEngine.MAX_COMPOSE

    /** Dokunarak oynayan çocuk için otomatik kontrolün bekleneceği taş sayısı. */
    val requiredGroupSize: Int
        get() = if (spec.endless) (mission?.groupSize ?: 4) else 2

    /** Sonsuz Av'ın ayrı yeşil puanı. Normal bölümlerin sarı puanına karışmaz. */
    var endlessScore by mutableIntStateOf(0)
        private set

    var missionsSolved by mutableIntStateOf(0)
        private set

    var bestEndlessStreak by mutableIntStateOf(0)
        private set

    private var endlessRecorded = false

    private val levelTimeLimitSeconds: Int? = if (timedMode && !spec.endless) {
        ((spec.timeLimitSeconds ?: spec.minutes * 60) + extraTimeSeconds).coerceAtLeast(30)
    } else null

    var board: Board by mutableStateOf(
        if (spec.endless) BoardBuilder.buildEndless(spec, rng)
        else BoardBuilder.build(spec, Random(seed), seed)
    )
        internal set

    var selected: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    var hinted: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    /** İpucu her istendiğinde artar; ekran ipucunun satırına kaydırmak için bunu izler. */
    var hintTick by mutableIntStateOf(0)
        private set

    /** Çıkarma/bölmede kısa süre görünen sıra uyarısı. */
    var orderHint: String? by mutableStateOf(null)
        private set

    /** Her sıra uyarısında artar; ekran uyarıyı ~1 saniye gösterip kapatır. */
    var orderHintTick by mutableIntStateOf(0)
        private set

    /** Son doğru hamlenin taşları: yeşil parlama efekti için. */
    var lastMatchedCoords: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    /** Son yanlış hamlenin taşları: kırmızı sarsılma efekti için. */
    var lastWrongCoords: List<Pair<Int, Int>> by mutableStateOf(emptyList())
        private set

    var message: String by mutableStateOf(
        if (spec.endless) "Görev hazırlanıyor…" else CurriculumRepository.objectiveText(spec)
    )
        private set

    init {
        if (spec.endless) {
            seedMissionOnBoard()
            message = missionMessage()
        }
    }

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

    /** 3 ve daha fazla taşlı kombolarda büyük kutlama efektini tetikler. */
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
            if (spec.endless) return (missionsSolved % 10).toFloat() / 10f
            val total = board.cells.size
            if (total == 0) return 0f
            return (total - board.activeCells().size).toFloat() / total.toFloat()
        }

    private val activeKind: RuleKind
        get() = if (spec.endless) (mission?.kind ?: RuleKind.SUM) else spec.ruleKind

    private val activeTarget: Int
        get() = if (spec.endless) (mission?.target ?: 0) else spec.target

    private val activeGroupSizes: List<Int>
        get() = if (spec.endless) (mission?.groupSizes ?: listOf(4))
        else BoardBuilder.allowedSizesFor(spec)

    private fun activeSpec(): LevelSpec =
        if (spec.endless && mission != null) {
            spec.copy(
                ruleKind = activeKind,
                target = activeTarget,
                groupSizes = activeGroupSizes
            )
        } else spec

    private fun missionMessage(): String {
        val m = mission ?: return "Yeni görev bekleniyor…"
        return "GÖREV: ${m.title}"
    }

    /**
     * Yeni görevin örnek sayılarını tahtadaki rastgele bir yatay hatta yerleştirir.
     * Böylece 6×100 tahtada görev anında oynanabilir olur.
     */
    private fun seedMissionOnBoard() {
        val m = mission ?: return
        val size = (m.groupSize).coerceIn(2, minOf(4, board.cols))
        val operands = m.seedOperands.take(size)
        if (operands.size != size) return
        // Görev, çocuğun ilk bakışta görebileceği üst satırlara yerleştirilir. 6×100 tahtada
        // rastgele bir satıra koymak görevi ekran dışına atıyordu; ipucu da artık oraya kaydırır.
        val row = rng.nextInt(0, minOf(10, board.rows).coerceAtLeast(1))
        val maxStart = (board.cols - size).coerceAtLeast(0)
        val startCol = if (maxStart > 0) rng.nextInt(0, maxStart + 1) else 0
        val coords = (0 until size).map { row to (startCol + it) }
        val byCoord = coords.mapIndexed { index, coord -> coord to operands[index] }.toMap()
        board = board.copy(
            cells = board.cells.map { cell ->
                val value = byCoord[cell.coord]
                if (value != null) {
                    cell.copy(value = value, isCleared = false, isHinted = false)
                } else {
                    cell
                }
            }
        )
        missionCoords = coords
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
        announceOrderHint()
    }

    /** Sıra gerektiren işlemlerde çocuğa kısa bir "önce büyük / önce bölünen" hatırlatması yapar. */
    private fun announceOrderHint() {
        val hint = RuleEngine.orderHint(activeKind) ?: return
        orderHint = hint
        orderHintTick += 1
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
        announceOrderHint()
    }

    /** Parmak kalktı. Sürükleyerek seçtiyse hamle anında denenir; tek dokunuşta beklenir. */
    fun endGesture(autoSubmit: Boolean) {
        if (autoSubmit && selected.size >= 2) submit()
    }

    /**
     * Sürükleme sırasında seçim hedefi tuttuysa anında kabul eder.
     * "EŞLEŞTİR" düğmesi olmadığı için doğru kombinasyon parmak kalkmadan patlar.
     */
    fun autoSubmitIfMatched(): Boolean {
        if (selected.size < 2) return false
        val values = selected.mapNotNull { coord -> board.cellAt(coord)?.value }
        if (values.size != selected.size) return false
        if (spec.endless && values.size != (mission?.groupSize ?: 4)) return false
        if (RuleEngine.geometryProblem(selected, board.activeCoords(), board.ruleMode) != null) return false
        if (!RuleEngine.mathMatches(values, activeKind, activeTarget)) return false
        submit()
        return true
    }

    /**
     * Tek tek dokunan çocuk için hızlı karar:
     * - Seçim zaten geçerliyse kabul eder.
     * - Geometri bozuksa ya da toplam hedefi aştıysa anında yanlış sayar.
     * - Toplama hedefe henüz ulaşmadıysa seçimi büyütmek için bekler.
     * Sonsuz Av'da görev 4/3/2 taş istiyorsa o sayı dolmadan karar vermez.
     */
    fun autoRejectIfDecisive(): Boolean {
        if (selected.size < 2 || isFinished || isTimeUp) return false
        val values = selected.mapNotNull { coord -> board.cellAt(coord)?.value }
        if (values.size != selected.size) return false

        if (spec.endless) {
            val required = mission?.groupSize ?: 4
            if (selected.size < required) {
                val geometry = RuleEngine.geometryProblem(selected, board.activeCoords(), board.ruleMode)
                if (geometry != null) {
                    submit()
                    return true
                }
                return false
            }
            submit()
            return true
        }

        val geometry = RuleEngine.geometryProblem(selected, board.activeCoords(), board.ruleMode)
        if (geometry != null) {
            submit()
            return true
        }
        if (RuleEngine.mathMatches(values, activeKind, activeTarget)) {
            // Kabul edilir ya da sıra hatasını cezasız uyarı olarak gösterir.
            submit()
            return true
        }
        // Kombolar sınırlı olmadığı için "hâlâ büyütülebilir" seçimde karar verilmez.
        if (isMaybeExtendable(values)) {
            message = extendableMessage(activeKind, values)
            return false
        }
        submit()
        return true
    }

    /**
     * Seçim, ek taşlarla şartı hâlâ sağlayabilir mi? Sağlayabilirse çocuğa süre tanınır;
     * kombolar sınırsız olduğu için yerde kalan seçim hemen "yanlış" sayılamaz.
     */
    private fun isMaybeExtendable(values: List<Int>): Boolean {
        if (selected.size >= maxGroupSize) return false
        return when (activeKind) {
            RuleKind.SUM -> values.sum() < activeTarget
            RuleKind.DIFF -> values.first() - values.drop(1).sum() > activeTarget
            RuleKind.PRODUCT -> {
                val product = values.reduce { a, b -> a * b }
                product != 0 && product < activeTarget && activeTarget % product == 0
            }

            RuleKind.QUOTIENT -> false
        }
    }

    /** Bekleme sırasında çocuğa gösterilen kısa durum metni. */
    private fun extendableMessage(kind: RuleKind, values: List<Int>): String = when (kind) {
        RuleKind.DIFF ->
            "Fark şu an ${values.first() - values.drop(1).sum()} · küçük bir sayı daha ekleyebilirsin."

        RuleKind.PRODUCT ->
            "Çarpım şu an ${values.reduce { a, b -> a * b }} · bir sayı daha ekleyebilirsin."

        else -> "Toplam ${values.sum()} · başka bir sayı daha ekleyebilirsin."
    }

    /** Tek dokunuşla seçim (erişilebilirlik ve tıklayarak oynayan çocuklar için). */
    fun tapCell(coord: Pair<Int, Int>) {
        if (isFinished || isTimeUp) return
        val cell = board.cells.firstOrNull { it.coord == coord } ?: return
        if (cell.isCleared) return
        hinted = emptyList()
        val wasSelected = selected.contains(coord)
        selected = if (wasSelected) {
            selected.filter { it != coord }
        } else {
            val next = selected + coord
            if (next.size > maxGroupSize) next.take(maxGroupSize) else next
        }
        if (!wasSelected) announceOrderHint()
    }

    // ---- Hamle -------------------------------------------------------------------------

    fun submit(): MatchResult {
        if (selected.size < 2) {
            message = "En az iki taş seç."
            lastResult = MatchResult(false, "", message, 0)
            return lastResult!!
        }
        val values = selected.map { coord -> board.cells.first { it.coord == coord }.value }
        val prompt = if (spec.endless) {
            mission?.title ?: "Sonsuz Av"
        } else {
            CurriculumRepository.objectiveText(spec)
        }

        val geometryProblem = RuleEngine.geometryProblem(selected, board.activeCoords(), board.ruleMode)
        if (geometryProblem != null) return reject(geometryProblem, values, prompt)

        // Sıra yalnızca oyun içinde önemlidir. Matematik tutuyor ama sıra yanlışsa
        // çocuğu cezalandırmadan seçimi temizle ve kısa bir hatırlatma göster.
        val mathMatches = RuleEngine.mathMatches(values, activeKind, activeTarget)
        if (mathMatches) {
            val orderProblem = RuleEngine.orderProblem(values, activeKind)
            if (orderProblem != null) return orderReject(orderProblem)
        }

        if (!mathMatches) {
            return reject(
                RuleEngine.failureMessage(values, activeKind, activeTarget),
                values,
                prompt
            )
        }

        if (spec.endless) return acceptEndless(values, prompt)

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

    /**
     * Sonsuz Av'da doğru görev: yeşil puan yazılır, temizlenen kareler ANINDA yenilenir,
     * yeni bir anlık görev gelir ve tahtaya oynanabilir bir örnek yerleştirilir.
     */
    private fun acceptEndless(values: List<Int>, prompt: String): MatchResult {
        val kind = activeKind
        val expression = RuleEngine.expressionOf(values, kind)
        val base = ScoreEngine.endlessMissionPoints(kind, values.size, streak + 1)
        val points = if (doubleNextActive) base * 2 else base
        val cleared = selected.toSet()
        lastMatchedCoords = selected.toList()
        val refreshed = board.cells.map { cell ->
            if (cleared.contains(cell.coord)) {
                cell.copy(value = rng.nextInt(1, 101), isCleared = false, isHinted = false)
            } else {
                cell
            }
        }
        board = board.copy(cells = refreshed)
        selected = emptyList()
        hinted = emptyList()
        doubleNextActive = false
        streak += 1
        correctCount += 1
        movesMade += 1
        missionsSolved += 1
        endlessScore += points
        bestEndlessStreak = maxOf(bestEndlessStreak, streak)
        correctTick += 1
        comboSize = values.size
        comboBonus = 0
        comboPoints = points
        if (values.size >= 3) comboTick += 1
        bridge.recordAttempt(kind.operation, true, values, prompt, elapsedMs())
        lastAttemptStartedAt = System.currentTimeMillis()
        mission = EndlessMissionFactory.next(mission, rng)
        seedMissionOnBoard()
        message = "GÖREV TAMAM · $expression · +$points yeşil puan"
        lastResult = MatchResult(
            accepted = true,
            expression = expression,
            message = message,
            pointsDelta = points,
            comboSize = values.size
        )
        return lastResult!!
    }

    /**
     * Sıra hatası: puan, seri ve hata sayacı korunur. Seçim temizlenir, kısa uyarı yanar.
     * Böylece çocuk "yanlış yaptım" hissi yerine "önce büyük/bölünen" hatırlatması görür.
     */
    private fun orderReject(reason: String): MatchResult {
        selected = emptyList()
        hinted = emptyList()
        orderHint = reason
        orderHintTick += 1
        message = reason
        lastResult = MatchResult(false, "", reason, 0)
        return lastResult!!
    }

    private fun reject(reason: String, values: List<Int>, prompt: String): MatchResult {
        mistakes += 1
        streak = 0
        wrongTick += 1
        lastWrongCoords = selected.toList()
        val before = levelScore
        if (!spec.endless) {
            levelScore = (levelScore - ScoreEngine.WRONG_PENALTY).coerceAtLeast(0)
        }
        selected = emptyList()
        message = if (spec.endless) {
            reason
        } else {
            "$reason  (−${before - levelScore} puan)"
        }
        bridge.recordAttempt(activeKind.operation, false, values, prompt, elapsedMs())
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
        if (spec.endless) return false
        val outcome = BoardDoctor.rescue(board, spec) ?: return false
        board = outcome.board
        isRescueUsed = true
        rescueTick += 1
        lastWrongCoords = emptyList()
        val spawnNote = if (outcome.spawnedCount > 0) " ${outcome.spawnedCount} yardımcı blok eklendi." else ""
        message = "Taşlar yeniden dizildi.$spawnNote"
        return true
    }

    /** Ücretsiz ipucu: doğru bir grubu parlatır. Puan götürmez. */
    fun showHint(): Boolean {
        if (isFinished || isTimeUp) return false
        hintTick += 1
        if (spec.endless) {
            val active = board.activeCoords()
            if (missionCoords.size >= 2 && missionCoords.all { active.contains(it) }) {
                hinted = missionCoords
            } else {
                seedMissionOnBoard()
                hinted = missionCoords
            }
            message = "İpucu: ${mission?.title ?: "görevi"} parlayan taşlarla tamamla."
            return true
        }
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
                if (spec.endless) {
                    board = board.copy(
                        cells = board.cells.map { it.copy(value = rng.nextInt(1, 101), isCleared = false) }
                    )
                    seedMissionOnBoard()
                    hinted = emptyList()
                    message = "Tahta yenilendi; yeni görev hazır."
                    true
                } else {
                    val outcome = BoardDoctor.rescue(board, spec)
                    if (outcome == null) false else {
                        board = outcome.board
                        hinted = emptyList()
                        message = "Tahta yeniden dizerildi; artık her zaman çözülebilir."
                        true
                    }
                }
            }

            JokerType.DOUBLE -> {
                doubleNextActive = true
                message = if (spec.endless) {
                    "Çifte hamle hazır: bir sonraki görev 2 kat yeşil puan!"
                } else {
                    "Çifte hamle hazır: bir sonraki doğru eşleşme 2 kat puan!"
                }
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

    /**
     * Sonsuz Av'ı bitirir ve YEŞİL puan kaydını ayrı olarak yazar.
     * Aynı oturumda iki kez çağrılsa bile kayıt ikiye katlanmaz.
     */
    fun finishEndless(): Int {
        if (!spec.endless) return 0
        if (!endlessRecorded) {
            bridge.recordEndlessResult(endlessScore, missionsSolved, bestEndlessStreak)
            endlessRecorded = true
        }
        isFinished = true
        message = "SONSUZ AV KAYDEDİLDİ · $endlessScore yeşil puan"
        lastResult = MatchResult(true, "", message, 0)
        return endlessScore
    }

    private var lastAttemptStartedAt: Long = System.currentTimeMillis()

    private fun elapsedMs(): Long = (System.currentTimeMillis() - lastAttemptStartedAt).coerceIn(0L, 60000L)

    /** Kaldığı yerden devam için tahta ve sayaçlar. Sonsuz Av kaydı ayrı tutulur. */
    fun encodeSnapshot(): String {
        if (spec.endless) return ""
        val cellsText = board.cells.joinToString(";") {
            "${it.row}:${it.col}:${it.value}:${if (it.isCleared) 1 else 0}"
        }
        return listOf(
            "v3",
            spec.levelId,
            board.rows.toString(),
            board.cols.toString(),
            board.ruleMode.name,
            movesMade.toString(),
            listOf(levelScore, streak, mistakes, correctCount, secondsLeft ?: -1).joinToString(":"),
            jokers.entries.joinToString(",") { "${it.key.name}=${it.value}" },
            if (isRescueUsed) "1" else "0",
            cellsText
        ).joinToString("|")
    }

    /** Kaydı geri yükler (v3 ve eski v2). Bozuk/uyumsuz kayıtta false döner. */
    fun restoreSnapshot(raw: String?): Boolean {
        if (spec.endless) return false
        if (raw.isNullOrBlank()) return false
        val parts = raw.split("|")
        val isV3 = parts.size == 10 && parts[0] == "v3"
        val isV2 = parts.size == 9 && parts[0] == "v2"
        if ((!isV3 && !isV2) || parts[1] != spec.levelId) return false

        val rows: Int
        val cols: Int
        val modeIndex: Int
        if (isV3) {
            rows = parts[2].toIntOrNull() ?: return false
            cols = parts[3].toIntOrNull() ?: return false
            modeIndex = 4
        } else {
            cols = parts[2].toIntOrNull() ?: return false
            rows = cols
            modeIndex = 3
        }
        if (rows < 2 || cols < 2 || rows > 100 || cols > 20) return false
        // Tablet sütun ayarı değişince eski tahta boyutuyla kayıt geri yüklenemez.
        if (rows != spec.rows || cols != spec.cols) return false
        val mode = MatchRuleMode.values().firstOrNull { it.name == parts[modeIndex] } ?: return false
        val movesIndex = modeIndex + 1
        val statsIndex = modeIndex + 2
        val jokersIndex = modeIndex + 3
        val rescueIndex = modeIndex + 4
        val cellsIndex = modeIndex + 5

        val cells = parts[cellsIndex].split(";").mapNotNull { entry ->
            val f = entry.split(":")
            if (f.size != 4) return@mapNotNull null
            val r = f[0].toIntOrNull() ?: return@mapNotNull null
            val c = f[1].toIntOrNull() ?: return@mapNotNull null
            val v = f[2].toIntOrNull() ?: return@mapNotNull null
            val cleared = f[3].toIntOrNull() ?: return@mapNotNull null
            if (r !in 0 until rows || c !in 0 until cols || v !in 0..99) return@mapNotNull null
            BlockCell(id = r * cols + c, row = r, col = c, value = v, isCleared = cleared == 1)
        }
        if (cells.size != rows * cols || cells.map { it.coord }.distinct().size != cells.size) return false

        val stats = parts[statsIndex].split(":").mapNotNull { it.toIntOrNull() }
        if (stats.size != 5) return false
        val counters = parts[jokersIndex].split(",").mapNotNull { entry ->
            val kv = entry.split("=")
            val type = JokerType.values().firstOrNull { it.name == kv[0] } ?: return@mapNotNull null
            type to (kv.getOrNull(1)?.toIntOrNull() ?: 0)
        }.toMap()

        board = Board(rows, cols, cells, board.plan, mode, board.seed)
        movesMade = parts[movesIndex].toIntOrNull() ?: 0
        levelScore = stats[0]
        streak = stats[1].coerceAtLeast(0)
        mistakes = stats[2].coerceAtLeast(0)
        correctCount = stats[3].coerceAtLeast(0)
        secondsLeft = stats[4].takeIf { it >= 0 }
        isRescueUsed = parts[rescueIndex] == "1"
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
        if (spec.endless) {
            mission = EndlessMissionFactory.first(rng)
            board = BoardBuilder.buildEndless(spec, rng)
            seedMissionOnBoard()
        } else {
            board = BoardBuilder.build(spec, Random(newSeed), newSeed)
        }
        selected = emptyList()
        hinted = emptyList()
        lastMatchedCoords = emptyList()
        lastWrongCoords = emptyList()
        orderHint = null
        orderHintTick = 0
        levelScore = 0
        endlessScore = 0
        missionsSolved = 0
        bestEndlessStreak = 0
        endlessRecorded = false
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
        message = if (spec.endless) missionMessage() else CurriculumRepository.objectiveText(spec)
        lastAttemptStartedAt = System.currentTimeMillis()
    }
}
