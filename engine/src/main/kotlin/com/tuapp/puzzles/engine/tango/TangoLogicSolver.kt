package com.tuapp.puzzles.engine.tango

object TangoLogicSolver {

    /**
     * Comprueba si el puzzle se puede resolver aplicando únicamente deducciones lógicas directas
     * (T1 a T5: Signos, Parejas, Sándwich, Conteo y Comprobación de línea), sin requerir hipótesis ni tanteo.
     */
    fun solvableWithoutGuessing(puzzle: TangoPuzzle, knownSolution: List<Int>? = null): Boolean {
        val solution = knownSolution ?: TangoSolver.solve(puzzle) ?: return false
        val level = TangoLevel(seed = 0L, difficulty = 1, puzzle = puzzle, solution = solution)
        val cells = puzzle.givens.toMutableList()

        while (cells.any { it == EMPTY }) {
            val hint = TangoHintEngine.nextHint(level, cells) ?: return false
            when (hint) {
                is TangoHint.Place -> {
                    if (hint.technique == TangoTechnique.HYPOTHETICAL ||
                        hint.technique == TangoTechnique.SOLUTION_FALLBACK ||
                        hint.technique == TangoTechnique.WRONG_CELL
                    ) {
                        return false
                    }
                    cells[hint.cell] = hint.symbol
                }
                is TangoHint.Wrong -> return false
            }
        }
        return TangoRules.isSolved(puzzle, cells)
    }
}
