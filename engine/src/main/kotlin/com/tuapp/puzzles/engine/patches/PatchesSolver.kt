package com.tuapp.puzzles.engine.patches

/**
 * Solver de partición en rectángulos (cobertura exacta).
 * 1. Para cada pista se enumeran todos los rectángulos candidatos (contienen la pista, ninguna
 *    otra, y cumplen forma/área).
 * 2. Backtracking: se ramifica sobre la pista con menos candidatos válidos (MRV). En cada nodo hay
 *    "forward checking": si alguna pista sin colocar se queda sin candidatos, o alguna celda libre
 *    ya no puede ser cubierta por ningún candidato, se poda.
 * Acepta parches ya colocados ([fixed]) para generar pistas a partir del estado del jugador.
 */
object PatchesSolver {

    fun countSolutions(p: PatchesPuzzle, limit: Int = 2, fixed: List<Placement> = emptyList()): Int =
        Search(p, limit, fixed).run().first

    /**
     * Como [countSolutions] pero con un tope de nodos de búsqueda: devuelve -1 si se agota
     * (resultado desconocido). Lo usa el generador para no perder tiempo en pistas muy débiles.
     */
    fun countSolutionsBounded(p: PatchesPuzzle, limit: Int, maxNodes: Int): Int {
        val s = Search(p, limit, emptyList(), maxNodes)
        val r = s.run().first
        return if (s.aborted) -1 else r
    }

    /** Rectángulo de cada pista (mismo orden que p.clues), o null si no hay solución. */
    fun solve(p: PatchesPuzzle, fixed: List<Placement> = emptyList()): List<Rect>? =
        Search(p, 1, fixed).run().second

    private class Cand(val rect: Rect, val cells: IntArray)

    private class Search(
        val p: PatchesPuzzle,
        val limit: Int,
        val fixed: List<Placement>,
        val maxNodes: Int = Int.MAX_VALUE,
    ) {
        var nodes = 0
        var aborted = false
        val n = p.size
        val k = p.clues.size
        val covered = BooleanArray(n * n)
        val placed = BooleanArray(k)
        val chosen = arrayOfNulls<Rect>(k)
        var count = 0
        var first: List<Rect>? = null
        val cands: List<List<Cand>> = List(k) { enumerate(it) }

        fun enumerate(ci: Int): List<Cand> {
            val out = ArrayList<Cand>()
            val cr = p.clues[ci].cell / n
            val cc = p.clues[ci].cell % n
            for (r0 in 0..cr) for (c0 in 0..cc) for (h in (cr - r0 + 1)..(n - r0)) for (w in (cc - c0 + 1)..(n - c0)) {
                val rect = Rect(r0, c0, h, w)
                if (PatchesRules.check(p, ci, rect) == RectCheck.OK) out.add(Cand(rect, rect.cells(n)))
            }
            return out
        }

        fun run(): Pair<Int, List<Rect>?> {
            for (f in fixed) {
                if (placed[f.clueIndex] || PatchesRules.check(p, f.clueIndex, f.rect) != RectCheck.OK) return 0 to null
                val cells = f.rect.cells(n)
                if (cells.any { covered[it] }) return 0 to null
                cells.forEach { covered[it] = true }
                placed[f.clueIndex] = true
                chosen[f.clueIndex] = f.rect
            }
            rec(k - fixed.size)
            return count to first
        }

        fun rec(remaining: Int) {
            if (count >= limit || aborted) return
            if (++nodes > maxNodes) { aborted = true; return }
            if (remaining == 0) {
                if (covered.all { it }) { count++; if (first == null) first = chosen.map { it!! } }
                return
            }
            var bestClue = -1
            var best: List<Cand>? = null
            val coverable = BooleanArray(n * n)
            for (ci in 0 until k) {
                if (placed[ci]) continue
                val valid = cands[ci].filter { cd -> cd.cells.none { covered[it] } }
                if (valid.isEmpty()) return
                for (cd in valid) for (x in cd.cells) coverable[x] = true
                if (best == null || valid.size < best.size) { best = valid; bestClue = ci }
            }
            for (x in 0 until n * n) if (!covered[x] && !coverable[x]) return
            for (cd in best!!) {
                cd.cells.forEach { covered[it] = true }
                placed[bestClue] = true; chosen[bestClue] = cd.rect
                rec(remaining - 1)
                placed[bestClue] = false; chosen[bestClue] = null
                cd.cells.forEach { covered[it] = false }
            }
        }
    }
}
