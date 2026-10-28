package com.tuapp.puzzles.tools

import com.tuapp.puzzles.core.CampaignLevel
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.engine.CampaignCodec
import com.tuapp.puzzles.engine.Generators
import com.tuapp.puzzles.engine.queens.QueensGenerator
import java.io.File

/**
 * Genera los ficheros de campaña (assets/campaign/<juego>.json) con los propios generadores del motor,
 * así que todos los niveles tienen solución única garantizada. Uso: ./gradlew :engine:generateCampaign
 */
private val COUNTS = mapOf(
    GameType.ZIP to 20, GameType.PATCHES to 30, GameType.SUDOKU to 40, GameType.TANGO to 30, GameType.QUEENS to 25,
)

/** Queens: de 5x5 a 10x10 (25 niveles). */
private val QUEENS_SIZES = List(3) { 5 } + List(4) { 6 } + List(5) { 7 } + List(5) { 8 } + List(4) { 9 } + List(4) { 10 }

fun main(args: Array<String>) {
    val dir = File(args.firstOrNull() ?: "app/src/main/assets/campaign")
    dir.mkdirs()
    for (game in GameType.entries) {
        val levels = buildCampaign(game)
        File(dir, "${game.id}.json").writeText(CampaignCodec.encode(game, levels))
        println("${game.id}: ${levels.size} niveles")
    }
}

fun buildCampaign(game: GameType): List<CampaignLevel> {
    val count = COUNTS.getValue(game)
    return List(count) { i ->
        // Dificultad creciente repartida de forma uniforme
        val difficulty = 1 + i * 5 / count
        var seed = game.ordinal * 100_000L + i * 31L + 1
        var level = generateOne(game, i, seed, difficulty)
        var tries = 0
        while (level == null && tries++ < 100) { seed++; level = generateOne(game, i, seed, difficulty) }
        CampaignLevel("${game.id}-%02d".format(i + 1), i, checkNotNull(level) { "No se pudo generar ${game.id} #$i" })
    }
}

private fun generateOne(game: GameType, index: Int, seed: Long, difficulty: Int) =
    if (game == GameType.QUEENS) QueensGenerator().generateSized(seed, QUEENS_SIZES[index], difficulty)
    else Generators.generate(game, seed, difficulty)
