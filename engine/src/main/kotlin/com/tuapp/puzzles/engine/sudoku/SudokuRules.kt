package com.tuapp.puzzles.engine.sudoku

object SudokuRules {

    /** Celdas con valor repetido en su fila, columna o región (para pintarlas en rojo). */
    fun conflicts(cells: List<Int>): Set<Int> {
        val bad = HashSet<Int>()
        for (i in cells.indices) {
            val v = cells[i]
            if (v == 0) continue
            if (SudokuGrid.peers[i].any { cells[it] == v }) bad.add(i)
        }
        return bad
    }

    fun isSolved(p: SudokuPuzzle, cells: List<Int>): Boolean {
        if (cells.size != SudokuGrid.CELLS) return false
        for (i in cells.indices) {
            if (cells[i] !in 1..6) return false
            if (p.givens[i] != 0 && p.givens[i] != cells[i]) return false
        }
        return conflicts(cells).isEmpty()
    }
}
