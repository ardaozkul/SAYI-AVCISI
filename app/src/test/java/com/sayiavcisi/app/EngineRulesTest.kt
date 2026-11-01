package com.sayiavcisi.app

import com.sayiavcisi.app.engine.BoardBuilder
import com.sayiavcisi.app.engine.BoardDoctor
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GameSession
import com.sayiavcisi.app.engine.ProgressRules
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
import org.junit.Assert.assertNull
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

    private fun play(session: GameSession, coords: List<Pair<Int, Int>>): MatchResult {
        session.beginSelection(coords.first())
        coords.drop(1).forEach { session.dragInto(it) }
        session.endGesture(true)
        return session.lastResult!!
    }

    // ---- MATEMATİK: bütün kombinasyonlar eşit kabul edilir ------------------------------

    @Test
    fun toplamadaButunKombinasyonlarKabulEdilir() {
        for (values in RuleEngine.combosFor(RuleKind.SUM, 7, 2)) {
            assertTrue("$values reddedilmemeli", RuleEngine.mathMatches(values, RuleKind.SUM, 7))
            assertTrue(
                "ters sıra da kabul edilmeli",
                RuleEngine.mathMatches(values.reversed(), RuleKind.SUM, 7)
            )
        }
        for (values in RuleEngine.combosFor(RuleKind.SUM, 7, 3)) {
            assertTrue("$values", RuleEngine.mathMatches(values, RuleKind.SUM, 7))
        }
        for (values in RuleEngine.combosFor(RuleKind.SUM, 7, 4)) {
            assertTrue("$values", RuleEngine.mathMatches(values, RuleKind.SUM, 7))
        }
        assertTrue(RuleEngine.mathMatches(listOf(1, 4), RuleKind.SUM, 5))
        assertTrue(RuleEngine.mathMatches(listOf(2, 2, 1), RuleKind.SUM, 5))
        assertTrue(RuleEngine.mathMatches(listOf(0, 1, 0, 4), RuleKind.SUM, 5))
        assertFalse(RuleEngine.mathMatches(listOf(1, 5), RuleKind.SUM, 5))
    }

    @Test
    fun hedefinTumKombinasyonlariDestedeVar() {
        assertEquals(
            setOf(listOf(0, 4), listOf(1, 3), listOf(2, 2)),
            RuleEngine.combosFor(RuleKind.SUM, 4, 2).map { it.sorted() }.toSet()
        )
        // 1+4 kadar 2+3 de üretilmeli: 4 toplamı için 2'li ve 3'lü desteler eksiksiz.
        assertTrue(RuleEngine.combosFor(RuleKind.SUM, 4, 3).isNotEmpty())
        assertEquals(
            RuleEngine.combosFor(RuleKind.SUM, 4, 3).size,
            RuleEngine.combosFor(RuleKind.SUM, 4, 3).distinct().size
        )
    }

    @Test
    fun tahtayaKombinasyonlarEsitDagitilir() {
        val base = CurriculumRepository.levelById("A4")!!
        val spec = base.copy(gridSize = 4, groupSizes = listOf(2))
        val board = BoardBuilder.build(spec, Random(5), 5)
        val pairs = board.plan.map { group ->
            group.map { coord -> board.cells.first { it.coord == coord }.value }.sorted()
        }.toSet()
        assertEquals(
            "0+4, 1+3 ve 2+2 aynı tahtada bulunmalı",
            RuleEngine.combosFor(RuleKind.SUM, 4, 2).map { it.sorted() }.toSet(),
            pairs
        )
    }

    @Test
    fun cikarmaCarpmaBolmeSiraBagimsiz() {
        assertTrue(RuleEngine.mathMatches(listOf(9, 4), RuleKind.DIFF, 5))
        assertTrue(RuleEngine.mathMatches(listOf(4, 9), RuleKind.DIFF, 5))
        assertTrue(RuleEngine.mathMatches(listOf(3, 8), RuleKind.PRODUCT, 24))
        assertTrue(RuleEngine.mathMatches(listOf(8, 3), RuleKind.PRODUCT, 24))
        assertTrue(RuleEngine.mathMatches(listOf(24, 6), RuleKind.QUOTIENT, 4))
        assertTrue(RuleEngine.mathMatches(listOf(6, 24), RuleKind.QUOTIENT, 4))
        assertFalse(RuleEngine.mathMatches(listOf(5, 0), RuleKind.QUOTIENT, 5))
    }

    @Test
    fun dortBlokluGrupYalnizcaToplamada() {
        assertNull(RuleEngine.operandsFor(RuleKind.DIFF, 3, 4, Random(1)))
        assertNull(RuleEngine.operandsFor(RuleKind.PRODUCT, 12, 3, Random(1)))
        val four = RuleEngine.operandsFor(RuleKind.SUM, 9, 4, Random(1))
        assertNotNull(four)
        assertEquals(4, four!!.size)
        assertEquals(9, four.sum())
    }

    // ---- GEOMETRİ: komşuluk, düz ve çapraz ---------------------------------------------

    @Test
    fun duzSonraCaprazaDonusGecerli() {
        val active = setOf(0 to 0, 0 to 1, 1 to 2)
        assertNull(
            "önce düz sonra çapraz komşu zinciri kabul edilmeli",
            RuleEngine.geometryProblem(listOf(0 to 0, 0 to 1, 1 to 2), active, MatchRuleMode.LINE_OF_SIGHT)
        )
        val four = setOf(0 to 0, 1 to 0, 1 to 1, 2 to 1)
        assertNull(RuleEngine.geometryProblem(listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1), four, MatchRuleMode.LINE_OF_SIGHT))
    }

    @Test
    fun aralarindaBlokKalmayanUzakTaslarKomsudur() {
        val active = setOf(0 to 0, 0 to 4)
        assertNull(
            "arada taş yoksa 2 + 5 gibi uzak taşlar eşleşir",
            RuleEngine.geometryProblem(listOf(0 to 0, 0 to 4), active, MatchRuleMode.LINE_OF_SIGHT)
        )
        val blocked = setOf(0 to 0, 0 to 2, 0 to 4)
        assertNotNull(RuleEngine.geometryProblem(listOf(0 to 0, 0 to 4), blocked, MatchRuleMode.LINE_OF_SIGHT))
    }

    @Test
    fun caprazVeTersYonlerGecerli() {
        val active = setOf(0 to 0, 1 to 1, 2 to 2, 3 to 3)
        assertNull(RuleEngine.geometryProblem(listOf(3 to 3, 2 to 2), active, MatchRuleMode.LINE_OF_SIGHT))
        assertNull(
            RuleEngine.geometryProblem(listOf(2 to 2, 1 to 1, 0 to 0), setOf(0 to 0, 1 to 1, 2 to 2), MatchRuleMode.LINE_OF_SIGHT)
        )
    }

    @Test
    fun komsuOlmayanVeAyniHattaOlmayanSecimReddedilir() {
        val active = setOf(0 to 0, 1 to 2, 2 to 2)
        assertNotNull(RuleEngine.geometryProblem(listOf(0 to 0, 1 to 2), active, MatchRuleMode.LINE_OF_SIGHT))
        assertNotNull(RuleEngine.geometryProblem(listOf(0 to 0, 1 to 2, 2 to 2), active, MatchRuleMode.LINE_OF_SIGHT))
    }

    // ---- TAHTA ÜRETİMİ ----------------------------------------------------------------

    @Test
    fun herBolumTahtasiTamamenKapanirVeHerGrupTekHedefiSaglar() {
        for (spec in CurriculumRepository.allLevels) {
            for (seed in listOf(1, 42)) {
                val board = BoardBuilder.build(spec, Random(seed), seed)
                val covered = board.plan.flatten()
                assertEquals("plan hücreleri tekil olmalı (${spec.levelId})", covered.size, covered.distinct().size)
                assertEquals("${spec.levelId} tahtası eksiksiz planlanmalı", spec.gridSize * spec.gridSize, covered.size)

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
                    assertTrue(
                        "${spec.levelId} değerleri hedefin kombinasyonlarından olmalı",
                        RuleEngine.combosFor(spec.ruleKind, spec.target, group.size)
                            .map { it.sorted() }
                            .contains(values.sorted())
                    )
                }
            }
        }
    }

    @Test
    fun planSirasiTakipEdilinceTahtaTamamenTemizlenir() {
        for (spec in CurriculumRepository.allLevels) {
            val s = session(spec, seed = spec.stageIndex * 97 + spec.dayNumber)
            for (group in s.board.plan) {
                play(s, group)
            }
            assertTrue("${spec.levelId} planla bitmeli", s.isFinished)
            assertTrue(s.board.isCleared())
        }
    }

    @Test
    fun rastgeleOynandigindaAslaTikanilmaz() {
        for (spec in CurriculumRepository.allLevels) {
            val s = session(spec, seed = 2026)
            val rng = Random(2026)
            var guard = 0
            while (!s.isFinished && guard++ < 300) {
                val moves = BoardDoctor.validMoves(s.board, spec)
                if (moves.isEmpty()) {
                    assertTrue("${spec.levelId} yıldız desteği üretmeli", s.starRescue())
                    assertNotNull("${spec.levelId} destekten sonra hamle olmalı", BoardDoctor.hint(s.board, spec))
                    continue
                }
                play(s, moves[rng.nextInt(moves.size)])
            }
            assertTrue("${spec.levelId} ${guard} turda bitmedi", s.isFinished)
        }
    }

    // ---- SÜRÜKLEME + OTOMATİK KONTROL --------------------------------------------------

    @Test
    fun parmakSurukleyinceDugmesizKontrolEdilir() {
        val spec = CurriculumRepository.levelById("A7")!!
        val s = session(spec)
        val move = BoardDoctor.validMoves(s.board, spec).first { it.size >= 2 }
        val result = play(s, move)
        assertTrue(result.accepted)
        assertTrue(s.levelScore > 0)
        assertTrue(s.correctCount == 1)
    }

    @Test
    fun ucVeDortTasliKombodaBuyukBonusVeKutlama() {
        val quadSpec = LevelSpec(
            levelId = "T4",
            dayNumber = 1,
            stageIndex = 1,
            title = "4 toplama",
            subtitle = "",
            segmentLabel = "+",
            minutes = 5,
            worldKey = "toplama",
            gridSize = 4,
            ruleKind = RuleKind.SUM,
            target = 6,
            groupSizes = listOf(4)
        )
        val s = session(quadSpec)
        assertEquals(4, s.maxGroupSize)
        val group = s.board.plan.first { it.size == 4 }
        val result = play(s, group)
        assertTrue(result.accepted)
        assertEquals(4, result.comboSize)
        assertEquals(4, s.comboSize)
        assertEquals(12, s.comboBonus)
        assertEquals(1, s.comboTick)
        assertTrue(s.levelScore >= 4 + 12)
    }

    @Test
    fun yanlisHamlePuanDusurur() {
        val spec = CurriculumRepository.levelById("A7")!!
        val s = session(spec)
        play(s, BoardDoctor.validMoves(s.board, spec).first())
        val bridge = fakeBridgeOf(s)
        val before = s.levelScore
        assertTrue(before >= ScoreEngine.WRONG_PENALTY)

        val active = s.board.activeCells()
        val bad = active.flatMap { a -> active.map { b -> a to b } }.firstOrNull { (a, b) ->
            a.id != b.id && a.value + b.value != spec.target &&
                RuleEngine.geometryProblem(listOf(a.coord, b.coord), s.board.activeCoords(), s.board.ruleMode) == null
        }
        assertNotNull(bad)
        val res = play(s, listOf(bad!!.first.coord, bad.second.coord))
        assertFalse(res.accepted)
        assertEquals(-ScoreEngine.WRONG_PENALTY, res.pointsDelta)
        assertEquals(before - ScoreEngine.WRONG_PENALTY, s.levelScore)
        assertEquals(1, s.mistakes)
        assertEquals(0, s.streak)
        assertEquals(1, s.correctCount)
        assertEquals(1, bridge.attempts.count { !it.first })
    }

    @Test
    fun gunlukTavanBolumPuaniniKesmez() {
        val spec = CurriculumRepository.levelById("A7")!!
        val s = session(spec)
        val bridge = fakeBridgeOf(s)
        bridge.gross = ScoreEngine.DAILY_CAP
        val res = play(s, BoardDoctor.validMoves(s.board, spec).first())
        assertTrue(res.pointsDelta > 0)
        assertEquals(ScoreEngine.DAILY_CAP, bridge.gross)
        assertEquals(res.pointsDelta, s.levelScore)
    }

    // ---- YILDIZ DESTEĞİ ----------------------------------------------------------------

    @Test
    fun hamleKalmayanTahtaYildizDestegiyleCozulur() {
        val spec = CurriculumRepository.levelById("A4")!!
        val cells = ArrayList<BlockCell>()
        for (r in 0 until 6) {
            for (c in 0 until 6) {
                cells.add(BlockCell(id = r * 6 + c, row = r, col = c, value = 9))
            }
        }
        val stuck = Board(gridSize = 6, cells = cells, plan = emptyList(), ruleMode = MatchRuleMode.LINE_OF_SIGHT, seed = 1)
        val s = session(spec)
        s.board = stuck
        assertNull(BoardDoctor.hint(s.board, spec))
        assertTrue(s.starRescue())
        assertEquals(1, s.rescueTick)
        assertTrue(s.isRescueUsed)
        assertNotNull("destekten sonra geçerli hamle olmalı", BoardDoctor.hint(s.board, spec))
        assertTrue(
            "desteklenen tahtanın tüm grupları hedefi tutar",
            s.board.plan.all { g ->
                RuleEngine.mathMatches(
                    g.map { c -> s.board.cells.first { it.coord == c }.value },
                    spec.ruleKind,
                    spec.target
                )
            }
        )
    }

    // ---- İLERLEME / KAYIT --------------------------------------------------------------

    @Test
    fun kayitVeGeriYukleme() {
        val spec = CurriculumRepository.levelById("A7")!!
        val s = session(spec, seed = 99)
        play(s, s.board.plan[0])
        play(s, s.board.plan[1])
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

    @Test
    fun jokerHakkiVePuaniCalisir() {
        val spec = CurriculumRepository.levelById("A7")!!
        val s = session(spec)
        play(s, BoardDoctor.validMoves(s.board, spec).first())
        val before = fakeBridgeOf(s).net
        assertTrue(s.useJoker(JokerType.HINT))
        assertEquals((before - JokerType.HINT.cost).coerceAtLeast(0), fakeBridgeOf(s).net)
        assertFalse(s.useJoker(JokerType.HINT))
    }

    // ---- MÜFREDAT ----------------------------------------------------------------------

    @Test
    fun dortBolumVeArdisikToplamaHedefleri() {
        assertEquals(4, CurriculumRepository.dayCount)
        val sums = CurriculumRepository.levelsForDay(1)
        assertEquals(30, sums.size)
        assertEquals((1..30).toList(), sums.map { it.target })
        assertEquals(4, sums.first().gridSize)
        assertEquals(4, sums[2].gridSize)
        assertEquals(6, sums[3].gridSize)
        assertEquals(6, sums.last().gridSize)
        assertEquals(listOf(2), sums.first().groupSizes)
        assertEquals(listOf(2, 3), CurriculumRepository.levelById("A7")!!.groupSizes)
        assertEquals(listOf(2, 3, 4), CurriculumRepository.levelById("A10")!!.groupSizes)
    }

    @Test
    fun cikarmaCarpimTablosuVeBolmeBolumleriVar() {
        assertTrue(CurriculumRepository.levelsForDay(2).all { it.ruleKind == RuleKind.DIFF })
        assertEquals(12, CurriculumRepository.levelsForDay(2).size)
        assertEquals(16, CurriculumRepository.levelsForDay(3).size)
        assertTrue(CurriculumRepository.levelsForDay(3).any { it.target == 36 })
        assertEquals(8, CurriculumRepository.levelsForDay(4).size)
        assertEquals(66, CurriculumRepository.allLevels.size)
        // Her bölüm açık: çarpım tablosunu denemek için kilit yok.
        for (spec in CurriculumRepository.allLevels) {
            assertTrue(ProgressRules.isLevelUnlocked(spec, emptySet()))
        }
    }

    @Test
    fun ustBaslikVeHedefMetniCocukDiliyle() {
        val sum = CurriculumRepository.levelById("A7")!!
        assertEquals("7 toplama", CurriculumRepository.topTitle(sum))
        assertEquals("Toplamı 7 yapan 2, 3 taşı seç", CurriculumRepository.objectiveText(sum))
        val diff = CurriculumRepository.levelById("S4")!!
        assertEquals("4 çıkarma", CurriculumRepository.topTitle(diff))
        assertEquals("Farkı 4 olan 2 taşı seç", CurriculumRepository.objectiveText(diff))
        val product = CurriculumRepository.levelById("M6")!!
        assertEquals("Çarpımı 6 olan 2 taşı seç", CurriculumRepository.objectiveText(product))
        assertTrue(CurriculumRepository.howToPlayText(sum).contains("sürükle"))
    }

    @Test
    fun sifirIcerikliIlkToplamalardaSifirDegerUretilebilir() {
        val spec = CurriculumRepository.levelById("A1")!!
        val board = BoardBuilder.build(spec, Random(4), 4)
        assertTrue(board.cells.any { it.value == 0 })
        assertTrue(board.cells.all { it.value in 0..spec.target })
    }

    // ---- yardımcılar -------------------------------------------------------------------

    private fun fakeBridgeOf(session: GameSession): FakeBridge {
        val field = GameSession::class.java.getDeclaredField("bridge")
        field.isAccessible = true
        return field.get(session) as FakeBridge
    }
}
