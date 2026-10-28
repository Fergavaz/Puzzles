package com.tuapp.puzzles.engine.zip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZipTest {
    private val gen = ZipGenerator()

    @Test fun generatedSolutionIsValid() {
        for (d in 1..5) {
            val l = gen.generate(30L + d, d)
            assertTrue(ZipRules.isSolved(l.puzzle, l.solution), "dificultad $d")
        }
    }

    @Test fun solverFindsTheSolution() {
        val l = gen.generate(6, 3)
        assertEquals(l.solution, ZipSolver.solve(l.puzzle))
    }

    @Test fun generatedPuzzlesHaveUniqueSolution() {
        for (d in 1..5) for (seed in 1L..3L) {
            assertEquals(1, ZipSolver.countSolutions(gen.generate(seed, d).puzzle, 5), "seed $seed dif $d")
        }
    }

    @Test fun puzzleWithMultipleSolutionsIsRejected() {
        // 4x4 con solo el 1 (esquina) y el 2 (otra esquina, color opuesto): hay varios caminos
        val nums = MutableList(16) { 0 }
        nums[0] = 1; nums[3] = 2
        assertTrue(ZipSolver.countSolutions(ZipPuzzle(4, 4, nums), 2) >= 2)
    }

    @Test fun sameSeedProducesSamePuzzle() {
        assertEquals(gen.generate(55, 5), gen.generate(55, 5))
    }

    @Test fun wallsOnlyAppearFromDifficultyThree() {
        for (seed in 1L..3L) {
            assertTrue(gen.generate(seed, 1).puzzle.walls.isEmpty())
            assertTrue(gen.generate(seed, 2).puzzle.walls.isEmpty())
        }
    }

    @Test fun crossingAWallOrSkippingANumberIsInvalid() {
        val nums = MutableList(16) { 0 }
        nums[0] = 1; nums[15] = 3; nums[1] = 2
        val p = ZipPuzzle(4, 4, nums, listOf(Edge(0, 4)))
        assertFalse(ZipRules.isValidPrefix(p, listOf(0, 4)))            // cruza muro
        assertTrue(ZipRules.isValidPrefix(p, listOf(0, 1)))
        val nums2 = MutableList(16) { 0 }
        nums2[0] = 1; nums2[2] = 2; nums2[15] = 3
        val p2 = ZipPuzzle(4, 4, nums2)
        assertTrue(ZipRules.isValidPrefix(p2, listOf(0, 1, 2, 3, 7, 11, 15)))
        assertFalse(ZipRules.isValidPrefix(p2, listOf(0, 4, 8, 12, 13, 14, 15))) // llega al 3 sin pasar por el 2
    }

    @Test fun hintContinuesFromPartialPath() {
        val l = gen.generate(12, 4)
        val prefix = l.solution.take(l.solution.size / 2)
        assertEquals(l.solution, assertNotNull(ZipSolver.solve(l.puzzle, prefix)))
    }
}
