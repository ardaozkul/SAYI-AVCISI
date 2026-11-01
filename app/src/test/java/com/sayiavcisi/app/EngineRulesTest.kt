package com.sayiavcisi.app

import com.sayiavcisi.app.engine.BoardBuilder
import com.sayiavcisi.app.engine.BoardDoctor
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GameSession
import com.sayiavcisi.app.engine.RuleEngine
import com.sayiavcisi.app.engine.ScoreEngine
import com.sayiavcisi.app.model.BlockCell
import com.sayiavcisi.app.model.Board
import com.sayiavcisi.app.model.JokerType
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.MatchResult
import com.sayiavcisi.app.model.MatchRuleMode
import com.sayiavcisi.app.model.OperationType
import com.sayiavcisi.app.model.RuleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Sahte ilerleme deposu: oturumun puan/ödül/kayıt çağrılarını tutar. */
private class FakeBridge : GameSession.ProgressBridge {
    var gross = 0
    var spent = 0
    var gold = 0
    var diamonds = 0
    var snapshot: String? = null
    val completedLevels = mutableSetOf<String>()
    val attempts = mutableListOf<Pair<Boolean, List<Int>>>()

    /** Çocuğun gördüğü günlük puan: kazanımdan joker harcaması düşülür, 0'ın altına inmez. */
    val net: Int get() = (gross - spent).coerceAtLeast(0)

    override fun grossEarnedToday(): Int = gross
    override fun addGrossEarned(delta: Int) {
        gross = (gross + delta).coerceAtMost(ScoreEngine.DAILY_CAP)
    }

    override fun addJokerSpending(amount: Int) {
        spent += amount
    }

    override fun recordAttempt(
        operation: OperationType,
        isCorrect: Boolean,
        operands: List<Int>,
        prompt: String,
        responseTimeMs: Long
    ) {
        attempts.add(isCorrect to operands)
    }

    override fun addRewards(goldDelta: Int, diamondDelta: Int) {
        gold += goldDelta
        diamonds += diamondDelta
    }

    override fun markLevelCompleted(levelId: String) {
        completedLevels.add(levelId)
    }

    override fun saveSnapshot(encoded: String) {
        snapshot = encoded
    }

    override fun clearSnapshot() {
        snapshot = null
    }
}

class EngineRulesTest {

    private fun session(spec: LevelSpec, seed: Int = 12345, timed: Boolean = false) =
        GameSession(spec, FakeBridge(), seed, timed)

    private fun playGroup(session: GameSession, group: List<Pair<Int, Int>>) {
        group.forEach { session.tapCell(it) }
        if (session.selected.size >= 2) session.submit()
    }

    // ---- TEK HEDEF KURALI: sıra bağımsızlığı (1.x'teki "bazen oluyor bazen olmuyor" hatasının panzehiri)

    @Test
    fun toplamaSiraBagimsiz() {
        val a = RuleEngine.mathMatches(listOf(0, 1), RuleKind.SUM, 1)
        val b = RuleEngine.mathMatches(listOf(1, 0), RuleKind.SUM, 1)
        assertTrue(a && b)
        assertTrue(RuleEngine.mathMatches(listOf(2, 2, 3), RuleKind.SUM, 7))
        assertTrue(RuleEngine.mathMatches(listOf(3, 2, 2), RuleKind.SUM, 7))
        assertFalse(RuleEngine.mathMatches(listOf(0, 1), RuleKind.SUM, 2))
    }

    @Test
    fun cikarmaMutlakFarkHangiSirlaSecilirseSecilsinAyniSonucuVerir() {
        assertTrue(RuleEngine.mathMatches(listOf(9, 4), RuleKind.DIFF, 5))
        assertTrue(RuleEngine.mathMatches(listOf(4, 9), RuleKind.DIFF, 5))
        assertFalse(RuleEngine.mathMatches(listOf(4, 9), RuleKind.DIFF, 4))
        // ikiz sayılar = fark 0
        assertTrue(RuleEngine.mathMatches(listOf(7, 7), RuleKind.DIFF, 0))
    }

    @Test
    fun carpmaVeBolmeSiraBagimsiz() {
        assertTrue(RuleEngine.mathMatches(listOf(3, 8), RuleKind.PRODUCT, 24))
        assertTrue(RuleEngine.mathMatches(listOf(8, 3), RuleKind.PRODUCT, 24))
        assertTrue(RuleEngine.mathMatches(listOf(24, 6), RuleKind.QUOTIENT, 4))
        assertTrue(RuleEngine.mathMatches(listOf(6, 24), RuleKind.QUOTIENT, 4))
        assertFalse(RuleEngine.mathMatches(listOf(24, 5), RuleKind.QUOTIENT, 4))
    }

    @Test
    fun sifiraBolumeHamleYok() {
        assertFalse(RuleEngine.mathMatches(listOf(0, 0), RuleKind.QUOTIENT, 0))
        assertFalse(RuleEngine.mathMatches(listOf(5, 0), RuleKind.QUOTIENT, 5))
        assertTrue(RuleEngine.mathMatches(listOf(0, 5), RuleKind.QUOTIENT, 0))
    }

    @Test
    fun yanlisHedefOturumdaReddedilirVePuanDusurmez() {
        val spec = CurriculumRepository.levelById("D1_S3")!! // 5x5, toplamı 5, 2 veya 3 taş
        val s = session(spec)
        val bridge = fakeBridgeOf(s)
        val netBefore = bridge.net
        val active = s.board.activeCells()
        val badPair = active.flatMap { a -> active.map { b -> a to b } }.firstOrNull { (a, b) ->
            a.id != b.id && a.value + b.value != spec.target &&
                RuleEngine.geometryProblem(listOf(a.coord, b.coord), s.board.activeCoords(), s.board.ruleMode) == null
        }
        assertNotNull("Test tahtasında hedefi tutmayan bir çift bulunmalı", badPair)
        val res = badPair!!.let { (a, b) ->
            s.tapCell(a.coord)
            s.tapCell(b.coord)
            s.submit()
        }
        assertFalse(res.accepted)
        assertEquals(0, res.pointsDelta)
        assertEquals(netBefore, bridge.net)
        assertEquals(1, s.mistakes)
        assertEquals(0, s.streak)
        assertEquals(0, s.correctCount)
    }

    // ---- GEOMETRİ

    @Test
    fun aradakiBlokKalkincayaKadarEslesmeYok() {
        val cells = listOf(
            BlockCell(0, 0, 0, 3),
            BlockCell(1, 0, 1, 4),
            BlockCell(2, 0, 2, 4)
        )
        val active = cells.map { it.coord }.toSet()
        assertNotNull(RuleEngine.geometryProblem(listOf(0 to 0, 0 to 2), active, MatchRuleMode.LINE_OF_SIGHT))
        val withoutMiddle = cells.filter { it.id != 1 }.map { it.coord }.toSet()
        assertEquals(null, RuleEngine.geometryProblem(listOf(0 to 0, 0 to 2), withoutMiddle, MatchRuleMode.LINE_OF_SIGHT))
    }

    @Test
    fun caprazVeTersYonlerGecerli() {
        val active = setOf(0 to 0, 1 to 1, 2 to 2, 3 to 3)
        // Bitişik çapraz çift, ters sırada seçilse de geçerli.
        assertEquals(null, RuleEngine.geometryProblem(listOf(3 to 3, 2 to 2), active, MatchRuleMode.LINE_OF_SIGHT))
        // Üç taşlık çapraz hat, sondan başa doğru okunsa da geçerli.
        assertEquals(
            null,
            RuleEngine.geometryProblem(listOf(2 to 2, 1 to 1, 0 to 0), setOf(0 to 0, 1 to 1, 2 to 2), MatchRuleMode.LINE_OF_SIGHT)
        )
        // Arada aktif blok olan iki uç geçersiz (önce aradaki kalkmalı).
        assertNotNull(RuleEngine.geometryProblem(listOf(0 to 0, 2 to 2), active, MatchRuleMode.LINE_OF_SIGHT))
        // Aynı hatta olmayan seçim geçersiz.
        assertNotNull(RuleEngine.geometryProblem(listOf(0 to 0, 2 to 3), active, MatchRuleMode.LINE_OF_SIGHT))
    }

    @Test
    fun serbestModdaYonAranmaz() {
        val active = setOf(0 to 0, 3 to 1)
        assertEquals(null, RuleEngine.geometryProblem(listOf(0 to 0, 3 to 1), active, MatchRuleMode.FREE))
    }

    // ---- MÜFREDAT + TAHTA GARANTİLERİ

    @Test
    fun herBolumTahtasiTamamenKapanirVeHerGrupTekHedefiSaglar() {
        for (spec in CurriculumRepository.allLevels) {
            for (seed in listOf(1, 7, 42)) {
                val board = BoardBuilder.build(spec, Random(seed), seed)
                val covered = board.plan.flatten()
                assertEquals("plan hücreleri tekil olmalı (${spec.levelId})", covered.size, covered.distinct().size)
                val activeCoords = board.activeCoords()
                assertEquals("${spec.levelId} tahtası eksiksiz planlanmalı", activeCoords.size, covered.size)

                for (group in board.plan) {
                    assertTrue(
                        "${spec.levelId} grup boyutu kurala uygun olmalı",
                        group.size in BoardBuilder.allowedSizesFor(spec)
                    )
                    val values = group.map { c -> board.cells.first { it.coord == c }.value }
                    assertTrue(
                        "${spec.levelId} grubu hedefi tutmalı: $values -> ${spec.target}",
                        RuleEngine.mathMatches(values, spec.ruleKind, spec.target)
                    )
                }
            }
        }
    }

    @Test
    fun planSirasiTakipEdilinceTahtaKoskosaTemizlenir() {
        for (spec in CurriculumRepository.allLevels) {
            val s = session(spec, seed = spec.stageIndex * 97 + spec.dayNumber)
            for (group in s.board.plan) {
                playGroup(s, group)
            }
            assertTrue("${spec.levelId} planla bitmeli", s.isFinished)
            assertTrue(s.board.isCleared())
        }
    }

    @Test
    fun rastgeleOynandigindaAslaTikanilmaz() {
        for (spec in CurriculumRepository.allLevels) {
            for (seed in listOf(3, 11, 2024)) {
                val s = session(spec, seed = seed)
                val rng = Random(seed)
                var guard = 0
                while (!s.isFinished && guard++ < 400) {
                    val moves = BoardDoctor.validMoves(s.board, spec)
                    if (moves.isEmpty()) {
                        // Otomatik kurtarma devreye girmemişse elle dene; hedef: asla kilitlenmemek.
                        assertTrue("${spec.levelId} kurtarma üretmeli", s.goldenRescue())
                        assertNotNull("${spec.levelId} kurtarmadan sonra hamle olmalı", BoardDoctor.hint(s.board, spec))
                        continue
                    }
                    playGroup(s, moves[rng.nextInt(moves.size)])
                }
                assertTrue("${spec.levelId} ${guard} turda bitmedi", s.isFinished)
                assertTrue(s.board.isCleared())
            }
        }
    }

    @Test
    fun buyukFinalTahtasiYuzBlokVeTekHedefli() {
        val final = CurriculumRepository.levelById("D5_S5")!!
        assertEquals(10, final.gridSize)
        assertEquals(100, final.totalCells)
        val s = session(final, seed = 5)
        assertEquals(100, s.board.activeCells().size)
        assertEquals(30, final.target)
        val bridge = fakeBridgeOf(s)
        for (group in s.board.plan) playGroup(s, group)
        assertTrue(s.isFinished)
        assertTrue(bridge.completedLevels.contains("D5_S5"))
        assertTrue(bridge.gold > 0)
    }

    // ---- KURTARMA (ALTIN KURTARMA)

    @Test
    fun hamleKalmayanTahtaKurtarmaIleCozulurHalAlir() {
        val spec = CurriculumRepository.levelById("D1_S3")!! // 5x5, toplam 5, 2/3 grup
        // Hedefi hiçbir çiftin/üçlünün tutmadığı elle kurulmuş bir tahta
        val cells = ArrayList<BlockCell>()
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                cells.add(BlockCell(id = r * 5 + c, row = r, col = c, value = 9))
            }
        }
        val stuck = Board(gridSize = 5, cells = cells, plan = emptyList(), ruleMode = MatchRuleMode.LINE_OF_SIGHT, seed = 1)
        val s = session(spec)
        s.board = stuck
        assertEquals(null, BoardDoctor.hint(s.board, spec))
        assertTrue(s.goldenRescue())
        assertNotNull("kurtarmadan sonra geçerli hamle olmalı", BoardDoctor.hint(s.board, spec))
        assertTrue("kurtarılan tahtanın tüm grupları hedefi tutar", s.board.plan.all { g ->
            RuleEngine.mathMatches(g.map { c -> s.board.cells.first { it.coord == c }.value }, spec.ruleKind, spec.target)
        })
    }

    @Test
    fun tekBlokKalanTahtaSihirliBloklaTamamlanir() {
        val spec = CurriculumRepository.levelById("D1_S3")!!
        val single = Board(
            gridSize = 4,
            cells = listOf(BlockCell(id = 0, row = 2, col = 1, value = 99)),
            plan = emptyList(),
            ruleMode = MatchRuleMode.LINE_OF_SIGHT,
            seed = 1
        )
        val outcome = BoardDoctor.rescue(single, spec)
        assertNotNull(outcome)
        assertEquals(1, outcome!!.spawnedCount)
        assertNotNull(BoardDoctor.hint(outcome.board, spec))
    }

    // ---- PUANLAMA

    @Test
    fun gunlukTavanYuzPuan() {
        val spec = CurriculumRepository.levelById("D1_S1")!!
        val s = session(spec)
        val bridge = fakeBridgeOf(s)
        bridge.gross = ScoreEngine.DAILY_CAP
        val res = playFirstValidMove(s)
        assertEquals(0, res.pointsDelta)
        assertEquals(ScoreEngine.DAILY_CAP, bridge.gross)
    }

    @Test
    fun jokerPuanGotururVeHakDusur() {
        val spec = CurriculumRepository.levelById("D2_S6")!! // DIFF 6x6: ilk hamle 6 puan
        val s = session(spec)
        val bridge = fakeBridgeOf(s)
        val before = playFirstValidMove(s).pointsDelta
        assertTrue(before > JokerType.HINT.cost)
        assertTrue(s.useJoker(JokerType.HINT))
        assertEquals((before - JokerType.HINT.cost).coerceAtLeast(0), bridge.net)
        assertEquals(0, s.jokers[JokerType.HINT])
        assertFalse("joker hakkı bitince kullanılamaz", s.useJoker(JokerType.HINT))
    }

    @Test
    fun cifteHamlePuaniIkiKatlar() {
        val spec = CurriculumRepository.levelById("D2_S4")!!
        val s = session(spec)
        assertTrue(s.useJoker(JokerType.DOUBLE))
        assertTrue(s.doubleNextActive)
        val res = playFirstValidMove(s)
        val single = ScoreEngine.matchPoints(spec, 2, 1, false)
        assertTrue(res.pointsDelta > single)
        assertFalse(s.doubleNextActive)
    }

    // ---- KALDIGI YERDEN DEVAM

    @Test
    fun kayitVeGeriYukleme() {
        val spec = CurriculumRepository.levelById("D1_S5")!!
        val s = session(spec, seed = 99)
        playGroup(s, s.board.plan.first())
        playGroup(s, s.board.plan[1])
        val encoded = s.encodeSnapshot()

        val fresh = session(spec, seed = 99)
        assertTrue(fresh.restoreSnapshot(encoded))
        assertEquals(s.movesMade, fresh.movesMade)
        assertEquals(s.levelScore, fresh.levelScore)
        assertEquals(
            s.board.activeCells().map { "${it.row}:${it.col}:${it.value}" }.sorted(),
            fresh.board.activeCells().map { "${it.row}:${it.col}:${it.value}" }.sorted()
        )
        assertFalse(fresh.restoreSnapshot("bozuk-kayit"))
    }

    // ---- MÜFREDAT TASARIMI

    @Test
    fun besGunXestiyesVeYaklasikAltmisDakika() {
        assertEquals(30, CurriculumRepository.allLevels.size)
        for (day in 1..5) {
            val levels = CurriculumRepository.levelsForDay(day)
            assertEquals(6, levels.size)
            val minutes = CurriculumRepository.dayMinutes(day)
            assertTrue("gün $day ${minutes}dk", minutes in 55..65)
        }
    }

    @Test
    fun hedeflerBirdBaslarSifiriIcerirVeBuyur() {
        val day1 = CurriculumRepository.levelsForDay(1)
        assertEquals(1, day1.first().target)
        val zeroLevel = CurriculumRepository.allLevels.first { it.target == 0 }
        assertEquals(RuleKind.DIFF, zeroLevel.ruleKind)
        val sumTargets = CurriculumRepository.allLevels.filter { it.ruleKind == RuleKind.SUM }.map { it.target }
        assertEquals(1, sumTargets.min())
        assertTrue(sumTargets.max() >= 20)
        // onluk ızgarada büyük bölüm var
        assertTrue(CurriculumRepository.allLevels.any { it.gridSize == 10 && it.minutes >= 15 })
        // dört işlem de temsil edilmiş
        val kinds = CurriculumRepository.allLevels.map { it.ruleKind }.toSet()
        assertEquals(setOf(RuleKind.SUM, RuleKind.DIFF, RuleKind.PRODUCT, RuleKind.QUOTIENT), kinds)
    }

    @Test
    fun sifirIcerikliToplamaBolumlerindeSifirDegerUretilebilir() {
        val spec = CurriculumRepository.levelById("D1_S1")!!
        val board = BoardBuilder.build(spec, Random(4), 4)
        val zeroCount = board.cells.count { it.value == 0 }
        assertTrue("hedef 1 toplama tahtasında sıfır olmalı", zeroCount > 0)
        assertTrue(board.cells.all { it.value in 0..spec.target })
    }

    @Test
    fun hedefKartiMetniAcikVeCocukDiliyle() {
        val sum = CurriculumRepository.levelById("D1_S3")!!
        assertEquals("Toplamı 5 olan 2 veya 3 taşı seç", CurriculumRepository.objectiveText(sum))
        val diff = CurriculumRepository.levelById("D2_S4")!!
        assertEquals("Farkı 2 olan 2 taşı seç", CurriculumRepository.objectiveText(diff))
        val product = CurriculumRepository.levelById("D3_S4")!!
        assertEquals("Çarpımı 6 olan 2 taşı seç", CurriculumRepository.objectiveText(product))
        val free = CurriculumRepository.levelById("D1_S6")!!
        assertTrue(CurriculumRepository.howToPlayText(free).contains("serbest"))
    }

    // ---- yardımcılar

    private fun fakeBridgeOf(session: GameSession): FakeBridge {
        val field = GameSession::class.java.getDeclaredField("bridge")
        field.isAccessible = true
        return field.get(session) as FakeBridge
    }

    private fun playFirstValidMove(session: GameSession): MatchResult {
        val move = BoardDoctor.validMoves(session.board, session.spec).first()
        playGroup(session, move)
        return session.lastResult!!
    }
}
