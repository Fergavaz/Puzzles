package com.tuapp.puzzles.engine.zip

/**
 * DFS con poda para caminos hamiltonianos con números ordenados.
 * Poda en cada nodo:
 *  - Conectividad: todas las celdas libres deben ser alcanzables desde la cabeza.
 *  - Grado: cada celda libre necesita >= 2 vecinos libres (o la cabeza) para entrar y salir;
 *    la celda final solo necesita 1.
 *  - Una celda numerada solo se puede pisar cuando le toca; llegar al final antes de cubrir todo es fallo.
 * Acepta un camino parcial ([prefix]) para generar pistas continuando desde lo que ya trazó el usuario.
 */
object ZipSolver {

    fun countSolutions(p: ZipPuzzle, limit: Int = 2, prefix: List<Int> = emptyList()): Int =
        Search(p, limit, prefix).run().first

    fun solve(p: ZipPuzzle, prefix: List<Int> = emptyList()): List<Int>? =
        Search(p, 1, prefix).run().second

    private class Search(val p: ZipPuzzle, val limit: Int, val prefix: List<Int>) {
        val n = p.cellCount
        val visited = BooleanArray(n)
        val path = IntArray(n)
        var len = 0
        var nextNum = 1
        var count = 0
        var first: List<Int>? = null

        fun push(cell: Int) {
            visited[cell] = true
            path[len++] = cell
            if (p.numbers[cell] != 0) nextNum++
        }

        fun pop() {
            val cell = path[--len]
            visited[cell] = false
            if (p.numbers[cell] != 0) nextNum--
        }

        fun run(): Pair<Int, List<Int>?> {
            val start = if (prefix.isEmpty()) listOf(p.startCell) else prefix
            if (!ZipRules.isValidPrefix(p, start)) return 0 to null
            start.forEach { push(it) }
            rec()
            return count to first
        }

        fun rec() {
            if (count >= limit) return
            val head = path[len - 1]
            if (len == n) {
                if (head == p.endCell) { count++; if (first == null) first = path.toList() }
                return
            }
            if (head == p.endCell) return
            if (!feasible(head)) return
            for (nb in p.adjacency[head]) {
                if (visited[nb]) continue
                val num = p.numbers[nb]
                if (num != 0 && num != nextNum) continue
                push(nb); rec(); pop()
            }
        }

        private val stack = IntArray(n)
        private val seen = BooleanArray(n)

        fun feasible(head: Int): Boolean {
            // Grado de cada celda libre
            for (c in 0 until n) {
                if (visited[c]) continue
                var deg = 0
                for (nb in p.adjacency[c]) if (!visited[nb] || nb == head) deg++
                if (deg < (if (c == p.endCell) 1 else 2)) return false
            }
            // Conectividad desde la cabeza
            java.util.Arrays.fill(seen, false)
            var sp = 0
            stack[sp++] = head; seen[head] = true
            var reached = 0
            while (sp > 0) {
                val i = stack[--sp]
                for (nb in p.adjacency[i]) {
                    if (!visited[nb] && !seen[nb]) { seen[nb] = true; reached++; stack[sp++] = nb }
                }
            }
            return reached == n - len
        }
    }
}
