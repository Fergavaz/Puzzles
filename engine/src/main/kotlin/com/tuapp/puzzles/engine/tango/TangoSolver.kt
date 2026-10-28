package com.tuapp.puzzles.engine.tango

/**
 * Solver por backtracking. Rellena celda a celda; la poda viene de [TangoRules.consistentAt],
 * que ya rechaza filas/columnas con demasiados símbolos, triples y relaciones violadas.
 * Si cada fila y columna tiene <= size/2 de cada símbolo y el tablero está lleno, el equilibrio es exacto.
 */
object TangoSolver {

    /** Cuenta soluciones hasta [limit] (basta con 2 para comprobar unicidad). */
    fun countSolutions(p: TangoPuzzle, limit: Int = 2): Int = Search(p, limit).run().first

    fun solve(p: TangoPuzzle): List<Int>? = Search(p, 1).run().second

    private class Search(val p: TangoPuzzle, val limit: Int) {
        val g = p.givens.toIntArray()
        var count = 0
        var first: List<Int>? = null

        fun run(): Pair<Int, List<Int>?> {
            // Las celdas dadas deben ser coherentes entre sí
            for (i in g.indices) if (!TangoRules.consistentAt(p, g, i)) return 0 to null
            rec(0)
            return count to first
        }

        fun rec(i: Int) {
            if (count >= limit) return
            if (i == g.size) {
                count++
                if (first == null) first = g.toList()
                return
            }
            if (p.givens[i] != EMPTY) { rec(i + 1); return }
            for (v in 0..1) {
                g[i] = v
                if (TangoRules.consistentAt(p, g, i)) rec(i + 1)
            }
            g[i] = EMPTY
        }
    }
}
