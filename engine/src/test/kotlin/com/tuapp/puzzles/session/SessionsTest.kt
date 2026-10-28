package com.tuapp.puzzles.engine.session

import com.tuapp.puzzles.core.CampaignLevel
import com.tuapp.puzzles.core.CampaignProgress
import com.tuapp.puzzles.engine.patches.*
import com.tuapp.puzzles.engine.queens.*
import com.tuapp.puzzles.engine.sudoku.*
import com.tuapp.puzzles.engine.tango.*
import com.tuapp.puzzles.engine.zip.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionsTest {
    // ---------- Tango ----------
    private val tango = TangoGenerator().generate(1, 3)

    @Test fun tangoTapCyclesAndGivensAreLocked() {
        val s = TangoSession(tango)
        val free = s.state.indices.first { !s.isGiven(it) }
        s.tap(free); assertEquals(SUN, s.state[free])
        s.tap(free); assertEquals(MOON, s.state[free])
        s.tap(free); assertEquals(EMPTY, s.state[free])
        s.state.indices.firstOrNull { s.isGiven(it) }?.let { assertFalse(s.tap(it)) }
    }

    @Test fun tangoUndoAndHintAndSolve() {
        val s = TangoSession(tango)
        val free = s.state.indices.first { !s.isGiven(it) }
        s.tap(free); assertTrue(s.undo()); assertEquals(EMPTY, s.state[free]); assertFalse(s.undo())
        while (!s.isSolved) assertNotNull(s.hint())
        assertEquals(tango.solution, s.state)
    }

    // ---------- Queens ----------
    private val queens = assertNotNull(QueensGenerator().generate(2, 2))

    @Test fun queensCycleXThenQueenThenEmpty() {
        val s = QueensSession(queens)
        s.tap(0); assertEquals(QueensSession.MARK, s.state[0])
        s.tap(0); assertEquals(QueensSession.QUEEN, s.state[0])
        s.tap(0); assertEquals(QueensSession.EMPTY, s.state[0])
    }

    @Test fun queensAutoMarkXOnQueenPlacement() {
        val s = QueensSession(queens)
        val n = queens.puzzle.size
        s.tap(0, autoMark = true) // Empty -> Mark
        assertEquals(QueensSession.MARK, s.state[0])
        s.tap(0, autoMark = true) // Mark -> Queen with autoMark
        assertEquals(QueensSession.QUEEN, s.state[0])
        // Same row and same col should be marked as MARK
        assertEquals(QueensSession.MARK, s.state[1])
        assertEquals(QueensSession.MARK, s.state[n])
    }

    @Test fun queensConflictsHighlightedAndSolvedByHints() {
        val s = QueensSession(queens)
        val n = queens.puzzle.size
        s.tap(0); s.tap(0); s.tap(1); s.tap(1)   // dos reinas en la misma fila
        assertEquals(setOf(0, 1), s.conflicts)
        s.clear()
        repeat(n) { assertNotNull(s.hint()) }
        assertTrue(s.isSolved)
    }

    // ---------- Sudoku ----------
    private val sudoku = SudokuGenerator().generate(3, 2)

    @Test fun sudokuEnterEraseAndGivensLocked() {
        val s = SudokuSession(sudoku)
        val given = s.state.indices.first { s.isGiven(it) }
        s.select(given); assertFalse(s.enter(1))
        val free = s.state.indices.first { !s.isGiven(it) }
        s.select(free); assertTrue(s.enter(4)); assertEquals(4, s.state[free])
        assertTrue(s.enter(0)); assertEquals(0, s.state[free])
        assertFalse(s.enter(7))
    }

    @Test fun sudokuHintFixesWrongValueThenSolves() {
        val s = SudokuSession(sudoku)
        val free = s.state.indices.first { !s.isGiven(it) }
        s.select(free); s.enter(if (sudoku.solution[free] == 1) 2 else 1)   // valor erróneo
        assertEquals(free to sudoku.solution[free], s.hint())
        while (!s.isSolved) assertNotNull(s.hint())
    }

    // ---------- Zip ----------
    private val zip = ZipGenerator().generate(4, 3)

    @Test fun zipDragFollowsSolutionAndSolves() {
        val s = ZipSession(zip)
        s.beginStroke()
        assertFalse(s.drag(zip.solution[1]))              // no se puede empezar fuera del 1
        zip.solution.forEach { assertTrue(s.drag(it)) }
        s.endStroke()
        assertTrue(s.isSolved)
        assertTrue(s.undo()); assertTrue(s.state.isEmpty())
    }

    @Test fun zipBacktrackWhenReturningOverOwnSteps() {
        val s = ZipSession(zip)
        s.beginStroke()
        zip.solution.take(4).forEach { s.drag(it) }
        assertTrue(s.drag(zip.solution[1]))
        assertEquals(zip.solution.take(2), s.state)
    }

    @Test fun zipHintExtendsCorrectPrefix() {
        val s = ZipSession(zip)
        assertEquals(zip.solution[0], s.hint())
        assertEquals(zip.solution[1], s.hint())
        assertEquals(zip.solution.take(2), s.state)
    }

    // ---------- Patches ----------
    private val patches = assertNotNull(PatchesGenerator().generate(5, 2))

    @Test fun patchesPreviewCommitAndDiscard() {
        val s = PatchesSession(patches)
        val ci = patches.solution.indices.first { patches.solution[it].area > 1 }
        val sol = patches.solution[ci]
        assertTrue(s.preview(ci, sol).valid)
        assertEquals(sol.area, s.preview(ci, sol).cells)
        val initialSize = s.state.size
        assertTrue(s.commit(ci, sol))
        val bad = Rect(sol.row, sol.col, sol.height + 1, sol.width + 1)
        assertFalse(s.commit(ci, bad.copy(row = 0, col = 0, height = patches.puzzle.size, width = patches.puzzle.size)))
        assertEquals(initialSize + 1, s.state.size)
        assertTrue(s.undo()); assertEquals(initialSize, s.state.size)
    }

    @Test fun patchesHintsSolveAndPlacedPatchCanBeRemoved() {
        val s = PatchesSession(patches)
        while (!s.isSolved) assertNotNull(s.hint())
        assertTrue(s.isSolved)
        val non1x1 = patches.solution.indices.first { patches.solution[it].area > 1 }
        val cell = patches.puzzle.clues[non1x1].cell
        val pl = assertNotNull(s.placementAt(cell))
        assertTrue(s.remove(pl.clueIndex)); assertFalse(s.isSolved)
    }

    @Test fun patchesRectDrawnWithoutChoosingTheClue() {
        val s = PatchesSession(patches)
        val sol = patches.solution[1]
        assertEquals(listOf(1), s.cluesIn(sol))
        assertTrue(s.previewRect(sol).valid)
        assertTrue(s.commitRect(sol))
        val whole = Rect(0, 0, patches.puzzle.size, patches.puzzle.size)
        assertEquals(RectCheck.CONTAINS_OTHER_CLUE, s.previewRect(whole).check)
        assertFalse(s.commitRect(whole))
    }

    // ---------- Campaña en juego libre ----------
    @Test fun campaignHelpersNeverLockAndSuggestNext() {
        val levels = List(6) { CampaignLevel("zip-0${it + 1}", it, ZipGenerator().generate(it.toLong(), 1 + it / 2)) }
        val done = setOf("zip-01", "zip-02", "zip-04")
        assertEquals(3, CampaignProgress.completedCount(levels, done))
        assertEquals("zip-03", CampaignProgress.nextToPlay(levels, done)?.id)
        assertEquals("zip-05", CampaignProgress.nextInDifficulty(levels, done, 3)?.id)
        assertEquals(mapOf(1 to (2 to 2), 2 to (1 to 2), 3 to (0 to 2)), CampaignProgress.difficultyStats(levels, done))
    }
}
