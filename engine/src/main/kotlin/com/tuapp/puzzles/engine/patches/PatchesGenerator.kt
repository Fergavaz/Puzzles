package com.tuapp.puzzles.engine.patches

import com.tuapp.puzzles.core.Difficulty
import kotlin.random.Random

/**
 * Generador determinista.
 * 1. Partición del tablero: se parte de un único rectángulo y se corta recursivamente (cortes
 *    horizontales/verticales aleatorios, eligiendo el rectángulo a cortar con probabilidad
 *    proporcional a su área) hasta llegar al número de parches objetivo. Todos son rectángulos.
 * 2. Cada parche recibe una pista en una celda aleatoria, con su tipo exacto (cuadrado/ancho/alto)
 *    y su área. Si esa versión "completa" no tiene solución única, se descarta la partición.
 * 3. Para subir la dificultad se debilitan pistas en orden aleatorio (quitar el número, o pasarla a
 *    libre) y solo se acepta el cambio si el solver sigue encontrando exactamente 1 solución.
 * Devuelve null si agota los intentos (el llamador usa un nivel precargado de respaldo).
 */
class PatchesGenerator {

    fun generate(seed: Long, difficulty: Int, maxAttempts: Int = 400): PatchesLevel? {
        val d = Difficulty.clamp(difficulty)
        val size = SIZES[d - 1]
        val rnd = Random(seed)
        repeat(maxAttempts) {
            val rects = partition(size, TARGET_PATCHES.getValue(size), rnd)
            // Pista en una celda aleatoria de cada parche; se ordenan por celda para ser canónicos
            val pairs = rects.map { r -> r to r.cells(size)[rnd.nextInt(r.area)] }.sortedBy { it.second }
            val clues = pairs.map { (r, cell) -> PatchClue(cell, exactType(r), r.area) }
            val solution = pairs.map { it.first }
            if (PatchesSolver.countSolutions(PatchesPuzzle(size, clues), 2) != 1) return@repeat
            return PatchesLevel(seed, d, PatchesPuzzle(size, weaken(size, clues, d, rnd)), solution)
        }
        return null
    }

    private fun exactType(r: Rect) = when {
        r.height == r.width -> PatchType.SQUARE
        r.width > r.height -> PatchType.WIDE
        else -> PatchType.TALL
    }

    private fun weaken(size: Int, clues: List<PatchClue>, d: Int, rnd: Random): List<PatchClue> {
        val cur = clues.toMutableList()
        val (pNumber, pFree) = WEAKEN[d - 1]
        fun unique(i: Int, c: PatchClue): Boolean {
            val old = cur[i]
            cur[i] = c
            // Con tope de nodos: si el solver no concluye rápido, la pista débil se rechaza
            val ok = PatchesSolver.countSolutionsBounded(PatchesPuzzle(size, cur), 2, NODE_BUDGET) == 1
            cur[i] = old
            return ok
        }
        for (i in cur.indices.shuffled(rnd)) {
            if (cur[i].area != null && rnd.nextDouble() < pNumber) {
                val c = cur[i].copy(area = null)
                if (unique(i, c)) cur[i] = c
            }
            if (cur[i].type != PatchType.FREE && rnd.nextDouble() < pFree) {
                val c = cur[i].copy(type = PatchType.FREE)
                if (unique(i, c)) cur[i] = c
            }
        }
        return cur
    }

    private fun partition(size: Int, target: Int, rnd: Random): List<Rect> {
        val rects = mutableListOf(Rect(0, 0, size, size))
        while (rects.size < target) {
            val splittable = rects.filter { it.area >= 2 }
            if (splittable.isEmpty()) break
            // Elegir un rectángulo con probabilidad proporcional a su área
            var t = rnd.nextInt(splittable.sumOf { it.area })
            val r = splittable.first { t -= it.area; t < 0 }
            val horizontal = r.height > 1 && (r.width == 1 || rnd.nextBoolean())
            rects.remove(r)
            if (horizontal) {
                val k = 1 + rnd.nextInt(r.height - 1)
                rects.add(Rect(r.row, r.col, k, r.width))
                rects.add(Rect(r.row + k, r.col, r.height - k, r.width))
            } else {
                val k = 1 + rnd.nextInt(r.width - 1)
                rects.add(Rect(r.row, r.col, r.height, k))
                rects.add(Rect(r.row, r.col + k, r.height, r.width - k))
            }
        }
        return rects
    }

    companion object {
        /** Lado del tablero para dificultad 1..5. */
        private val SIZES = intArrayOf(6, 6, 8, 10, 10)
        private const val NODE_BUDGET = 3000
        private val TARGET_PATCHES = mapOf(6 to 9, 8 to 13, 10 to 17)
        /** (prob. de quitar el número, prob. de pasar a libre) por dificultad. */
        private val WEAKEN = arrayOf(0.0 to 0.0, 0.5 to 0.0, 0.6 to 0.3, 0.8 to 0.6, 1.0 to 1.0)
    }
}
