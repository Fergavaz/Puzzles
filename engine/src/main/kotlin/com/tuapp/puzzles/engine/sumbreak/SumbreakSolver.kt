package com.tuapp.puzzles.engine.sumbreak

import java.util.ArrayDeque

object SumbreakSolver {

    fun solve(puzzle: SumbreakPuzzle): List<SumbreakMove>? {
        return findSolution(puzzle.size, puzzle.initialTiles)
    }

    fun findWinningMovesFrom(size: Int, tiles: List<SumbreakTile>): List<SumbreakMove>? {
        return findSolution(size, tiles)
    }

    private fun findSolution(size: Int, initial: List<SumbreakTile>): List<SumbreakMove>? {
        if (SumbreakRules.isSolved(initial)) return emptyList()

        val queue = ArrayDeque<Pair<List<SumbreakTile>, List<SumbreakMove>>>()
        val visited = HashSet<List<SumbreakTile>>()

        queue.add(initial to emptyList())
        visited.add(initial)

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            val moves = SumbreakRules.validMoves(size, current)

            for (m in moves) {
                val next = SumbreakRules.applyMove(size, current, m) ?: continue
                val newPath = path + m
                if (SumbreakRules.isSolved(next)) {
                    return newPath
                }
                if (visited.add(next)) {
                    queue.add(next to newPath)
                }
            }
        }
        return null
    }

    fun countSolutions(puzzle: SumbreakPuzzle, maxSolutions: Int = 2): Int {
        var count = 0
        fun dfs(current: List<SumbreakTile>) {
            if (count >= maxSolutions) return
            if (SumbreakRules.isSolved(current)) {
                count++
                return
            }
            val moves = SumbreakRules.validMoves(puzzle.size, current)
            for (m in moves) {
                val next = SumbreakRules.applyMove(puzzle.size, current, m) ?: continue
                dfs(next)
                if (count >= maxSolutions) return
            }
        }
        dfs(puzzle.initialTiles)
        return count
    }
}
