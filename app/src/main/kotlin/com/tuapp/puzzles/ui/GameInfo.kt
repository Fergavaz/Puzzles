package com.tuapp.puzzles.ui

import com.tuapp.puzzles.core.GameType

val GameType.title: String
    get() = when (this) {
        GameType.ZIP -> "Zip"
        GameType.PATCHES -> "Patches"
        GameType.SUDOKU -> "Mini Sudoku"
        GameType.TANGO -> "Tango"
        GameType.QUEENS -> "Queens"
        GameType.SUMBREAK -> "Sumbreak"
    }

val GameType.emoji: String
    get() = when (this) {
        GameType.ZIP -> "➰"
        GameType.PATCHES -> "🟦"
        GameType.SUDOKU -> "🔢"
        GameType.TANGO -> "☀️"
        GameType.QUEENS -> "👑"
        GameType.SUMBREAK -> "➕"
    }

fun difficultyLabel(d: Int): String =
    listOf("Muy fácil", "Fácil", "Media", "Difícil", "Experto")[(d - 1).coerceIn(0, 4)]

fun formatTime(ms: Long): String = "%d:%02d".format(ms / 60_000, (ms / 1000) % 60)
