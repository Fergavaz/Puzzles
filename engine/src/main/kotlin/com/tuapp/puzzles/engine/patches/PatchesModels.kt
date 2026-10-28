package com.tuapp.puzzles.engine.patches

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel

enum class PatchType {
    /** Cuadrado perfecto (incluye 1x1). */
    SQUARE,
    /** Estrictamente más ancho que alto. */
    WIDE,
    /** Estrictamente más alto que ancho. */
    TALL,
    /** Cualquier rectángulo, cuadrados incluidos. */
    FREE,
}

/** Pista en la celda [cell] (fila*size+col). [area] = null si la pista no muestra número. */
data class PatchClue(val cell: Int, val type: PatchType, val area: Int?)

data class Rect(val row: Int, val col: Int, val height: Int, val width: Int) {
    val area: Int get() = height * width

    fun contains(r: Int, c: Int) = r >= row && r < row + height && c >= col && c < col + width

    fun containsCell(cell: Int, size: Int) = contains(cell / size, cell % size)

    fun cells(size: Int): IntArray {
        val out = IntArray(area)
        var k = 0
        for (r in row until row + height) for (c in col until col + width) out[k++] = r * size + c
        return out
    }

    fun overlaps(o: Rect) =
        row < o.row + o.height && o.row < row + height && col < o.col + o.width && o.col < col + width

    companion object {
        /** Rectángulo definido por dos esquinas arrastradas (en cualquier orden). */
        fun fromCorners(r1: Int, c1: Int, r2: Int, c2: Int) = Rect(
            minOf(r1, r2), minOf(c1, c2), kotlin.math.abs(r1 - r2) + 1, kotlin.math.abs(c1 - c2) + 1,
        )
    }
}

data class PatchesPuzzle(val size: Int, val clues: List<PatchClue>) {
    init {
        require(clues.map { it.cell }.toSet().size == clues.size) { "Dos pistas en la misma celda" }
        require(clues.all { it.cell in 0 until size * size })
    }
}

/** Un parche colocado por el jugador para la pista [clueIndex]. */
data class Placement(val clueIndex: Int, val rect: Rect)

/** solution[i] es el rectángulo de la pista clues[i]. */
data class PatchesLevel(
    override val seed: Long,
    override val difficulty: Int,
    val puzzle: PatchesPuzzle,
    val solution: List<Rect>,
) : PuzzleLevel {
    override val game: GameType get() = GameType.PATCHES
}
