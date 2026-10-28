package com.tuapp.puzzles.engine.session

import com.tuapp.puzzles.engine.zip.*

/**
 * Trazado por arrastre. La UI llama a [beginStroke] al tocar, [drag] por cada celda que atraviesa
 * el dedo y [endStroke] al soltar; Deshacer retrocede un trazo completo.
 */
class ZipSession(val level: ZipLevel) : Snapshotted<List<Int>>(emptyList()) {
    private val p = level.puzzle
    private var strokeStart: List<Int> = emptyList()

    fun beginStroke() { strokeStart = state }

    /**
     * Procesa una celda alcanzada por el dedo. Devuelve true si el camino cambió.
     *  - Camino vacío: solo se acepta la celda del 1.
     *  - Celda ya en el camino: retroceso hasta ella.
     *  - Celda adyacente (sin muro), libre y con el número que toca: se añade.
     */
    fun drag(cell: Int): Boolean {
        val path = state
        if (path.isEmpty()) {
            if (cell != p.startCell) return false
            state = listOf(cell)
            return true
        }
        if (cell == path.last()) return false
        val idx = path.indexOf(cell)
        if (idx >= 0) { state = path.subList(0, idx + 1).toList(); return true }
        if (p.adjacency[path.last()].none { it == cell }) return false
        val next = path + cell
        if (!ZipRules.isValidPrefix(p, next)) return false
        state = next
        return true
    }

    fun endStroke() = recordHistory(strokeStart)

    fun clear() = change(emptyList())

    val isSolved: Boolean get() = ZipRules.isSolved(p, state)

    /** Mantiene el tramo correcto del camino y añade la siguiente celda de la solución única. */
    fun hint(): Int? {
        val sol = level.solution
        val k = state.zip(sol).takeWhile { it.first == it.second }.size
        if (k >= sol.size) return null
        change(sol.take(k + 1))
        return sol[k]
    }
}
