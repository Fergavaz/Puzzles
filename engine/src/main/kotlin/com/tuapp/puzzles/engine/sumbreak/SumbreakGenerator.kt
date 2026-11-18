package com.tuapp.puzzles.engine.sumbreak

import com.tuapp.puzzles.core.Difficulty
import kotlin.random.Random

class SumbreakGenerator {

    fun generate(seed: Long, difficulty: Int): SumbreakLevel {
        val d = Difficulty.clamp(difficulty)
        val rnd = Random(seed)

        val size = when (d) {
            1 -> 3
            2, 3 -> 4
            else -> 5
        }

        val targetMoves = when (d) {
            1 -> 4
            2 -> 6
            3 -> 8
            4 -> 10
            else -> 12
        }

        val baseTargetValues = when (d) {
            1 -> listOf(16, 32)
            2 -> listOf(32, 64)
            3 -> listOf(48, 64, 96)
            4 -> listOf(64, 96, 128)
            else -> listOf(96, 128, 192, 256)
        }

        var attempt = 0
        while (attempt < 50) {
            attempt++
            val targetVal = baseTargetValues.random(rnd)
            val grid = ArrayList<SumbreakTile>(List(size * size) { SumbreakTile.Empty })

            // Colocar la ficha final en una casilla central
            val startCell = (size / 2) * size + (size / 2)
            grid[startCell] = SumbreakTile.Number(targetVal)

            var steps = 0
            val reverseMoves = ArrayList<SumbreakMove>()
            var attemptsLeft = 60

            while (steps < targetMoves && attemptsLeft-- > 0) {
                // Buscar casillas con números que tengan un vecino vacío
                val candidates = mutableListOf<Pair<Int, Int>>() // (numCell, emptyNeighbor)
                for (cell in 0 until size * size) {
                    val tile = grid[cell]
                    if (tile is SumbreakTile.Number && tile.value >= 2) {
                        for (neighbor in adjacentCells(size, cell)) {
                            if (grid[neighbor] is SumbreakTile.Empty) {
                                candidates.add(cell to neighbor)
                            }
                        }
                    }
                }

                if (candidates.isEmpty()) break
                candidates.shuffle(rnd)
                val (numCell, emptyNeighbor) = candidates.first()
                val currentNum = (grid[numCell] as SumbreakTile.Number).value

                // Descomponer currentNum en 2 fichas
                val (tileForNumCell, tileForNeighbor) = decompose(currentNum, d, rnd)
                grid[numCell] = tileForNumCell
                grid[emptyNeighbor] = tileForNeighbor
                // En el juego hacia adelante: la ficha en emptyNeighbor se arrastra hacia numCell
                reverseMoves.add(SumbreakMove(emptyNeighbor, numCell))
                steps++
            }

            if (steps >= targetMoves - 1) {
                val puzzle = SumbreakPuzzle(size, grid)
                val forwardMoves = reverseMoves.reversed()
                var cur: List<SumbreakTile>? = grid.toList()
                for (m in forwardMoves) {
                    cur = cur?.let { SumbreakRules.applyMove(size, it, m) }
                    if (cur == null) break
                }
                if (cur != null && SumbreakRules.isSolved(cur)) {
                    return SumbreakLevel(
                        seed = seed,
                        difficulty = d,
                        puzzle = puzzle,
                        targetNumber = targetVal,
                        solutionMoves = forwardMoves,
                    )
                }
            }
        }

        // Respaldo de emergencia determinista si se agotan intentos
        return createFallback(seed, d, size)
    }

    private fun adjacentCells(size: Int, cell: Int): List<Int> {
        val r = cell / size
        val c = cell % size
        val list = mutableListOf<Int>()
        if (r > 0) list.add(cell - size)
        if (r < size - 1) list.add(cell + size)
        if (c > 0) list.add(cell - 1)
        if (c < size - 1) list.add(cell + 1)
        return list
    }

    private fun decompose(value: Int, difficulty: Int, rnd: Random): Pair<SumbreakTile, SumbreakTile> {
        val options = mutableListOf<Pair<SumbreakTile, SumbreakTile>>()

        // Opción 1: división exacta en 2 números idénticos (siempre válida si es par)
        if (value % 2 == 0 && value >= 2) {
            val half = value / 2
            options.add(SumbreakTile.Number(half) to SumbreakTile.Number(half))
        }

        // Opción 2: Modificador Suma
        if (difficulty >= 2) {
            val addends = intArrayOf(2, 3, 4, 5, 6)
            for (a in addends) {
                if (value - a >= 2 && (value - a) % 2 == 0) {
                    options.add(SumbreakTile.Number(value - a) to SumbreakTile.Modifier(ModifierOp.ADD, a))
                }
            }
        }

        // Opción 3: Modificador Multiplicación
        if (difficulty >= 3) {
            if (value % 2 == 0 && value / 2 >= 2) {
                options.add(SumbreakTile.Number(value / 2) to SumbreakTile.Modifier(ModifierOp.MUL, 2))
            }
            if (value % 3 == 0 && value / 3 >= 2) {
                options.add(SumbreakTile.Number(value / 3) to SumbreakTile.Modifier(ModifierOp.MUL, 3))
            }
        }

        // Opción 4: Comodín Estrella
        if (difficulty >= 2 && value % 2 == 0 && value >= 2) {
            val half = value / 2
            options.add(SumbreakTile.Number(half) to SumbreakTile.Modifier(ModifierOp.WILDCARD))
        }

        // Respaldo garantizado: nunca retorna nulo
        if (options.isEmpty()) {
            if (value % 2 == 0 && value >= 2) {
                options.add(SumbreakTile.Number(value / 2) to SumbreakTile.Number(value / 2))
            } else if (value >= 3) {
                options.add(SumbreakTile.Number(value - 1) to SumbreakTile.Modifier(ModifierOp.ADD, 1))
            } else {
                options.add(SumbreakTile.Number(1) to SumbreakTile.Number(1))
            }
        }

        return options.random(rnd)
    }

    private fun createFallback(seed: Long, difficulty: Int, size: Int): SumbreakLevel {
        val grid = ArrayList<SumbreakTile>(List(size * size) { SumbreakTile.Empty })
        // Nivel mínimo válido con 4 casillas fusionables: 4 -> (2+2) y (2+2)
        grid[0] = SumbreakTile.Number(4)
        grid[1] = SumbreakTile.Number(4)
        grid[size] = SumbreakTile.Number(8)
        val puzzle = SumbreakPuzzle(size, grid)
        val sol = listOf(SumbreakMove(0, 1), SumbreakMove(1, size))
        return SumbreakLevel(seed, difficulty, puzzle, 16, sol)
    }
}
