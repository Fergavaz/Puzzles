package com.tuapp.puzzles.engine.sudoku

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel

/** Tablero 6x6, regiones de 2 filas x 3 columnas. 0 = celda vacía, 1..6 = valor. */
data class SudokuPuzzle(val givens: List<Int>) {
    init { require(givens.size == SudokuGrid.CELLS) }
}

data class SudokuLevel(
    override val seed: Long,
    override val difficulty: Int,
    val puzzle: SudokuPuzzle,
    val solution: List<Int>,
) : PuzzleLevel {
    override val game: GameType get() = GameType.SUDOKU
}

object SudokuGrid {
    const val SIZE = 6
    const val CELLS = 36

    fun row(i: Int) = i / SIZE
    fun col(i: Int) = i % SIZE
    /** Región 0..5: dos filas x tres columnas. */
    fun box(i: Int) = (row(i) / 2) * 2 + col(i) / 3

    /** Celdas que comparten fila, columna o región con cada celda. */
    val peers: Array<IntArray> = Array(CELLS) { i ->
        (0 until CELLS).filter { it != i && (row(it) == row(i) || col(it) == col(i) || box(it) == box(i)) }.toIntArray()
    }

    /** Las 18 unidades (6 filas, 6 columnas, 6 regiones). */
    val units: List<IntArray> =
        (0 until SIZE).map { r -> IntArray(SIZE) { c -> r * SIZE + c } } +
            (0 until SIZE).map { c -> IntArray(SIZE) { r -> r * SIZE + c } } +
            (0 until SIZE).map { b -> (0 until CELLS).filter { box(it) == b }.toIntArray() }

    /** Máscara de candidatos (bits 1..6) de la celda [i]. */
    fun candidates(g: IntArray, i: Int): Int {
        var m = 0b1111110
        for (p in peers[i]) if (g[p] != 0) m = m and (1 shl g[p]).inv()
        return m
    }
}
