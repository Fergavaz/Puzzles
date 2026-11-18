package com.tuapp.puzzles.engine.session

import com.tuapp.puzzles.engine.sumbreak.SumbreakHint
import com.tuapp.puzzles.engine.sumbreak.SumbreakHintEngine
import com.tuapp.puzzles.engine.sumbreak.SumbreakLevel
import com.tuapp.puzzles.engine.sumbreak.SumbreakMove
import com.tuapp.puzzles.engine.sumbreak.SumbreakRules
import com.tuapp.puzzles.engine.sumbreak.SumbreakTile

class SumbreakSession(val level: SumbreakLevel) : Snapshotted<List<SumbreakTile>>(level.puzzle.initialTiles) {
    val size: Int get() = level.puzzle.size

    fun canMove(from: Int, to: Int): Boolean {
        if (!SumbreakRules.isAdjacent(size, from, to)) return false
        return SumbreakRules.mergeResult(state[from], state[to]) != null
    }

    fun makeMove(from: Int, to: Int): Boolean {
        val next = SumbreakRules.applyMove(size, state, SumbreakMove(from, to)) ?: return false
        change(next)
        return true
    }

    fun clear() {
        change(level.puzzle.initialTiles)
    }

    val isSolved: Boolean get() = SumbreakRules.isSolved(state)

    val isDeadEnd: Boolean get() = SumbreakRules.isDeadEnd(size, state)

    val validMoves: List<SumbreakMove> get() = SumbreakRules.validMoves(size, state)

    fun currentHint(): SumbreakHint? = SumbreakHintEngine.nextHint(size, state)

    fun applyHint(hint: SumbreakHint): Boolean = when (hint) {
        is SumbreakHint.SuggestMove -> makeMove(hint.move.from, hint.move.to)
        is SumbreakHint.DeadEnd -> undo()
    }

    /** Atajo de ayuda directa: aplica el siguiente movimiento y retorna la celda destino. */
    fun hint(): Int? {
        val h = currentHint() as? SumbreakHint.SuggestMove ?: return null
        return if (applyHint(h)) h.move.to else null
    }
}
