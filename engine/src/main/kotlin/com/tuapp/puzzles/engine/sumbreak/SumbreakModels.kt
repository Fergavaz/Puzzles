package com.tuapp.puzzles.engine.sumbreak

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel

enum class ModifierOp {
    ADD, SUB, MUL, WILDCARD;

    fun symbol(): String = when (this) {
        ADD -> "+"
        SUB -> "-"
        MUL -> "×"
        WILDCARD -> "★"
    }
}

sealed interface SumbreakTile {
    data object Empty : SumbreakTile {
        override fun toString(): String = "."
    }

    data class Number(val value: Int) : SumbreakTile {
        override fun toString(): String = value.toString()
    }

    data class Modifier(val op: ModifierOp, val value: Int = 0) : SumbreakTile {
        override fun toString(): String = when (op) {
            ModifierOp.ADD -> "+$value"
            ModifierOp.SUB -> "-$value"
            ModifierOp.MUL -> "×$value"
            ModifierOp.WILDCARD -> "★"
        }
    }
}

data class SumbreakMove(val from: Int, val to: Int)

data class SumbreakPuzzle(
    val size: Int,
    val initialTiles: List<SumbreakTile>,
)

data class SumbreakLevel(
    override val seed: Long,
    override val difficulty: Int,
    val puzzle: SumbreakPuzzle,
    val targetNumber: Int,
    val solutionMoves: List<SumbreakMove> = emptyList(),
) : PuzzleLevel {
    override val game: GameType get() = GameType.SUMBREAK
}
