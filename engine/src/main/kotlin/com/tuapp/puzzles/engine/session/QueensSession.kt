package com.tuapp.puzzles.engine.session

import com.tuapp.puzzles.engine.queens.*

/** Toques cíclicos: vacío -> X -> reina -> vacío. */
class QueensSession(val level: QueensLevel) : Snapshotted<List<Int>>(List(level.puzzle.size * level.puzzle.size) { EMPTY }) {
    private val n = level.puzzle.size

    fun markEmpty(cell: Int) {
        if (state[cell] == EMPTY) {
            change(state.toMutableList().also { it[cell] = MARK })
        }
    }

    fun unmark(cell: Int) {
        if (state[cell] == MARK) {
            change(state.toMutableList().also { it[cell] = EMPTY })
        }
    }

    fun tap(cell: Int, autoMark: Boolean = false) {
        val next = when (state[cell]) { EMPTY -> MARK; MARK -> QUEEN; else -> EMPTY }
        val nextList = state.toMutableList()
        nextList[cell] = next
        if (autoMark && next == QUEEN) {
            val r = cell / n
            val c = cell % n
            val reg = level.puzzle.regions[cell]
            for (i in nextList.indices) {
                if (i == cell || nextList[i] != EMPTY) continue
                val ir = i / n
                val ic = i % n
                val isRow = ir == r
                val isCol = ic == c
                val isAdj = kotlin.math.abs(ir - r) <= 1 && kotlin.math.abs(ic - c) <= 1
                val isSameReg = level.puzzle.regions[i] == reg
                if (isRow || isCol || isAdj || isSameReg) {
                    nextList[i] = MARK
                }
            }
        }
        change(nextList)
    }

    fun clear() = change(List(n * n) { EMPTY })

    val queens: Set<Int> get() = state.indices.filter { state[it] == QUEEN }.toSet()

    /** Celdas con reina en conflicto (fila, columna, región o contacto). */
    val conflicts: Set<Int> get() = QueensRules.conflicts(level.puzzle, queens)

    val isSolved: Boolean get() = QueensRules.isSolved(level.puzzle, queens)

    /** Devuelve la pista lógica actual (o error) según QueensHintEngine. */
    fun currentHint(): QueensHint? = QueensHintEngine.nextHint(level, state)

    /** Aplica la deducción de la pista en un único paso de historial (deshacer). */
    fun applyHint(hint: QueensHint, autoMark: Boolean = true) {
        val nextList = state.toMutableList()
        when (hint) {
            is QueensHint.Wrong -> {
                for (c in hint.cells) {
                    nextList[c] = EMPTY
                }
            }
            is QueensHint.PlaceQueen -> {
                val cell = hint.cell
                nextList[cell] = QUEEN
                if (autoMark) {
                    val r = cell / n
                    val c = cell % n
                    val reg = level.puzzle.regions[cell]
                    for (i in nextList.indices) {
                        if (i == cell || nextList[i] != EMPTY) continue
                        val ir = i / n
                        val ic = i % n
                        val isRow = ir == r
                        val isCol = ic == c
                        val isAdj = kotlin.math.abs(ir - r) <= 1 && kotlin.math.abs(ic - c) <= 1
                        val isSameReg = level.puzzle.regions[i] == reg
                        if (isRow || isCol || isAdj || isSameReg) {
                            nextList[i] = MARK
                        }
                    }
                }
            }
            is QueensHint.Eliminate -> {
                for (c in hint.cells) {
                    if (nextList[c] == EMPTY) {
                        nextList[c] = MARK
                    }
                }
            }
        }
        change(nextList)
    }

    /** Atajo de depuración: coloca la reina correcta de la primera fila que no la tiene. */
    fun hint(): Int? {
        for (r in 0 until n) {
            val target = r * n + level.solution[r]
            if (state[target] == QUEEN) continue
            val next = state.toMutableList()
            for (c in 0 until n) if (next[r * n + c] == QUEEN) next[r * n + c] = EMPTY
            next[target] = QUEEN
            change(next)
            return target
        }
        return null
    }

    companion object {
        const val EMPTY = 0
        const val MARK = 1
        const val QUEEN = 2
    }
}
