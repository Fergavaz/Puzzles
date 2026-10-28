package com.tuapp.puzzles.engine.sudoku

import com.tuapp.puzzles.core.Difficulty
import kotlin.random.Random

/**
 * Generador determinista.
 * 1. Solución completa por backtracking aleatorio (dígitos barajados con la semilla).
 * 2. Se retiran pistas en orden aleatorio; cada retirada solo se acepta si el solver sigue
 *    encontrando exactamente 1 solución (unicidad por construcción).
 * 3. Dificultad 1-3: además el puzzle debe seguir resolviéndose solo con "singles" (sin adivinar).
 *    Se retira hasta llegar al máximo de pistas del nivel (5 = mínimo irreducible, ~12 o menos).
 */
class SudokuGenerator {

    fun generate(seed: Long, difficulty: Int): SudokuLevel {
        val d = Difficulty.clamp(difficulty)
        val rnd = Random(seed)
        val solution = randomSolution(rnd)
        val givens = solution.copyOf()
        var clues = SudokuGrid.CELLS
        val target = TARGET_CLUES[d - 1]
        val order = (0 until SudokuGrid.CELLS).shuffled(rnd)

        for (cell in order) {
            if (clues <= target) break
            val saved = givens[cell]
            givens[cell] = 0
            val puzzle = SudokuPuzzle(givens.toList())
            val ok = SudokuSolver.countSolutions(puzzle, 2) == 1 &&
                (d >= 4 || SudokuSolver.solvableWithSingles(puzzle))
            if (ok) clues-- else givens[cell] = saved
        }
        return SudokuLevel(seed, d, SudokuPuzzle(givens.toList()), solution.toList())
    }

    private fun randomSolution(rnd: Random): IntArray {
        val g = IntArray(SudokuGrid.CELLS)
        fun rec(i: Int): Boolean {
            if (i == g.size) return true
            val m = SudokuGrid.candidates(g, i)
            for (v in (1..6).shuffled(rnd)) {
                if (((m shr v) and 1) == 0) continue
                g[i] = v
                if (rec(i + 1)) return true
            }
            g[i] = 0
            return false
        }
        check(rec(0)) { "No se pudo generar una solución de Mini Sudoku" }
        return g
    }

    companion object {
        /** Pistas máximas para dificultad 1..5. */
        private val TARGET_CLUES = intArrayOf(22, 18, 15, 13, 0)
    }
}
