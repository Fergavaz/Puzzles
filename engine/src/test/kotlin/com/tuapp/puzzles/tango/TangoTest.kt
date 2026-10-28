package com.tuapp.puzzles.engine.tango

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TangoTest {
    private val gen = TangoGenerator()
    private val blank = TangoPuzzle(6, List(36) { EMPTY }, emptyList())

    @Test fun generatedSolutionIsValid() {
        for (d in 1..5) {
            val l = gen.generate(100L + d, d)
            assertTrue(TangoRules.isSolved(l.puzzle, l.solution), "dificultad $d")
        }
    }

    @Test fun solverFindsTheSolution() {
        val l = gen.generate(7, 3)
        val s = TangoSolver.solve(l.puzzle)
        assertNotNull(s)
        assertEquals(l.solution, s)
    }

    @Test fun generatedPuzzlesHaveUniqueSolution() {
        for (d in 1..5) for (seed in 1L..3L) {
            assertEquals(1, TangoSolver.countSolutions(gen.generate(seed, d).puzzle, 5), "seed $seed dif $d")
        }
    }

    @Test fun puzzleWithMultipleSolutionsIsRejected() {
        assertTrue(TangoSolver.countSolutions(blank, 2) >= 2)
    }

    @Test fun sameSeedProducesSamePuzzle() {
        assertEquals(gen.generate(42, 4), gen.generate(42, 4))
    }

    @Test fun tripleInARowIsAConflict() {
        val cells = MutableList(36) { EMPTY }
        cells[0] = SUN; cells[1] = SUN; cells[2] = SUN
        assertTrue(TangoRules.conflicts(blank, cells).isNotEmpty())
        assertFalse(TangoRules.isSolved(blank, cells))
    }

    @Test fun relationViolationIsAConflict() {
        val p = TangoPuzzle(6, List(36) { EMPTY }, listOf(TangoConstraint(0, 1, equal = false)))
        val cells = MutableList(36) { EMPTY }
        cells[0] = SUN; cells[1] = SUN
        assertTrue(0 in TangoRules.conflicts(p, cells))
    }

    @Test fun hardestLevelHasFewerCluesThanEasiest() {
        fun clues(l: TangoLevel) = l.puzzle.givens.count { it != EMPTY } + l.puzzle.constraints.size
        assertTrue(clues(gen.generate(5, 5)) < clues(gen.generate(5, 1)))
    }
}
