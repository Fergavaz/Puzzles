package com.tuapp.puzzles.engine.sumbreak

import kotlin.math.abs

object SumbreakRules {

    fun isAdjacent(size: Int, from: Int, to: Int): Boolean {
        if (from !in 0 until size * size || to !in 0 until size * size || from == to) return false
        val r1 = from / size; val c1 = from % size
        val r2 = to / size; val c2 = to % size
        return abs(r1 - r2) + abs(c1 - c2) == 1
    }

    fun mergeResult(from: SumbreakTile, to: SumbreakTile): SumbreakTile? {
        if (from is SumbreakTile.Empty || to is SumbreakTile.Empty) return null

        if (from is SumbreakTile.Number && to is SumbreakTile.Number) {
            return if (from.value == to.value) {
                SumbreakTile.Number(from.value + to.value)
            } else null
        }

        if (from is SumbreakTile.Number && to is SumbreakTile.Modifier) {
            return applyModifier(from.value, to)
        }

        if (from is SumbreakTile.Modifier && to is SumbreakTile.Number) {
            return applyModifier(to.value, from)
        }

        return null
    }

    private fun applyModifier(baseValue: Int, mod: SumbreakTile.Modifier): SumbreakTile? {
        return when (mod.op) {
            ModifierOp.ADD -> SumbreakTile.Number(baseValue + mod.value)
            ModifierOp.SUB -> {
                val res = baseValue - mod.value
                if (res > 0) SumbreakTile.Number(res) else null
            }
            ModifierOp.MUL -> SumbreakTile.Number(baseValue * mod.value)
            ModifierOp.WILDCARD -> SumbreakTile.Number(baseValue * 2)
        }
    }

    fun validMoves(size: Int, tiles: List<SumbreakTile>): List<SumbreakMove> {
        val moves = ArrayList<SumbreakMove>()
        val total = size * size
        val deltas = intArrayOf(-size, size, -1, 1) // Arriba, abajo, izquierda, derecha

        for (from in 0 until total) {
            val fromTile = tiles[from]
            if (fromTile is SumbreakTile.Empty) continue
            val r = from / size
            val c = from % size

            for (d in deltas) {
                val to = from + d
                if (to !in 0 until total) continue
                val toR = to / size
                val toC = to % size
                if (abs(r - toR) + abs(c - toC) != 1) continue

                val toTile = tiles[to]
                if (toTile !is SumbreakTile.Empty && mergeResult(fromTile, toTile) != null) {
                    moves.add(SumbreakMove(from, to))
                }
            }
        }
        return moves
    }

    fun applyMove(size: Int, tiles: List<SumbreakTile>, move: SumbreakMove): List<SumbreakTile>? {
        if (!isAdjacent(size, move.from, move.to)) return null
        val res = mergeResult(tiles[move.from], tiles[move.to]) ?: return null
        val next = tiles.toMutableList()
        next[move.from] = SumbreakTile.Empty
        next[move.to] = res
        return next
    }

    fun isSolved(tiles: List<SumbreakTile>): Boolean {
        var numberCount = 0
        for (tile in tiles) {
            when (tile) {
                is SumbreakTile.Number -> numberCount++
                is SumbreakTile.Modifier -> return false
                SumbreakTile.Empty -> Unit
            }
        }
        return numberCount == 1
    }

    fun isDeadEnd(size: Int, tiles: List<SumbreakTile>): Boolean {
        return !isSolved(tiles) && validMoves(size, tiles).isEmpty()
    }
}
