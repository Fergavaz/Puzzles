package com.tuapp.puzzles.engine.queens

import kotlin.math.abs

/**
 * Backtracking fila a fila: una reina por fila, con máscaras de bits para columnas y regiones
 * usadas, y la regla de no-contacto comprobada solo con la fila anterior.
 */
object QueensSolver {

    fun countSolutions(p: QueensPuzzle, limit: Int = 2): Int = run(p, limit).first

    /** Devuelve la columna de cada fila, o null si no hay solución. */
    fun solve(p: QueensPuzzle): List<Int>? = run(p, 1).second

    private fun run(p: QueensPuzzle, limit: Int): Pair<Int, List<Int>?> {
        val n = p.size
        val cols = IntArray(n) { -1 }
        var count = 0
        var first: List<Int>? = null

        fun rec(r: Int, colMask: Int, regMask: Int) {
            if (count >= limit) return
            if (r == n) {
                count++
                if (first == null) first = cols.toList()
                return
            }
            for (c in 0 until n) {
                if (((colMask shr c) and 1) == 1) continue
                val reg = p.regions[r * n + c]
                if (((regMask shr reg) and 1) == 1) continue
                if (r > 0 && abs(cols[r - 1] - c) <= 1) continue
                cols[r] = c
                rec(r + 1, colMask or (1 shl c), regMask or (1 shl reg))
            }
        }
        rec(0, 0, 0)
        return count to first
    }
}
