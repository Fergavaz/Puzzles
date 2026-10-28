package com.tuapp.puzzles.engine.tango

import com.tuapp.puzzles.core.Difficulty
import kotlin.random.Random

/**
 * Generador determinista (misma semilla + dificultad => mismo puzzle).
 *
 * Estrategia:
 * 1. Solución completa aleatoria por backtracking.
 * 2. Se parte del puzzle con TODAS las celdas dadas y TODAS las relaciones (único trivialmente).
 * 3. En orden aleatorio se intenta quitar cada elemento; solo se acepta si el solver sigue
 *    encontrando exactamente 1 solución. Así la unicidad queda garantizada por construcción.
 * 4. La dificultad fija cuántas pistas quedan como máximo (5 = mínimo irreducible).
 */
class TangoGenerator(private val size: Int = 6) {

    fun generate(seed: Long, difficulty: Int): TangoLevel {
        val d = Difficulty.clamp(difficulty)
        val rnd = Random(seed)
        val n = size
        val solution = randomSolution(rnd)

        val edges = ArrayList<Pair<Int, Int>>()
        for (r in 0 until n) for (c in 0 until n) {
            val i = r * n + c
            if (c + 1 < n) edges.add(i to i + 1)
            if (r + 1 < n) edges.add(i to i + n)
        }
        val keepGiven = BooleanArray(n * n) { true }
        val keepEdge = BooleanArray(edges.size) { true }

        fun build() = TangoPuzzle(
            n,
            List(n * n) { if (keepGiven[it]) solution[it] else EMPTY },
            edges.indices.filter { keepEdge[it] }.map {
                val (a, b) = edges[it]
                TangoConstraint(a, b, solution[a] == solution[b])
            },
        )

        val items = (0 until n * n + edges.size).toMutableList()
        items.shuffle(rnd)
        var remaining = items.size
        val target = TARGET_CLUES[d - 1]

        for (item in items) {
            if (remaining <= target) break
            setKeep(item, n * n, keepGiven, keepEdge, false)
            if (TangoSolver.countSolutions(build(), 2) == 1) remaining--
            else setKeep(item, n * n, keepGiven, keepEdge, true)
        }
        return TangoLevel(seed, d, build(), solution.toList())
    }

    private fun setKeep(item: Int, cells: Int, g: BooleanArray, e: BooleanArray, value: Boolean) {
        if (item < cells) g[item] = value else e[item - cells] = value
    }

    private fun randomSolution(rnd: Random): IntArray {
        val empty = TangoPuzzle(size, List(size * size) { EMPTY }, emptyList())
        val g = IntArray(size * size) { EMPTY }
        fun rec(i: Int): Boolean {
            if (i == g.size) return true
            val order = if (rnd.nextBoolean()) intArrayOf(SUN, MOON) else intArrayOf(MOON, SUN)
            for (v in order) {
                g[i] = v
                if (TangoRules.consistentAt(empty, g, i) && rec(i + 1)) return true
            }
            g[i] = EMPTY
            return false
        }
        check(rec(0)) { "No se pudo generar una solución de Tango" }
        return g
    }

    companion object {
        /** Máximo de pistas (celdas dadas + relaciones) para dificultad 1..5. */
        private val TARGET_CLUES = intArrayOf(24, 18, 14, 10, 0)
    }
}
