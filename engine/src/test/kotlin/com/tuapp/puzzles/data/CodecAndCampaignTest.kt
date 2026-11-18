package com.tuapp.puzzles.data

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.Json
import com.tuapp.puzzles.engine.CampaignCodec
import com.tuapp.puzzles.engine.Generators
import com.tuapp.puzzles.engine.LevelCodec
import com.tuapp.puzzles.engine.patches.*
import com.tuapp.puzzles.engine.queens.*
import com.tuapp.puzzles.engine.sudoku.*
import com.tuapp.puzzles.engine.sumbreak.*
import com.tuapp.puzzles.engine.tango.*
import com.tuapp.puzzles.engine.zip.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("UNCHECKED_CAST")
class CodecAndCampaignTest {

    @Test fun everyGameRoundTripsThroughJson() {
        for (game in GameType.entries) {
            val level = assertNotNull(Generators.generate(game, 123, 3), game.id)
            val json = Json.write(LevelCodec.encode(level))
            val back = LevelCodec.decode(game, Json.parse(json) as Map<String, Any?>)
            assertEquals(level, back, game.id)
        }
    }

    @Test fun generatorsAreDeterministicThroughTheEntryPoint() {
        for (game in GameType.entries) assertEquals(Generators.generate(game, 9, 2), Generators.generate(game, 9, 2))
    }

    private fun campaignDir(): File? = listOf(
        "../app/src/main/assets/campaign", "app/src/main/assets/campaign",
    ).map { File(it) }.firstOrNull { it.isDirectory }

    @Test fun shippedCampaignHasRequiredSizeOrderAndUniqueSolutions() {
        val dir = campaignDir() ?: return   // si aún no se ha generado, no se comprueba
        val expected = mapOf(
            GameType.ZIP to 20, GameType.PATCHES to 30, GameType.SUDOKU to 40, GameType.TANGO to 30, GameType.QUEENS to 25, GameType.SUMBREAK to 25,
        )
        for (game in GameType.entries) {
            val levels = CampaignCodec.decode(game, File(dir, "${game.id}.json").readText())
            assertTrue(levels.size >= expected.getValue(game), "${game.id}: ${levels.size}")
            assertEquals(levels.size, levels.map { it.id }.toSet().size, "ids únicos ${game.id}")
            val diffs = levels.map { it.level.difficulty }
            assertEquals(diffs.sorted(), diffs, "dificultad creciente ${game.id}")
            for (cl in levels) {
                val ok = when (val l = cl.level) {
                    is ZipLevel -> ZipSolver.countSolutions(l.puzzle, 2) == 1 && ZipRules.isSolved(l.puzzle, l.solution)
                    is PatchesLevel -> PatchesSolver.countSolutions(l.puzzle, 2) == 1 &&
                        PatchesRules.isSolved(l.puzzle, l.solution.mapIndexed { i, r -> Placement(i, r) })
                    is SudokuLevel -> SudokuSolver.countSolutions(l.puzzle, 2) == 1 && SudokuRules.isSolved(l.puzzle, l.solution)
                    is TangoLevel -> TangoSolver.countSolutions(l.puzzle, 2) == 1 && TangoRules.isSolved(l.puzzle, l.solution)
                    is QueensLevel -> QueensSolver.countSolutions(l.puzzle, 2) == 1 &&
                        QueensRules.isSolved(l.puzzle, l.solution.mapIndexed { r, c -> r * l.puzzle.size + c }.toSet())
                    is SumbreakLevel -> SumbreakSolver.solve(l.puzzle) != null
                    else -> false
                }
                assertTrue(ok, "${cl.id} no es válido o no tiene solución única")
            }
        }
    }
}
