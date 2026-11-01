package com.tuapp.puzzles.engine.session

import com.tuapp.puzzles.engine.tango.*

/** Toques cíclicos: vacío -> sol -> luna -> vacío. Las celdas dadas no se pueden cambiar. */
class TangoSession(val level: TangoLevel) : Snapshotted<List<Int>>(level.puzzle.givens) {
    private val givens = level.puzzle.givens

    fun isGiven(i: Int) = givens[i] != EMPTY

    fun tap(i: Int): Boolean {
        if (isGiven(i)) return false
        val next = when (state[i]) { EMPTY -> SUN; SUN -> MOON; else -> EMPTY }
        change(state.toMutableList().also { it[i] = next })
        return true
    }

    /** Borra todo lo escrito por el jugador (se puede deshacer). */
    fun clear() = change(givens)

    /** Devuelve la siguiente pista lógica o error según TangoHintEngine. */
    fun currentHint(): TangoHint? = TangoHintEngine.nextHint(level, state)

    /** Aplica la deducción de la pista en un único paso de historial (deshacer). */
    fun applyHint(hint: TangoHint) {
        val next = state.toMutableList()
        when (hint) {
            is TangoHint.Wrong -> {
                for (c in hint.cells) {
                    if (!isGiven(c)) next[c] = EMPTY
                }
            }
            is TangoHint.Place -> {
                next[hint.cell] = hint.symbol
            }
        }
        change(next)
    }

    /** Atajo de depuración: rellena la primera celda que no coincide con la solución; devuelve su índice. */
    fun hint(): Int? {
        val i = state.indices.firstOrNull { state[it] != level.solution[it] } ?: return null
        change(state.toMutableList().also { it[i] = level.solution[i] })
        return i
    }

    val conflicts: Set<Int> get() = TangoRules.conflicts(level.puzzle, state)
    val isSolved: Boolean get() = TangoRules.isSolved(level.puzzle, state)
}
