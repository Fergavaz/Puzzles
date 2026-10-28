package com.tuapp.puzzles

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.tuapp.puzzles.engine.sudoku.SudokuGenerator
import com.tuapp.puzzles.engine.session.SudokuSession
import com.tuapp.puzzles.ui.boards.SudokuBoard
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import com.tuapp.puzzles.engine.patches.PatchesGenerator
import com.tuapp.puzzles.engine.queens.QueensGenerator
import com.tuapp.puzzles.engine.session.PatchesSession
import com.tuapp.puzzles.engine.session.QueensSession
import com.tuapp.puzzles.engine.session.TangoSession
import com.tuapp.puzzles.engine.session.ZipSession
import com.tuapp.puzzles.engine.tango.TangoGenerator
import com.tuapp.puzzles.engine.zip.ZipGenerator
import com.tuapp.puzzles.ui.boards.PatchesBoard
import com.tuapp.puzzles.ui.boards.QueensBoard
import com.tuapp.puzzles.ui.boards.SudokuBoard
import com.tuapp.puzzles.ui.boards.TangoBoard
import com.tuapp.puzzles.ui.boards.ZipBoard
import com.tuapp.puzzles.ui.theme.PuzzlesTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.junit.Rule
import org.junit.Test

/** Pruebas básicas de UI: tocar una celda cambia su estado y su descripción para lectores de pantalla. */
class BoardsSmokeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tangoCellCyclesOnTap() {
        val session = TangoSession(TangoGenerator().generate(1, 1))
        val free = session.state.indices.first { !session.isGiven(it) }
        val n = session.level.puzzle.size
        val desc = "Fila ${free / n + 1}, columna ${free % n + 1}: "
        rule.setContent {
            var rev by remember { mutableIntStateOf(0) }
            PuzzlesTheme { TangoBoard(session, rev, { it(); rev++ }) }
        }
        rule.onNodeWithContentDescription(desc + "vacía").performClick()
        rule.onNodeWithContentDescription(desc + "sol").assertContentDescriptionContains("sol", substring = true)
    }

    @Test fun queensCellCyclesToQueen() {
        val session = QueensSession(checkNotNull(QueensGenerator().generate(1, 1)))
        rule.setContent {
            var rev by remember { mutableIntStateOf(0) }
            PuzzlesTheme { QueensBoard(session, rev, { it(); rev++ }) }
        }
        val first = "Fila 1, columna 1, región "
        rule.onNode(androidx.compose.ui.test.hasContentDescription(first, substring = true)).performClick().performClick()
        assert(session.state[0] == QueensSession.QUEEN)
    }

    @Test fun sudokuCellSelectAndPaletteInput() {
        val session = SudokuSession(SudokuGenerator().generate(1, 1))
        val free = session.state.indices.first { !session.isGiven(it) }
        val n = 6
        val r = free / n + 1
        val c = free % n + 1
        rule.setContent {
            var rev by remember { mutableIntStateOf(0) }
            PuzzlesTheme { SudokuBoard(session, rev, { it(); rev++ }) }
        }
        rule.onNodeWithContentDescription("Fila $r, columna $c: vacía").performClick()
        val nodes = rule.onAllNodes(hasText("3") and hasClickAction())
        nodes[nodes.fetchSemanticsNodes().size - 1].performClick()
        rule.onNode(androidx.compose.ui.test.hasContentDescription("Fila $r, columna $c: 3", substring = true)).assertExists()
        assert(session.state[free] == 3)
    }

    @Test fun zipBoardRenders() {
        val session = ZipSession(ZipGenerator().generate(1, 1))
        rule.setContent {
            var rev by remember { mutableIntStateOf(0) }
            PuzzlesTheme { ZipBoard(session, rev, { it(); rev++ }) }
        }
        rule.onNode(androidx.compose.ui.test.hasContentDescription("Tablero Zip", substring = true)).assertExists()
    }

    @Test fun patchesBoardRenders() {
        val session = PatchesSession(checkNotNull(PatchesGenerator().generate(1, 1)))
        rule.setContent {
            var rev by remember { mutableIntStateOf(0) }
            PuzzlesTheme { PatchesBoard(session, rev, { it(); rev++ }) }
        }
        rule.onNode(androidx.compose.ui.test.hasContentDescription("Tablero Patches", substring = true)).assertExists()
    }
}
