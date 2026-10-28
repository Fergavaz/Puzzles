package com.tuapp.puzzles.engine.session

import com.tuapp.puzzles.engine.sudoku.*

/** Selección de celda + paleta 1..6 (0 = borrar). Las pistas iniciales no se pueden modificar. */
class SudokuSession(val level: SudokuLevel) : Snapshotted<List<Int>>(level.puzzle.givens) {
    private val givens = level.puzzle.givens

    var selected: Int? = null
        private set

    fun isGiven(i: Int) = givens[i] != 0

    fun select(i: Int?) { selected = i }

    /** Escribe [value] (1..6, o 0 para borrar) en la celda seleccionada. */
    fun enter(value: Int): Boolean {
        val i = selected ?: return false
        if (isGiven(i) || value !in 0..6) return false
        change(state.toMutableList().also { it[i] = value })
        return true
    }

    fun clear() = change(givens)

    val conflicts: Set<Int> get() = SudokuRules.conflicts(state)
    val isSolved: Boolean get() = SudokuRules.isSolved(level.puzzle, state)

    /**
     * Pista: primero corrige un valor erróneo; si no hay, usa una deducción por "singles" del
     * solver; si tampoco, rellena la primera celda vacía con la solución. Devuelve (celda, valor).
     */
    fun hint(): Pair<Int, Int>? {
        val wrong = state.indices.firstOrNull { !isGiven(it) && state[it] != 0 && state[it] != level.solution[it] }
        val move = when {
            wrong != null -> wrong to level.solution[wrong]
            else -> SudokuSolver.nextSingle(state)
                ?: state.indices.firstOrNull { state[it] == 0 }?.let { it to level.solution[it] }
        } ?: return null
        change(state.toMutableList().also { it[move.first] = move.second })
        selected = move.first
        return move
    }
}
