package com.tuapp.puzzles.engine.sumbreak

sealed interface SumbreakHint {
    val explanation: String

    data class DeadEnd(
        override val explanation: String = "¡Bloqueo! No hay movimientos válidos hacia la victoria. Pulsa Deshacer para retroceder.",
    ) : SumbreakHint

    data class SuggestMove(
        val move: SumbreakMove,
        val resultTile: SumbreakTile,
        override val explanation: String,
    ) : SumbreakHint
}

object SumbreakHintEngine {

    fun nextHint(size: Int, tiles: List<SumbreakTile>): SumbreakHint? {
        if (SumbreakRules.isSolved(tiles)) return null

        val winningMoves = SumbreakSolver.findWinningMovesFrom(size, tiles)
        if (winningMoves == null || winningMoves.isEmpty()) {
            return SumbreakHint.DeadEnd()
        }

        val move = winningMoves.first()
        val fromTile = tiles[move.from]
        val toTile = tiles[move.to]
        val result = SumbreakRules.mergeResult(fromTile, toTile) ?: return SumbreakHint.DeadEnd()

        val text = when {
            fromTile is SumbreakTile.Number && toTile is SumbreakTile.Number ->
                "Fusiona el ${fromTile.value} con el ${toTile.value} para obtener ${(result as SumbreakTile.Number).value}."
            fromTile is SumbreakTile.Number && toTile is SumbreakTile.Modifier ->
                "Aplica el modificador $toTile al número ${fromTile.value} para obtener ${(result as SumbreakTile.Number).value}."
            fromTile is SumbreakTile.Modifier && toTile is SumbreakTile.Number ->
                "Aplica el modificador $fromTile al número ${toTile.value} para obtener ${(result as SumbreakTile.Number).value}."
            else -> "Realiza esta fusión para avanzar hacia la solución."
        }

        return SumbreakHint.SuggestMove(move, result, text)
    }
}
