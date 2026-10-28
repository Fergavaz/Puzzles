package com.tuapp.puzzles.data

import com.tuapp.puzzles.core.GameType
import kotlinx.coroutines.flow.Flow

/** Identifica un nivel jugado: de campaña (por id) o generado (reproducible por semilla + dificultad). */
sealed interface LevelRef {
    val key: String

    data class Campaign(val id: String) : LevelRef {
        override val key: String get() = id
    }

    data class Generated(val seed: Long, val difficulty: Int) : LevelRef {
        override val key: String get() = "g-$seed-$difficulty"
    }
}

data class GameProgress(
    val game: GameType,
    val campaignCompletedIds: Set<String> = emptySet(),
    val generatedCompleted: Int = 0,
    /** Mejor tiempo (ms) entre todos los niveles del juego. */
    val bestTimeMs: Long? = null,
    /** Mejor tiempo por nivel (clave = [LevelRef.key]). */
    val bestTimesById: Map<String, Long> = emptyMap(),
    /** Días consecutivos con algún puzzle resuelto (0 si se ha perdido la racha). */
    val streakDays: Int = 0,
) {
    val solvedTotal: Int get() = campaignCompletedIds.size + generatedCompleted
}

/** Fuente de "hoy" inyectable para poder probar la racha. */
fun interface DayClock {
    fun epochDay(): Long
}

interface ProgressRepository {
    /** Progreso de los cinco juegos; emite de nuevo cada vez que cambia. */
    val progress: Flow<Map<GameType, GameProgress>>

    /** Registra un puzzle resuelto: nivel completado, mejor tiempo y racha. */
    suspend fun recordCompletion(game: GameType, level: LevelRef, timeMs: Long)

    /** Niveles generados que el usuario ha completado (se pueden volver a generar con su semilla). */
    suspend fun generatedHistory(game: GameType): List<LevelRef.Generated>
}
