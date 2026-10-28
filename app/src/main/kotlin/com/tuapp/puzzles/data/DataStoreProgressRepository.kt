package com.tuapp.puzzles.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.Json
import com.tuapp.puzzles.core.Streak
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Progreso persistido en Jetpack DataStore (Preferences). Todo local, sin red. */
@Singleton
class DataStoreProgressRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: DayClock,
) : ProgressRepository {

    private class Keys(game: GameType) {
        val campaignDone = stringSetPreferencesKey("${game.id}_campaign_done")
        val generatedDone = stringSetPreferencesKey("${game.id}_generated_done") // "seed:dificultad"
        val bestTimes = stringPreferencesKey("${game.id}_best_times")           // JSON {clave: ms}
        val streak = intPreferencesKey("${game.id}_streak_days")
        val lastDay = longPreferencesKey("${game.id}_last_solve_day")
    }

    override val progress: Flow<Map<GameType, GameProgress>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val today = clock.epochDay()
            GameType.entries.associateWith { read(prefs, it, today) }
        }

    private fun read(prefs: Preferences, game: GameType, today: Long): GameProgress {
        val k = Keys(game)
        val times = decodeTimes(prefs[k.bestTimes])
        return GameProgress(
            game = game,
            campaignCompletedIds = prefs[k.campaignDone].orEmpty(),
            generatedCompleted = prefs[k.generatedDone].orEmpty().size,
            bestTimeMs = times.values.minOrNull(),
            bestTimesById = times,
            streakDays = Streak.current(prefs[k.lastDay], prefs[k.streak] ?: 0, today),
        )
    }

    override suspend fun recordCompletion(game: GameType, level: LevelRef, timeMs: Long) {
        val today = clock.epochDay()
        dataStore.edit { prefs ->
            val k = Keys(game)
            when (level) {
                is LevelRef.Campaign -> prefs[k.campaignDone] = prefs[k.campaignDone].orEmpty() + level.id
                is LevelRef.Generated ->
                    prefs[k.generatedDone] = prefs[k.generatedDone].orEmpty() + "${level.seed}:${level.difficulty}"
            }
            val times = decodeTimes(prefs[k.bestTimes]).toMutableMap()
            val previous = times[level.key]
            if (previous == null || timeMs < previous) times[level.key] = timeMs
            prefs[k.bestTimes] = Json.write(times)
            prefs[k.streak] = Streak.afterSolve(prefs[k.lastDay], prefs[k.streak] ?: 0, today)
            prefs[k.lastDay] = today
        }
    }

    override suspend fun generatedHistory(game: GameType): List<LevelRef.Generated> =
        dataStore.data.first()[Keys(game).generatedDone].orEmpty().mapNotNull { entry ->
            val parts = entry.split(":")
            val seed = parts.getOrNull(0)?.toLongOrNull()
            val difficulty = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && seed != null && difficulty != null) LevelRef.Generated(seed, difficulty) else null
        }

    private fun decodeTimes(json: String?): Map<String, Long> {
        if (json.isNullOrEmpty()) return emptyMap()
        return try {
            (Json.parse(json) as Map<*, *>).entries.associate { (key, value) -> key.toString() to (value as Number).toLong() }
        } catch (e: Exception) {
            emptyMap() // dato corrupto: se ignora en vez de bloquear la app
        }
    }
}
