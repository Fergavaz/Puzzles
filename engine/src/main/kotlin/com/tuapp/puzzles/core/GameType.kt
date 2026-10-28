package com.tuapp.puzzles.core

enum class GameType(val id: String) {
    ZIP("zip"), PATCHES("patches"), SUDOKU("sudoku"), TANGO("tango"), QUEENS("queens");

    companion object {
        fun fromId(id: String): GameType = entries.first { it.id == id }
    }
}

/** Nivel de cualquiera de los cinco juegos (semilla + dificultad lo hacen reproducible). */
interface PuzzleLevel {
    val seed: Long
    val difficulty: Int
    val game: GameType
}

/** Nivel de la campaña: id estable ("zip-01"), posición y contenido. */
data class CampaignLevel(val id: String, val index: Int, val level: PuzzleLevel)
