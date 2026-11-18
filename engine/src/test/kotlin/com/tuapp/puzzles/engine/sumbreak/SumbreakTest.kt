package com.tuapp.puzzles.engine.sumbreak

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.Json
import com.tuapp.puzzles.engine.LevelCodec
import com.tuapp.puzzles.engine.session.SumbreakSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SumbreakTest {

    private val generator = SumbreakGenerator()

    @Test
    fun rulesMergingIdenticalNumbers() {
        val n4 = SumbreakTile.Number(4)
        val n8 = SumbreakTile.Number(8)
        val empty = SumbreakTile.Empty

        assertEquals(SumbreakTile.Number(8), SumbreakRules.mergeResult(n4, n4))
        assertEquals(null, SumbreakRules.mergeResult(n4, n8))
        assertEquals(null, SumbreakRules.mergeResult(n4, empty))
    }

    @Test
    fun rulesModifiers() {
        val n6 = SumbreakTile.Number(6)
        val add3 = SumbreakTile.Modifier(ModifierOp.ADD, 3)
        val mul2 = SumbreakTile.Modifier(ModifierOp.MUL, 2)
        val wildcard = SumbreakTile.Modifier(ModifierOp.WILDCARD)

        assertEquals(SumbreakTile.Number(9), SumbreakRules.mergeResult(n6, add3))
        assertEquals(SumbreakTile.Number(9), SumbreakRules.mergeResult(add3, n6))
        assertEquals(SumbreakTile.Number(12), SumbreakRules.mergeResult(n6, mul2))
        assertEquals(SumbreakTile.Number(12), SumbreakRules.mergeResult(n6, wildcard))
    }

    @Test
    fun solverSolvesSimplePuzzle() {
        // 3x3 con 3 casillas: 2, 2, 4 -> (2+2) = 4, (4+4) = 8
        val tiles = MutableList<SumbreakTile>(9) { SumbreakTile.Empty }
        tiles[0] = SumbreakTile.Number(2)
        tiles[1] = SumbreakTile.Number(2)
        tiles[4] = SumbreakTile.Number(4)
        val puzzle = SumbreakPuzzle(3, tiles)

        val sol = SumbreakSolver.solve(puzzle)
        assertNotNull(sol)
        assertEquals(2, sol.size)
    }

    @Test
    fun generatorProducesSolvableLevelsForAllDifficulties() {
        for (d in 1..5) {
            val level = generator.generate(seed = 100L + d, difficulty = d)
            assertNotNull(level)
            assertTrue(level.puzzle.size in 3..5)
            val solution = SumbreakSolver.solve(level.puzzle)
            assertNotNull(solution, "Nivel de dificultad $d debe ser resoluble")
            assertTrue(solution.isNotEmpty(), "Nivel de dificultad $d debe tener movimientos")
        }
    }

    @Test
    fun roundTripThroughCodec() {
        val level = generator.generate(seed = 42L, difficulty = 2)
        val encoded = LevelCodec.encode(level)
        val json = Json.write(encoded)
        @Suppress("UNCHECKED_CAST")
        val decoded = LevelCodec.decode(GameType.SUMBREAK, Json.parse(json) as Map<String, Any?>) as SumbreakLevel

        assertEquals(level.seed, decoded.seed)
        assertEquals(level.difficulty, decoded.difficulty)
        assertEquals(level.puzzle.size, decoded.puzzle.size)
        assertEquals(level.puzzle.initialTiles, decoded.puzzle.initialTiles)
        assertEquals(level.targetNumber, decoded.targetNumber)
    }

    @Test
    fun sessionMakeMoveUndoAndHint() {
        val tiles = MutableList<SumbreakTile>(9) { SumbreakTile.Empty }
        tiles[0] = SumbreakTile.Number(4)
        tiles[1] = SumbreakTile.Number(4)
        val puzzle = SumbreakPuzzle(3, tiles)
        val level = SumbreakLevel(1L, 1, puzzle, 8, listOf(SumbreakMove(0, 1)))

        val session = SumbreakSession(level)
        assertFalse(session.isSolved)

        val hint = session.currentHint()
        assertNotNull(hint)
        assertTrue(hint is SumbreakHint.SuggestMove)

        // Aplicar pista
        assertTrue(session.applyHint(hint))
        assertEquals(SumbreakTile.Empty, session.state[0])
        assertEquals(SumbreakTile.Number(8), session.state[1])
        assertTrue(session.isSolved)

        // Deshacer
        assertTrue(session.undo())
        assertEquals(SumbreakTile.Number(4), session.state[0])
        assertEquals(SumbreakTile.Number(4), session.state[1])
        assertFalse(session.isSolved)
    }
}
