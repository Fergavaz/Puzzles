package com.tuapp.puzzles.engine.zip

object ZipRules {

    /**
     * El camino (lista de celdas) es válido hasta ahora: empieza en el 1, celdas contiguas sin
     * cruzar muros, sin repetir celdas y con los números visitados en orden ascendente.
     */
    fun isValidPrefix(p: ZipPuzzle, path: List<Int>): Boolean {
        if (path.isEmpty()) return true
        if (path[0] != p.startCell) return false
        val seen = BooleanArray(p.cellCount)
        var next = 1
        for (k in path.indices) {
            val cell = path[k]
            if (cell !in 0 until p.cellCount || seen[cell]) return false
            seen[cell] = true
            if (k > 0 && p.adjacency[path[k - 1]].none { it == cell }) return false
            val num = p.numbers[cell]
            if (num != 0) {
                if (num != next) return false
                next++
            }
        }
        return true
    }

    fun isSolved(p: ZipPuzzle, path: List<Int>): Boolean =
        path.size == p.cellCount && path.last() == p.endCell && isValidPrefix(p, path)
}
