package com.tuapp.puzzles.engine.sudoku

/**
 * Solver por backtracking con la heurística "celda con menos candidatos" (MRV).
 * Además ofrece deducciones por "singles" (naked/hidden) para pistas y para clasificar dificultad.
 */
object SudokuSolver {

    fun countSolutions(p: SudokuPuzzle, limit: Int = 2): Int = Search(p, limit).run().first

    fun solve(p: SudokuPuzzle): List<Int>? = Search(p, 1).run().second

    /**
     * Siguiente deducción por singles sobre el estado actual: (celda, valor) o null.
     * Naked single: la celda solo admite un valor. Hidden single: en una unidad, un valor solo cabe en una celda.
     */
    fun nextSingle(cells: List<Int>): Pair<Int, Int>? {
        val g = cells.toIntArray()
        for (i in g.indices) {
            if (g[i] != 0) continue
            val m = SudokuGrid.candidates(g, i)
            if (Integer.bitCount(m) == 1) return i to Integer.numberOfTrailingZeros(m)
        }
        for (unit in SudokuGrid.units) {
            for (v in 1..6) {
                if (unit.any { g[it] == v }) continue
                val spots = unit.filter { g[it] == 0 && (SudokuGrid.candidates(g, it) shr v) and 1 == 1 }
                if (spots.size == 1) return spots[0] to v
            }
        }
        return null
    }

    /** true si el puzzle se resuelve solo con singles (sin adivinar). */
    fun solvableWithSingles(p: SudokuPuzzle): Boolean {
        val g = p.givens.toMutableList()
        while (g.any { it == 0 }) {
            val (cell, v) = nextSingle(g) ?: return false
            g[cell] = v
        }
        return SudokuRules.conflicts(g).isEmpty()
    }

    private class Search(val p: SudokuPuzzle, val limit: Int) {
        val g = p.givens.toIntArray()
        var count = 0
        var first: List<Int>? = null

        fun run(): Pair<Int, List<Int>?> {
            if (SudokuRules.conflicts(p.givens).isNotEmpty()) return 0 to null
            rec()
            return count to first
        }

        fun rec() {
            if (count >= limit) return
            var best = -1
            var bestMask = 0
            var bestN = 7
            for (i in g.indices) {
                if (g[i] != 0) continue
                val m = SudokuGrid.candidates(g, i)
                val c = Integer.bitCount(m)
                if (c < bestN) { best = i; bestMask = m; bestN = c; if (c == 0) return }
            }
            if (best == -1) {
                count++
                if (first == null) first = g.toList()
                return
            }
            for (v in 1..6) {
                if (((bestMask shr v) and 1) == 1) { g[best] = v; rec() }
            }
            g[best] = 0
        }
    }
}
