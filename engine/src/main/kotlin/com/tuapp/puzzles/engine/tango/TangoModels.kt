package com.tuapp.puzzles.engine.tango

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel

const val EMPTY = -1
const val SUN = 0
const val MOON = 1

/** Relación entre dos celdas adyacentes (índices fila*size+col): igual (=) u opuesta (x). */
data class TangoConstraint(val a: Int, val b: Int, val equal: Boolean)

/** Puzzle inmutable: celdas dadas (EMPTY si no) y relaciones. */
data class TangoPuzzle(
    val size: Int,
    val givens: List<Int>,
    val constraints: List<TangoConstraint>,
) {
    init {
        require(size % 2 == 0) { "El tamaño debe ser par" }
        require(givens.size == size * size) { "givens debe tener size*size elementos" }
    }

    /** Índice celda -> relaciones en las que participa (acelera las comprobaciones). */
    val byCell: Map<Int, List<TangoConstraint>> by lazy {
        val m = HashMap<Int, MutableList<TangoConstraint>>()
        for (c in constraints) {
            m.getOrPut(c.a) { mutableListOf() }.add(c)
            m.getOrPut(c.b) { mutableListOf() }.add(c)
        }
        m
    }
}

data class TangoLevel(
    override val seed: Long,
    override val difficulty: Int,
    val puzzle: TangoPuzzle,
    val solution: List<Int>,
) : PuzzleLevel {
    override val game: GameType get() = GameType.TANGO
}
