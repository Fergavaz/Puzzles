package com.tuapp.puzzles.data

import com.tuapp.puzzles.core.Difficulty
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel
import com.tuapp.puzzles.core.defaultSeed
import com.tuapp.puzzles.di.DefaultDispatcher
import com.tuapp.puzzles.engine.Generators
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Resultado de "Generar nuevo". [ref] es la referencia con la que se guardará el progreso;
 * [fromFallback] indica que la generación falló y se entregó un nivel de campaña de respaldo.
 */
data class GeneratedLevel(val level: PuzzleLevel, val ref: LevelRef, val fromFallback: Boolean)

interface LevelGenerator {
    suspend fun generate(game: GameType, difficulty: Int, seed: Long? = null): GeneratedLevel
}

@Singleton
class DefaultLevelGenerator @Inject constructor(
    private val campaign: CampaignRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : LevelGenerator {

    /** Los algoritmos son CPU puro: se ejecutan en Dispatchers.Default para no bloquear el hilo principal. */
    override suspend fun generate(game: GameType, difficulty: Int, seed: Long?): GeneratedLevel =
        withContext(dispatcher) {
            val s = seed ?: defaultSeed()
            val d = Difficulty.clamp(difficulty)
            val level = try {
                Generators.generate(game, s, d)
            } catch (e: Exception) {
                null
            }
            if (level != null) {
                GeneratedLevel(level, LevelRef.Generated(s, d), fromFallback = false)
            } else {
                // Respaldo: nunca se bloquea al usuario; se entrega un nivel precargado de dificultad similar
                val pool = campaign.levels(game)
                val pick = pool.filter { it.level.difficulty == d }.ifEmpty { pool }.random(Random(s))
                GeneratedLevel(pick.level, LevelRef.Campaign(pick.id), fromFallback = true)
            }
        }
}
