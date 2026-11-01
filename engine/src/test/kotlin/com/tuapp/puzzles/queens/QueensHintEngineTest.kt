package com.tuapp.puzzles.engine.queens

import com.tuapp.puzzles.engine.session.QueensSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class QueensHintEngineTest {
    private val gen = QueensGenerator()

    // 1 & 2: Sonoridad y Termina (para 30 niveles generados, dificultades 1–5)
    @Test
    fun testSoundnessAndTerminationAcross30Levels() {
        var count = 0
        for (d in 1..5) {
            for (i in 1..6) {
                val seed = (d * 100 + i).toLong()
                val level = checkNotNull(gen.generate(seed, d)) { "No se pudo generar nivel d=$d seed=$seed" }
                val n = level.puzzle.size
                val solQueens = (0 until n).map { r -> r * n + level.solution[r] }.toSet()
                val session = QueensSession(level)

                var steps = 0
                val maxSteps = n * n * 2
                while (!session.isSolved && steps < maxSteps) {
                    val hint = QueensHintEngine.nextHint(level, session.state)
                    assertNotNull(hint, "Debe haber una pista disponible si no está resuelto (d=$d, seed=$seed, paso=$steps)")

                    when (hint) {
                        is QueensHint.PlaceQueen -> {
                            assertTrue(
                                hint.cell in solQueens,
                                "PlaceQueen debe coincidir con la solución: celda ${hint.cell} (d=$d, seed=$seed)"
                            )
                        }
                        is QueensHint.Eliminate -> {
                            for (c in hint.cells) {
                                assertFalse(
                                    c in solQueens,
                                    "Eliminate NO debe contener una celda de la solución: celda $c (d=$d, seed=$seed)"
                                )
                                assertTrue(
                                    session.state[c] != QueensSession.MARK,
                                    "Eliminate solo debe incluir celdas sin X (d=$d, seed=$seed)"
                                )
                            }
                        }
                        is QueensHint.Wrong -> {
                            fail("No debería haber colocaciones erróneas al aplicar pistas válidas (d=$d, seed=$seed)")
                        }
                    }

                    session.applyHint(hint, autoMark = false)
                    steps++
                }

                assertTrue(session.isSolved, "El puzzle debe quedar resuelto en pasos finitos (pasos=$steps, d=$d, seed=$seed)")
                count++
            }
        }
        assertEquals(30, count)
    }

    // 3: Errores primero (WRONG_QUEEN y WRONG_MARK)
    @Test
    fun testErrorsFirst() {
        val level = checkNotNull(gen.generate(seed = 42L, difficulty = 2))
        val n = level.puzzle.size
        val session = QueensSession(level)

        // Colocar una reina incorrecta
        val wrongCol = (level.solution[0] + 1) % n
        val wrongQueenCell = wrongCol
        val stateWithWrongQueen = session.state.toMutableList()
        stateWithWrongQueen[wrongQueenCell] = QueensSession.QUEEN

        val hint1 = QueensHintEngine.nextHint(level, stateWithWrongQueen)
        assertNotNull(hint1)
        assertTrue(hint1 is QueensHint.Wrong)
        assertEquals(QueensTechnique.WRONG_QUEEN, hint1.technique)
        assertTrue(wrongQueenCell in hint1.cells)

        // Colocar una X sobre la solución
        val solCell = level.solution[0]
        val stateWithWrongMark = session.state.toMutableList()
        stateWithWrongMark[solCell] = QueensSession.MARK

        val hint2 = QueensHintEngine.nextHint(level, stateWithWrongMark)
        assertNotNull(hint2)
        assertTrue(hint2 is QueensHint.Wrong)
        assertEquals(QueensTechnique.WRONG_MARK, hint2.technique)
        assertTrue(solCell in hint2.cells)
    }

    // 4: Cada técnica tiene un caso (probar T1, T2, T3, T4 y T5 con comprobación de técnica, causa y unidades)
    @Test
    fun testEachTechniqueHasACase() {
        // En los tableros generados, las 5 técnicas aparecen de forma natural:
        val foundTechniques = mutableSetOf<QueensTechnique>()
        for (d in 1..5) {
            for (s in 1..10) {
                val level = gen.generate((d * 100 + s).toLong(), d) ?: continue
                val session = QueensSession(level)
                while (!session.isSolved) {
                    val hint = QueensHintEngine.nextHint(level, session.state) ?: break
                    foundTechniques.add(hint.technique)
                    assertTrue(hint.cause.isNotEmpty(), "La causa de ${hint.technique} no debe estar vacía")
                    if (hint !is QueensHint.Wrong) {
                        assertTrue(hint.units.isNotEmpty(), "Las unidades de ${hint.technique} no deben estar vacías")
                    }
                    session.applyHint(hint, autoMark = false)
                }
            }
        }

        assertTrue(foundTechniques.contains(QueensTechnique.GIVEAWAY), "Debe haber casos de GIVEAWAY")
        assertTrue(foundTechniques.contains(QueensTechnique.KNOCKOUT), "Debe haber casos de KNOCKOUT")
        assertTrue(foundTechniques.contains(QueensTechnique.CONFINED), "Debe haber casos de CONFINED")
        assertTrue(foundTechniques.contains(QueensTechnique.N_IN_N), "Debe haber casos de N_IN_N")
        assertTrue(foundTechniques.contains(QueensTechnique.HYPOTHETICAL), "Debe haber casos de HYPOTHETICAL")

        // Verificamos además un caso unitario construido de GIVEAWAY:
        val p1 = QueensPuzzle(4, listOf(0, 0, 1, 1, 0, 0, 1, 1, 2, 2, 3, 3, 2, 2, 3, 3))
        val l1 = QueensLevel(1L, 1, p1, listOf(1, 3, 0, 2))
        val s1 = MutableList(16) { QueensSession.EMPTY }
        s1[7] = QueensSession.QUEEN
        s1[8] = QueensSession.QUEEN
        s1[14] = QueensSession.QUEEN
        val h1 = QueensHintEngine.nextHint(l1, s1)
        assertNotNull(h1)
        assertEquals(QueensTechnique.GIVEAWAY, h1.technique)
        assertEquals(setOf(1), h1.cause)
        assertEquals(listOf(UnitRef(UnitKind.REGION, 0)), h1.units)
    }

    // 5: Utilidad (Eliminate nunca incluye celdas que ya tienen X)
    @Test
    fun testEliminateUtilityDoesNotIncludeExistingMarks() {
        val level = checkNotNull(gen.generate(seed = 101L, difficulty = 3))
        val session = QueensSession(level)
        while (!session.isSolved) {
            val hint = QueensHintEngine.nextHint(level, session.state) ?: break
            if (hint is QueensHint.Eliminate) {
                for (c in hint.cells) {
                    assertFalse(session.state[c] == QueensSession.MARK, "No debe incluir celdas que ya tienen X")
                }
            }
            session.applyHint(hint, autoMark = false)
        }
    }

    // 6: Determinismo (mismo tablero -> mismo hint)
    @Test
    fun testDeterminism() {
        val level = checkNotNull(gen.generate(seed = 77L, difficulty = 3))
        val state = List(level.puzzle.size * level.puzzle.size) { QueensSession.EMPTY }
        val hintA = QueensHintEngine.nextHint(level, state)
        val hintB = QueensHintEngine.nextHint(level, state)
        assertEquals(hintA, hintB)
    }

    // 7: Resuelto (tablero completo y correcto -> null)
    @Test
    fun testSolvedBoardReturnsNull() {
        val level = checkNotNull(gen.generate(seed = 55L, difficulty = 2))
        val n = level.puzzle.size
        val solvedState = MutableList(n * n) { QueensSession.EMPTY }
        for (r in 0 until n) {
            solvedState[r * n + level.solution[r]] = QueensSession.QUEEN
        }
        val hint = QueensHintEngine.nextHint(level, solvedState)
        assertNull(hint, "Tablero completo y correcto debe devolver null")
    }

    // 8: Estadística (no falla, informa porcentaje por técnica)
    @Test
    fun testTechniqueUsageStatistics() {
        val stats = mutableMapOf<QueensTechnique, Int>()
        var totalSteps = 0

        for (d in 1..5) {
            val diffStats = mutableMapOf<QueensTechnique, Int>()
            var diffSteps = 0
            for (i in 1..4) {
                val level = gen.generate((d * 1000 + i).toLong(), d) ?: continue
                val session = QueensSession(level)
                while (!session.isSolved) {
                    val hint = QueensHintEngine.nextHint(level, session.state) ?: break
                    stats[hint.technique] = (stats[hint.technique] ?: 0) + 1
                    diffStats[hint.technique] = (diffStats[hint.technique] ?: 0) + 1
                    totalSteps++
                    diffSteps++
                    session.applyHint(hint, autoMark = false)
                }
            }
            if (diffSteps > 0) {
                val breakdown = diffStats.entries.joinToString(", ") { "${it.key}: ${it.value * 100 / diffSteps}%" }
                println("Estadísticas Queens Dif $d ($diffSteps pasos): $breakdown")
            }
        }
        if (totalSteps > 0) {
            val globalBreakdown = stats.entries.joinToString(", ") { "${it.key}: ${it.value * 100 / totalSteps}%" }
            println("Estadísticas Queens Global ($totalSteps pasos): $globalBreakdown")
        }
    }

    // 9: Rendimiento (nextHint < 20 ms en 10x10)
    @Test
    fun testPerformanceNextHint() {
        val level = checkNotNull(gen.generate(seed = 999L, difficulty = 5))
        val session = QueensSession(level)
        // Calentamiento JIT
        repeat(5) { QueensHintEngine.nextHint(level, session.state) }

        val start = System.nanoTime()
        val hint = QueensHintEngine.nextHint(level, session.state)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        assertNotNull(hint)
        println("nextHint 10x10 tardó: $elapsedMs ms")
        assertTrue(elapsedMs < 20.0, "nextHint tardó $elapsedMs ms, debe ser < 20 ms")
    }
}
