package com.sayiavcisi.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import com.sayiavcisi.app.audio.SoundEffectsManager
import com.sayiavcisi.app.data.LocalProgressStorage
import com.sayiavcisi.app.engine.CurriculumRepository
import com.sayiavcisi.app.engine.GameSession
import com.sayiavcisi.app.engine.ProgressRules
import com.sayiavcisi.app.model.LevelSpec
import com.sayiavcisi.app.model.OperationType

/** Uygulama içi gezinme: giriş -> harita -> bölüm. */
private sealed class Route {
    data object Start : Route()
    data object Map : Route()
    data class Game(val levelId: String) : Route()
    data object Parent : Route()
}

/**
 * Uygulamanın kökü: ilerleme kaydını okur, ekranlar arasında gezinir ve
 * oyun oturumunu (GameSession) cihaz deposuna bağlar.
 */
@Composable
fun SayiAvcisiApp(storage: LocalProgressStorage) {
    var snapshot by remember { mutableStateOf(storage.loadSnapshot()) }
    var route by remember { mutableStateOf<Route>(Route.Start) }
    var session by remember { mutableStateOf<GameSession?>(null) }

    SoundEffectsManager.isSoundEnabled = snapshot.soundEnabled

    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    fun refresh() {
        snapshot = storage.loadSnapshot()
    }

    /** Otomatik modda tableti yatay, geniş tahtaya çevirir. Telefon 8 sütundan başlar. */
    fun effectiveColumns(preferred: Int): Int {
        if (preferred > 0) return preferred
        return when {
            screenWidthDp >= 1000 -> 20
            screenWidthDp >= 840 -> 16
            screenWidthDp >= 700 -> 12
            else -> 8
        }
    }

    fun startLevel(levelId: String, resume: Boolean) {
        val base = CurriculumRepository.levelById(levelId) ?: return
        val progress = storage.loadSnapshot()
        val spec = CurriculumRepository.withColumns(
            base,
            effectiveColumns(progress.boardColumns)
        )
        val newSession = GameSession(
            spec = spec,
            bridge = StorageBridge(storage),
            timedMode = !progress.relaxedUntimedMode,
            extraTimeSeconds = progress.customTimeAdditionSeconds
        )
        if (resume && !spec.endless) {
            val saved = storage.loadGameSnapshot(spec.levelId)
            if (saved != null && !newSession.restoreSnapshot(saved)) {
                // Tahta genişliği değişmiş ya da kayıt bozulmuş: temiz bir tahta başlat.
                storage.clearGameSnapshot(spec.levelId)
            }
        } else if (!spec.endless) {
            storage.clearGameSnapshot(spec.levelId)
        }
        if (!spec.endless) storage.saveLastActiveLevel(spec.levelId)
        session = newSession
        route = Route.Game(spec.levelId)
    }

    /** Android geri tuşu: oyun içindeyse kaydedip haritaya, harita/paneldaysa girişe döner. */
    BackHandler(enabled = route !is Route.Start) {
        val current = route
        if (current is Route.Game) {
            val active = session
            val spec = CurriculumRepository.levelById(current.levelId)
            if (active != null && spec != null) {
                if (spec.endless) {
                    active.finishEndless()
                } else if (!active.isFinished && !active.isTimeUp) {
                    storage.saveGameSnapshot(spec.levelId, active.encodeSnapshot())
                }
            }
            route = Route.Map
        } else {
            route = Route.Start
        }
        refresh()
    }

    when (val current = route) {
        Route.Start -> StartScreen(
            dailyScore = snapshot.dailyScore,
            completedCount = ProgressRules.completedLevelCount(snapshot.completedLevelIds),
            totalLevels = CurriculumRepository.standardLevels.size,
            endlessBestScore = snapshot.endlessBestScore,
            resumeLevelId = snapshot.lastActiveLevelId?.takeIf { id ->
                storage.loadGameSnapshot(id) != null
            },
            onPlay = {
                refresh()
                route = Route.Map
            },
            onResume = {
                val resumable = snapshot.lastActiveLevelId?.takeIf { storage.loadGameSnapshot(it) != null }
                    ?: ProgressRules.currentLevelId(snapshot.completedLevelIds)
                startLevel(resumable, resume = true)
            },
            onParentPanel = {
                refresh()
                route = Route.Parent
            }
        )

        Route.Map -> AdventureMapScreen(
            completedLevelIds = snapshot.completedLevelIds,
            boardColumns = effectiveColumns(snapshot.boardColumns),
            endlessBestScore = snapshot.endlessBestScore,
            onLevelClick = { spec -> startLevel(spec.levelId, resume = true) },
            onBack = {
                refresh()
                route = Route.Start
            }
        )

        is Route.Game -> {
            val active = session
            val spec = CurriculumRepository.levelById(current.levelId)
            if (active != null && spec != null) {
                GameScreen(
                    spec = active.spec,
                    session = active,
                    dailyScore = storage.dailyNetScore(),
                    endlessBestScore = snapshot.endlessBestScore,
                    onBack = {
                        if (active.isEndless) {
                            active.finishEndless()
                        } else if (!active.isFinished) {
                            storage.saveGameSnapshot(spec.levelId, active.encodeSnapshot())
                        }
                        storage.addPlayTimeSeconds(0)
                        refresh()
                        route = Route.Map
                    },
                    onNextLevel = {
                        val nextId = CurriculumRepository.nextLevelId(spec.levelId)
                        if (nextId == null) {
                            refresh()
                            route = Route.Map
                        } else {
                            startLevel(nextId, resume = false)
                        }
                    },
                    onReplay = { startLevel(active.spec.levelId, resume = false) },
                    onFinishEndless = {
                        active.finishEndless()
                        refresh()
                    },
                    onPlaySecond = { storage.addPlayTimeSeconds(1) }
                )
                // Oyunda kaldığımız sürece puan/ilerleme ekranı canlı kalsın.
                LaunchedEffect(active) {
                    snapshotFlow { active.levelScore to active.movesMade }.collect { refresh() }
                }
            }
        }

        Route.Parent -> ParentPanelScreen(
            snapshot = snapshot,
            onToggleSound = { enabled ->
                storage.setSoundEnabled(enabled)
                SoundEffectsManager.isSoundEnabled = enabled
                refresh()
            },
            onToggleTimedMode = { enabled ->
                storage.setRelaxedUntimedMode(!enabled)
                refresh()
            },
            onTimeExtension = { extra ->
                storage.setCustomTimeAdditionSeconds(extra)
                refresh()
            },
            onBoardColumns = { columns ->
                storage.setBoardColumns(columns)
                refresh()
            },
            onResetDailyScore = {
                storage.resetDailyScoreOnly()
                refresh()
            },
            onResetAllProgress = {
                storage.resetAllProgress()
                session = null
                refresh()
                route = Route.Start
            },
            onBack = {
                refresh()
                route = Route.Start
            }
        )
    }
}

/** Oyun oturumunu cihaz içi depoya bağlayan köprü. */
private class StorageBridge(private val storage: LocalProgressStorage) : GameSession.ProgressBridge {

    override fun grossEarnedToday(): Int = storage.grossEarnedToday()

    override fun addGrossEarned(delta: Int) = storage.addGrossEarned(delta)

    override fun addJokerSpending(amount: Int) = storage.addJokerSpending(amount)

    override fun recordAttempt(
        operation: OperationType,
        isCorrect: Boolean,
        operands: List<Int>,
        prompt: String,
        responseTimeMs: Long
    ) = storage.recordOperationAttempt(operation, isCorrect, operands, prompt, responseTimeMs)

    override fun addRewards(goldDelta: Int, diamondDelta: Int) = storage.addRewards(goldDelta, diamondDelta)

    override fun markLevelCompleted(levelId: String) = storage.markLevelCompleted(levelId)

    override fun saveSnapshot(encoded: String) {
        val levelId = encodedLevelId(encoded) ?: return
        storage.saveGameSnapshot(levelId, encoded)
    }

    override fun clearSnapshot() {
        storage.loadSnapshot().lastActiveLevelId?.let { storage.clearGameSnapshot(it) }
    }

    override fun recordEndlessResult(score: Int, missions: Int, bestStreak: Int) {
        storage.recordEndlessResult(score, missions, bestStreak)
    }

    private fun encodedLevelId(encoded: String): String? = encoded.split("|").getOrNull(1)
}
