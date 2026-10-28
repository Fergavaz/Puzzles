package com.tuapp.puzzles.engine.patches

enum class RectCheck {
    OK, OUT_OF_BOUNDS, NOT_CONTAINING_CLUE, CONTAINS_OTHER_CLUE, WRONG_SHAPE, WRONG_AREA,
}

object PatchesRules {

    /** Valida un rectángulo (p. ej. mientras se arrastra) contra su pista. OK = forma/área/pista correctas. */
    fun check(p: PatchesPuzzle, clueIndex: Int, rect: Rect): RectCheck {
        val n = p.size
        val clue = p.clues[clueIndex]
        if (rect.row < 0 || rect.col < 0 || rect.row + rect.height > n || rect.col + rect.width > n) {
            return RectCheck.OUT_OF_BOUNDS
        }
        if (!rect.containsCell(clue.cell, n)) return RectCheck.NOT_CONTAINING_CLUE
        if (p.clues.indices.any { it != clueIndex && rect.containsCell(p.clues[it].cell, n) }) {
            return RectCheck.CONTAINS_OTHER_CLUE
        }
        val shapeOk = when (clue.type) {
            PatchType.SQUARE -> rect.height == rect.width
            PatchType.WIDE -> rect.width > rect.height
            PatchType.TALL -> rect.height > rect.width
            PatchType.FREE -> true
        }
        if (!shapeOk) return RectCheck.WRONG_SHAPE
        if (clue.area != null && rect.area != clue.area) return RectCheck.WRONG_AREA
        return RectCheck.OK
    }

    /** Un parche por pista, todos válidos, sin solapes y cubriendo todo el tablero. */
    fun isSolved(p: PatchesPuzzle, placements: List<Placement>): Boolean {
        if (placements.size != p.clues.size) return false
        if (placements.map { it.clueIndex }.toSet() != p.clues.indices.toSet()) return false
        val covered = BooleanArray(p.size * p.size)
        for (pl in placements) {
            if (check(p, pl.clueIndex, pl.rect) != RectCheck.OK) return false
            for (cell in pl.rect.cells(p.size)) {
                if (covered[cell]) return false
                covered[cell] = true
            }
        }
        return covered.all { it }
    }
}
