package com.tuapp.puzzles.engine.queens

import com.tuapp.puzzles.core.Difficulty
import kotlin.math.abs
import kotlin.random.Random

/**
 * Generador determinista.
 * 1. Solución aleatoria de N reinas (backtracking, sin contacto).
 * 2. Cada reina siembra una región que crece por vecindad (4 direcciones) hasta cubrir el tablero;
 *    así las regiones son conexas y cada una contiene exactamente una reina de la solución.
 * 3. El solver comprueba unicidad. Si hay varias soluciones, un paso de reparación (hill-climbing)
 *    reasigna celdas frontera a regiones vecinas, manteniéndolas conexas, y solo acepta cambios que
 *    no aumentan el número de soluciones. Si no converge, se reintenta con otra solución.
 * Devuelve null si agota los intentos (el llamador usa un nivel precargado de respaldo).
 */
class QueensGenerator {

    fun generate(seed: Long, difficulty: Int, maxAttempts: Int = 200): QueensLevel? {
        val d = Difficulty.clamp(difficulty)
        return generateSized(seed, SIZES[d - 1], d, maxAttempts)
    }

    /** Igual que [generate] pero con el tamaño del tablero explícito (la campaña usa de 5x5 a 10x10). */
    fun generateSized(
        seed: Long,
        size: Int,
        difficulty: Int = Difficulty.clamp(size - 4),
        maxAttempts: Int = 200,
    ): QueensLevel? {
        val d = Difficulty.clamp(difficulty)
        val n = size
        val rnd = Random(seed)
        repeat(maxAttempts) {
            val sol = randomSolution(n, rnd)
            val reg = growRegions(n, sol, rnd).toIntArray()
            if (refine(n, reg, sol, rnd, REPAIR_STEPS)) {
                val puzzle = QueensPuzzle(n, reg.toList())
                check(QueensSolver.countSolutions(puzzle, 2) == 1)
                return QueensLevel(seed, d, puzzle, sol.toList())
            }
        }
        return null
    }

    /** Devuelve true cuando el puzzle queda con exactamente una solución. */
    private fun refine(n: Int, reg: IntArray, sol: IntArray, rnd: Random, steps: Int): Boolean {
        val queenCells = sol.indices.map { it * n + sol[it] }.toSet()
        var score = QueensSolver.countSolutions(QueensPuzzle(n, reg.toList()), CAP)
        var step = 0
        while (score > 1 && step < steps) {
            step++
            val cell = rnd.nextInt(n * n)
            if (cell in queenCells) continue
            val r = cell / n; val c = cell % n
            val options = ArrayList<Int>()
            for ((dr, dc) in DIRS) {
                val rr = r + dr; val cc = c + dc
                if (rr in 0 until n && cc in 0 until n && reg[rr * n + cc] != reg[cell]) options.add(reg[rr * n + cc])
            }
            if (options.isEmpty()) continue
            val old = reg[cell]
            reg[cell] = options[rnd.nextInt(options.size)]
            if (!isConnected(n, reg, old, sol[old] + old * n)) { reg[cell] = old; continue }
            val ns = QueensSolver.countSolutions(QueensPuzzle(n, reg.toList()), CAP)
            if (ns <= score) score = ns else reg[cell] = old
        }
        return score == 1
    }

    /** La región [id] (sembrada en [start]) sigue siendo conexa. */
    private fun isConnected(n: Int, reg: IntArray, id: Int, start: Int): Boolean {
        val seen = BooleanArray(n * n)
        val stack = ArrayDeque<Int>()
        stack.add(start); seen[start] = true
        var reached = 0
        while (stack.isNotEmpty()) {
            val i = stack.removeLast(); reached++
            val r = i / n; val c = i % n
            for ((dr, dc) in DIRS) {
                val rr = r + dr; val cc = c + dc
                if (rr in 0 until n && cc in 0 until n) {
                    val j = rr * n + cc
                    if (!seen[j] && reg[j] == id) { seen[j] = true; stack.add(j) }
                }
            }
        }
        return reached == reg.count { it == id }
    }

    private fun randomSolution(n: Int, rnd: Random): IntArray {
        val cols = IntArray(n) { -1 }
        val used = BooleanArray(n)
        fun rec(r: Int): Boolean {
            if (r == n) return true
            for (c in (0 until n).shuffled(rnd)) {
                if (used[c]) continue
                if (r > 0 && abs(cols[r - 1] - c) <= 1) continue
                cols[r] = c; used[c] = true
                if (rec(r + 1)) return true
                used[c] = false; cols[r] = -1
            }
            return false
        }
        check(rec(0)) { "No hay solución para n=$n" }
        return cols
    }

    private fun growRegions(n: Int, sol: IntArray, rnd: Random): List<Int> {
        val reg = IntArray(n * n) { -1 }
        for (r in 0 until n) reg[r * n + sol[r]] = r
        var left = n * n - n
        val dr = intArrayOf(-1, 1, 0, 0)
        val dc = intArrayOf(0, 0, -1, 1)
        while (left > 0) {
            // Candidatos: (celda libre, región de un vecino ya asignado)
            val cells = ArrayList<Int>()
            val regs = ArrayList<Int>()
            for (i in 0 until n * n) {
                if (reg[i] != -1) continue
                val r = i / n; val c = i % n
                for (k in 0 until 4) {
                    val rr = r + dr[k]; val cc = c + dc[k]
                    if (rr in 0 until n && cc in 0 until n && reg[rr * n + cc] != -1) {
                        cells.add(i); regs.add(reg[rr * n + cc])
                    }
                }
            }
            val pick = rnd.nextInt(cells.size)
            reg[cells[pick]] = regs[pick]
            left--
        }
        return reg.toList()
    }

    companion object {
        /** Tamaño del tablero para dificultad 1..5. */
        private val SIZES = intArrayOf(5, 6, 7, 8, 9)
        private const val REPAIR_STEPS = 400
        private const val CAP = 40
        private val DIRS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}
