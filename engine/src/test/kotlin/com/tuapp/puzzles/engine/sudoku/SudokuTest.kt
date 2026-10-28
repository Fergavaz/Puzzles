package com.tuapp.puzzles.engine.sudoku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SudokuTest {
    private val gen = SudokuGenerator()

    @Test fun generatedSolutionIsValid() {
        for (d in 1..5) {
            val l = gen.generate(20L + d, d)
            assertTrue(SudokuRules.isSolved(l.puzzle, l.solution), "dificultad $d")
        }
    }

    @Test fun solverFindsTheSolution() {
        val l = gen.generate(8, 3)
        assertEquals(l.solution, SudokuSolver.solve(l.puzzle))
    }

    @Test fun generatedPuzzlesHaveUniqueSolution() {
        for (d in 1..5) for (seed in 1L..3L) {
            assertEquals(1, SudokuSolver.countSolutions(gen.generate(seed, d).puzzle, 5), "seed $seed dif $d")
        }
    }

    @Test fun puzzleWithMultipleSolutionsIsRejected() {
        val p = SudokuPuzzle(List(36) { 0 })
        assertTrue(SudokuSolver.countSolutions(p, 2) >= 2)
    }

    @Test fun sameSeedProducesSamePuzzle() {
        assertEquals(gen.generate(77, 4), gen.generate(77, 4))
    }

    @Test fun duplicatesAreConflicts() {
        val cells = MutableList(36) { 0 }
        cells[0] = 3; cells[5] = 3   // misma fila
        cells[6] = 3                 // misma región que (0) y misma columna
        assertEquals(setOf(0, 5, 6), SudokuRules.conflicts(cells))
    }

    @Test fun easyLevelsNeedOnlySingles() {
        for (seed in 1L..3L) assertTrue(SudokuSolver.solvableWithSingles(gen.generate(seed, 1).puzzle))
    }

    @Test fun hardestHasFewerCluesThanEasiest() {
        fun clues(l: SudokuLevel) = l.puzzle.givens.count { it != 0 }
        assertTrue(clues(gen.generate(4, 5)) < clues(gen.generate(4, 1)))
        assertTrue(clues(gen.generate(4, 5)) <= 14)
    }

    @Test fun nextSingleReturnsACorrectValue() {
        val l = gen.generate(9, 1)
        val hint = assertNotNull(SudokuSolver.nextSingle(l.puzzle.givens))
        assertEquals(l.solution[hint.first], hint.second)
        assertFalse(l.puzzle.givens[hint.first] != 0)
    }
}
