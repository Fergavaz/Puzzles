package com.tuapp.puzzles.engine.queens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QueensTest {
    private val gen = QueensGenerator()

    private fun level(seed: Long, d: Int) = assertNotNull(gen.generate(seed, d))

    private fun cells(l: QueensLevel) = l.solution.mapIndexed { r, c -> r * l.puzzle.size + c }.toSet()

    @Test fun generatedSolutionIsValid() {
        for (d in 1..5) {
            val l = level(10L + d, d)
            assertTrue(QueensRules.isSolved(l.puzzle, cells(l)), "dificultad $d")
        }
    }

    @Test fun solverFindsTheSolution() {
        val l = level(3, 3)
        assertEquals(l.solution, QueensSolver.solve(l.puzzle))
    }

    @Test fun generatedPuzzlesHaveUniqueSolution() {
        for (d in 1..5) for (seed in 1L..3L) {
            assertEquals(1, QueensSolver.countSolutions(level(seed, d).puzzle, 5), "seed $seed dif $d")
        }
    }

    @Test fun puzzleWithMultipleSolutionsIsRejected() {
        // Regiones = columnas: la restricción de región no aporta nada y hay muchas soluciones
        val n = 6
        val p = QueensPuzzle(n, List(n * n) { it % n })
        assertTrue(QueensSolver.countSolutions(p, 2) >= 2)
    }

    @Test fun sameSeedProducesSamePuzzle() {
        assertEquals(gen.generate(99, 4), gen.generate(99, 4))
    }

    @Test fun diagonalContactIsAConflict() {
        val l = level(5, 2)
        val n = l.puzzle.size
        assertEquals(setOf(0, n + 1), QueensRules.conflicts(l.puzzle, setOf(0, n + 1)))
    }

    @Test fun incompleteBoardIsNotSolved() {
        val l = level(5, 2)
        assertFalse(QueensRules.isSolved(l.puzzle, cells(l).drop(1).toSet()))
    }
}
