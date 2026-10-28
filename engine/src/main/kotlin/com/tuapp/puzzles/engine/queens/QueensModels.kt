package com.tuapp.puzzles.engine.queens

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel

/** regions[fila*size+col] = id de región (0 until size). */
data class QueensPuzzle(val size: Int, val regions: List<Int>) {
    init { require(regions.size == size * size) }
}

/** solution[fila] = columna de la reina de esa fila. */
data class QueensLevel(
    override val seed: Long,
    override val difficulty: Int,
    val puzzle: QueensPuzzle,
    val solution: List<Int>,
) : PuzzleLevel {
    override val game: GameType get() = GameType.QUEENS
}
