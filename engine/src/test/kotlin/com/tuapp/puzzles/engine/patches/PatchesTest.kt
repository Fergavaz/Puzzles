package com.tuapp.puzzles.engine.patches

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PatchesTest {
    private val gen = PatchesGenerator()

    private fun level(seed: Long, d: Int) = assertNotNull(gen.generate(seed, d), "seed $seed dif $d")

    private fun placements(l: PatchesLevel) = l.solution.mapIndexed { i, r -> Placement(i, r) }

    @Test fun generatedSolutionIsValid() {
        for (d in 1..5) {
            val l = level(40L + d, d)
            assertTrue(PatchesRules.isSolved(l.puzzle, placements(l)), "dificultad $d")
        }
    }

    @Test fun solverFindsTheSolution() {
        val l = level(5, 3)
        assertEquals(l.solution, PatchesSolver.solve(l.puzzle))
    }

    @Test fun generatedPuzzlesHaveUniqueSolution() {
        for (d in 1..5) for (seed in 1L..3L) {
            assertEquals(1, PatchesSolver.countSolutions(level(seed, d).puzzle, 5), "seed $seed dif $d")
        }
    }

    @Test fun puzzleWithMultipleSolutionsIsRejected() {
        // 2x2 con dos pistas libres en esquinas opuestas: partición por filas o por columnas
        val p = PatchesPuzzle(2, listOf(PatchClue(0, PatchType.FREE, null), PatchClue(3, PatchType.FREE, null)))
        assertTrue(PatchesSolver.countSolutions(p, 2) >= 2)
    }

    @Test fun sameSeedProducesSamePuzzle() {
        assertEquals(gen.generate(21, 4), gen.generate(21, 4))
    }

    @Test fun shapeRulesAreEnforced() {
        fun p(type: PatchType) = PatchesPuzzle(6, listOf(PatchClue(0, type, null)))
        assertEquals(RectCheck.OK, PatchesRules.check(p(PatchType.SQUARE), 0, Rect(0, 0, 2, 2)))
        assertEquals(RectCheck.WRONG_SHAPE, PatchesRules.check(p(PatchType.SQUARE), 0, Rect(0, 0, 2, 3)))
        assertEquals(RectCheck.WRONG_SHAPE, PatchesRules.check(p(PatchType.WIDE), 0, Rect(0, 0, 2, 2)))
        assertEquals(RectCheck.OK, PatchesRules.check(p(PatchType.WIDE), 0, Rect(0, 0, 2, 4)))
        assertEquals(RectCheck.WRONG_SHAPE, PatchesRules.check(p(PatchType.TALL), 0, Rect(0, 0, 1, 3)))
        assertEquals(RectCheck.OK, PatchesRules.check(p(PatchType.FREE), 0, Rect(0, 0, 2, 2)))
    }

    @Test fun areaAndClueContainmentAreChecked() {
        val p = PatchesPuzzle(6, listOf(PatchClue(0, PatchType.FREE, 4), PatchClue(2, PatchType.FREE, null)))
        assertEquals(RectCheck.WRONG_AREA, PatchesRules.check(p, 0, Rect(0, 0, 1, 2)))
        assertEquals(RectCheck.CONTAINS_OTHER_CLUE, PatchesRules.check(p, 0, Rect(0, 0, 1, 4)))
        assertEquals(RectCheck.NOT_CONTAINING_CLUE, PatchesRules.check(p, 1, Rect(1, 0, 2, 2)))
        assertEquals(RectCheck.OUT_OF_BOUNDS, PatchesRules.check(p, 0, Rect(0, 0, 1, 7)))
    }

    @Test fun easyLevelsShowShapeAndNumberAndHardOnesFreeClues() {
        val easy = level(3, 1).puzzle.clues
        assertTrue(easy.all { it.area != null && it.type != PatchType.FREE })
        val hard = level(3, 5).puzzle.clues
        assertTrue(hard.count { it.type == PatchType.FREE } > hard.size / 2)
    }

    @Test fun hintCompletesFromPlacedPatches() {
        val l = level(9, 2)
        val partial = placements(l).take(2)
        assertEquals(l.solution, PatchesSolver.solve(l.puzzle, partial))
    }
}
