package com.tuapp.puzzles.engine.session

import com.tuapp.puzzles.engine.patches.*

/** Arrastre desde una pista para dibujar un rectángulo; al soltar se coloca si es válido, si no se descarta. */
class PatchesSession(val level: PatchesLevel) : Snapshotted<List<Placement>>(
    level.solution.indices
        .filter { level.solution[it].area == 1 }
        .map { Placement(it, level.solution[it]) }
) {
    private val p = level.puzzle
    val fixed1x1: Set<Int> = level.solution.indices.filter { level.solution[it].area == 1 }.toSet()

    /** Estado en vivo del rectángulo que se arrastra: nº de celdas (para mostrarlo) y validez (borde rojo si no). */
    data class Preview(val cells: Int, val check: RectCheck, val overlapsOthers: Boolean) {
        val valid: Boolean get() = check == RectCheck.OK && !overlapsOthers
    }

    fun preview(clueIndex: Int, rect: Rect): Preview = Preview(
        rect.area,
        PatchesRules.check(p, clueIndex, rect),
        state.any { it.clueIndex != clueIndex && it.rect.overlaps(rect) },
    )

    /** Pistas cuyo cuadro cae dentro del rectángulo. */
    fun cluesIn(rect: Rect): List<Int> = p.clues.indices.filter { rect.containsCell(p.clues[it].cell, p.size) }

    /**
     * Vista previa de un rectángulo arrastrado entre dos esquinas cualesquiera: la pista es la única
     * que queda dentro (la pista puede estar en el interior, no solo en una esquina).
     */
    fun previewRect(rect: Rect): Preview {
        val inside = cluesIn(rect)
        return when (inside.size) {
            1 -> preview(inside[0], rect)
            0 -> Preview(rect.area, RectCheck.NOT_CONTAINING_CLUE, false)
            else -> Preview(rect.area, RectCheck.CONTAINS_OTHER_CLUE, false)
        }
    }

    fun commitRect(rect: Rect): Boolean {
        val ci = cluesIn(rect).singleOrNull() ?: return false
        return commit(ci, rect)
    }

    /** Coloca el parche (sustituye el anterior de esa pista). false = descartado por inválido. */
    fun commit(clueIndex: Int, rect: Rect): Boolean {
        if (clueIndex in fixed1x1 && rect != level.solution[clueIndex]) return false
        if (!preview(clueIndex, rect).valid) return false
        change(state.filter { it.clueIndex != clueIndex } + Placement(clueIndex, rect))
        return true
    }

    fun remove(clueIndex: Int): Boolean {
        if (clueIndex in fixed1x1) return false
        if (state.none { it.clueIndex == clueIndex }) return false
        change(state.filter { it.clueIndex != clueIndex })
        return true
    }

    fun clear() = change(level.solution.indices.filter { it in fixed1x1 }.map { Placement(it, level.solution[it]) })

    /** Parche colocado que contiene la celda, si lo hay (para tocarlo y quitarlo). */
    fun placementAt(cell: Int): Placement? = state.firstOrNull { it.rect.containsCell(cell, p.size) }

    val isSolved: Boolean get() = PatchesRules.isSolved(p, state)

    /** Coloca el rectángulo correcto de la primera pista mal resuelta, retirando lo que estorbe. */
    fun hint(): Int? {
        val i = p.clues.indices.firstOrNull { ci -> state.none { it.clueIndex == ci && it.rect == level.solution[ci] } }
            ?: return null
        val sol = level.solution[i]
        change(state.filter { it.clueIndex != i && !it.rect.overlaps(sol) } + Placement(i, sol))
        return i
    }
}
