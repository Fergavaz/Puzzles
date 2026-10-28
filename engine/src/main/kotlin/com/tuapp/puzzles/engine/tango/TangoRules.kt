package com.tuapp.puzzles.engine.tango

/** Validador de reglas. Funciona con tableros parciales (EMPTY = sin rellenar). */
object TangoRules {

    /**
     * Comprueba que la celda [i] no viola ninguna regla respecto a las celdas ya asignadas:
     * máximo size/2 símbolos iguales por fila/columna, no tres seguidos y relaciones =/x.
     */
    fun consistentAt(p: TangoPuzzle, g: IntArray, i: Int): Boolean {
        val v = g[i]
        if (v == EMPTY) return true
        val n = p.size
        val r = i / n
        val c = i % n
        var rowCount = 0
        var colCount = 0
        for (k in 0 until n) {
            if (g[r * n + k] == v) rowCount++
            if (g[k * n + c] == v) colCount++
        }
        if (rowCount > n / 2 || colCount > n / 2) return false
        // Ventanas horizontales de 3 que contienen la celda
        for (s in maxOf(0, c - 2)..minOf(c, n - 3)) {
            if (g[r * n + s] == v && g[r * n + s + 1] == v && g[r * n + s + 2] == v) return false
        }
        // Ventanas verticales de 3 que contienen la celda
        for (s in maxOf(0, r - 2)..minOf(r, n - 3)) {
            if (g[s * n + c] == v && g[(s + 1) * n + c] == v && g[(s + 2) * n + c] == v) return false
        }
        p.byCell[i]?.forEach { k ->
            val other = if (k.a == i) k.b else k.a
            val ov = g[other]
            if (ov != EMPTY && ((ov == v) != k.equal)) return false
        }
        return true
    }

    /** Celdas en conflicto (para pintarlas en rojo en la UI). */
    fun conflicts(p: TangoPuzzle, cells: List<Int>): Set<Int> {
        val g = cells.toIntArray()
        return g.indices.filter { !consistentAt(p, g, it) }.toSet()
    }

    fun isSolved(p: TangoPuzzle, cells: List<Int>): Boolean {
        if (cells.size != p.size * p.size) return false
        val g = cells.toIntArray()
        for (i in g.indices) {
            if (g[i] == EMPTY) return false
            if (p.givens[i] != EMPTY && p.givens[i] != g[i]) return false
            if (!consistentAt(p, g, i)) return false
        }
        return true
    }
}
