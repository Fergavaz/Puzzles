package com.tuapp.puzzles.engine.queens

import kotlin.math.abs

object QueensRules {

    /** Celdas con reina que chocan con otra (misma fila, columna, región o contacto en las 8 direcciones). */
    fun conflicts(p: QueensPuzzle, queens: Set<Int>): Set<Int> {
        val n = p.size
        val list = queens.toList()
        val bad = HashSet<Int>()
        for (x in list.indices) for (y in x + 1 until list.size) {
            val a = list[x]; val b = list[y]
            val ra = a / n; val ca = a % n; val rb = b / n; val cb = b % n
            if (ra == rb || ca == cb || p.regions[a] == p.regions[b] ||
                (abs(ra - rb) <= 1 && abs(ca - cb) <= 1)
            ) { bad.add(a); bad.add(b) }
        }
        return bad
    }

    /** N reinas sin conflictos => exactamente una por fila, columna y región (principio del palomar). */
    fun isSolved(p: QueensPuzzle, queens: Set<Int>): Boolean =
        queens.size == p.size && conflicts(p, queens).isEmpty()
}
