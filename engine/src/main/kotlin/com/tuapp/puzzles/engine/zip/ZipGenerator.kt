package com.tuapp.puzzles.engine.zip

import com.tuapp.puzzles.core.Difficulty
import kotlin.random.Random

/**
 * Generador determinista.
 * 1. Camino hamiltoniano aleatorio: se parte de un recorrido en serpiente y se aplican muchos
 *    movimientos "backbite" (rotaciones de Pósa), que conservan siempre un camino hamiltoniano válido.
 * 2. Se numeran el primer y el último punto del camino más algunos intermedios (menos = más difícil).
 * 3. Mientras el solver encuentre más de una solución se añade un muro en una arista que el camino
 *    no usa (solo dificultad >= 3) o un número extra. Con todas las celdas numeradas la solución es
 *    única, así que el bucle siempre termina con un puzzle de solución única.
 */
class ZipGenerator {

    fun generate(seed: Long, difficulty: Int): ZipLevel {
        val d = Difficulty.clamp(difficulty)
        val size = SIZES[d - 1]
        val n = size * size
        val rnd = Random(seed)
        val path = randomPath(size, size, rnd)

        val marked = sortedSetOf(0, n - 1)
        val pool = (1 until n - 1).shuffled(rnd).toMutableList()
        repeat(EXTRA_NUMBERS[d - 1]) { if (pool.isNotEmpty()) marked.add(pool.removeAt(0)) }

        val pathEdges = (0 until n - 1).map { Edge.of(path[it], path[it + 1]) }.toHashSet()
        val freeEdges = allEdges(size, size).filter { it !in pathEdges }.shuffled(rnd).toMutableList()
        val walls = ArrayList<Edge>()
        val maxWalls = if (d >= 3) n / 4 else 0

        while (true) {
            val numbers = IntArray(n)
            marked.forEachIndexed { rank, idx -> numbers[path[idx]] = rank + 1 }
            val puzzle = ZipPuzzle(size, size, numbers.toList(), walls.sortedWith(compareBy({ it.a }, { it.b })))
            if (ZipSolver.countSolutions(puzzle, 2) == 1) return ZipLevel(seed, d, puzzle, path.toList())

            if (walls.size < maxWalls && freeEdges.isNotEmpty() && rnd.nextBoolean()) {
                walls.add(freeEdges.removeAt(0))
            } else {
                marked.add(pool.removeAt(0))
            }
        }
    }

    private fun allEdges(w: Int, h: Int): List<Edge> {
        val out = ArrayList<Edge>()
        for (r in 0 until h) for (c in 0 until w) {
            val i = r * w + c
            if (c + 1 < w) out.add(Edge(i, i + 1))
            if (r + 1 < h) out.add(Edge(i, i + w))
        }
        return out
    }

    private fun randomPath(w: Int, h: Int, rnd: Random): IntArray {
        val n = w * h
        val p = IntArray(n)
        var k = 0
        for (r in 0 until h) {
            val cols = if (r % 2 == 0) (0 until w).toList() else (w - 1 downTo 0).toList()
            for (c in cols) p[k++] = r * w + c
        }
        val pos = IntArray(n)
        fun reindex() { for (i in 0 until n) pos[p[i]] = i }
        reindex()
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        repeat(n * 80) {
            val atTail = rnd.nextBoolean()
            if (atTail) { p.reverse(); reindex() }
            val head = p[0]
            val (dr, dc) = dirs[rnd.nextInt(4)]
            val r = head / w + dr
            val c = head % w + dc
            if (r in 0 until h && c in 0 until w) {
                val j = pos[r * w + c]
                // Backbite: unir la cabeza con p[j] e invertir el tramo p[0..j-1]
                if (j > 1) { p.reverse(0, j); reindex() }
            }
            if (atTail) { p.reverse(); reindex() }
        }
        return p
    }

    companion object {
        /** Lado del tablero para dificultad 1..5. */
        private val SIZES = intArrayOf(4, 5, 6, 6, 7)
        /** Números intermedios iniciales (además del 1 y el último). */
        private val EXTRA_NUMBERS = intArrayOf(4, 3, 2, 1, 0)
    }
}
