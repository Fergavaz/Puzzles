package com.tuapp.puzzles.engine.zip

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel

/** Muro entre dos celdas adyacentes (a < b, índices fila*width+col). */
data class Edge(val a: Int, val b: Int) {
    init { require(a < b) }
    companion object {
        fun of(x: Int, y: Int) = if (x < y) Edge(x, y) else Edge(y, x)
    }
}

/**
 * numbers[celda] = 0 si no está numerada, o k en 1..K (K números consecutivos).
 * Regla adoptada: el camino empieza en el 1, pasa por los números en orden, cubre todas las
 * celdas y termina en el número más alto.
 */
data class ZipPuzzle(
    val width: Int,
    val height: Int,
    val numbers: List<Int>,
    val walls: List<Edge> = emptyList(),
) {
    init {
        require(numbers.size == width * height)
        val nums = numbers.filter { it > 0 }.sorted()
        require(nums == (1..nums.size).toList()) { "Los números deben ser 1..K sin repetir" }
        require(nums.size >= 2)
    }

    val cellCount: Int get() = width * height
    val maxNumber: Int by lazy { numbers.max() }
    val startCell: Int by lazy { numbers.indexOf(1) }
    val endCell: Int by lazy { numbers.indexOf(maxNumber) }

    /** Vecinos transitables (sin cruzar muros). */
    val adjacency: List<IntArray> by lazy {
        val wallSet = walls.toHashSet()
        List(cellCount) { i ->
            val r = i / width
            val c = i % width
            val out = ArrayList<Int>(4)
            if (r > 0) out.add(i - width)
            if (r < height - 1) out.add(i + width)
            if (c > 0) out.add(i - 1)
            if (c < width - 1) out.add(i + 1)
            out.filter { Edge.of(i, it) !in wallSet }.toIntArray()
        }
    }
}

data class ZipLevel(
    override val seed: Long,
    override val difficulty: Int,
    val puzzle: ZipPuzzle,
    val solution: List<Int>,
) : PuzzleLevel {
    override val game: GameType get() = GameType.ZIP
}
