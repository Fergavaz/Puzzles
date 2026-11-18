package com.tuapp.puzzles.engine

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel
import com.tuapp.puzzles.engine.patches.PatchesGenerator
import com.tuapp.puzzles.engine.queens.QueensGenerator
import com.tuapp.puzzles.engine.sudoku.SudokuGenerator
import com.tuapp.puzzles.engine.sumbreak.SumbreakGenerator
import com.tuapp.puzzles.engine.tango.TangoGenerator
import com.tuapp.puzzles.engine.zip.ZipGenerator

/** Punto de entrada único: misma semilla + dificultad => mismo nivel. null = el generador agotó intentos. */
object Generators {
    fun generate(game: GameType, seed: Long, difficulty: Int): PuzzleLevel? = when (game) {
        GameType.ZIP -> ZipGenerator().generate(seed, difficulty)
        GameType.PATCHES -> PatchesGenerator().generate(seed, difficulty)
        GameType.SUDOKU -> SudokuGenerator().generate(seed, difficulty)
        GameType.TANGO -> TangoGenerator().generate(seed, difficulty)
        GameType.QUEENS -> QueensGenerator().generate(seed, difficulty)
        GameType.SUMBREAK -> SumbreakGenerator().generate(seed, difficulty)
    }
}
