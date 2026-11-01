package com.tuapp.puzzles.engine.tango

import com.tuapp.puzzles.engine.session.TangoSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class TangoHintEngineTest {
    private val gen = TangoGenerator(6)

    // 1 & 2: Sonoridad y Termina (30 niveles, dificultades 1–5)
    @Test
    fun testSoundnessAndTerminationAcross30Levels() {
        var count = 0
        for (d in 1..5) {
            for (i in 1..6) {
                val seed = (d * 200 + i).toLong()
                val level = gen.generate(seed, d)
                val session = TangoSession(level)

                var steps = 0
                val maxSteps = level.puzzle.size * level.puzzle.size * 2
                while (!session.isSolved && steps < maxSteps) {
                    val hint = TangoHintEngine.nextHint(level, session.state)
                    assertNotNull(hint, "Debe haber una pista disponible si no está resuelto (d=$d, seed=$seed, paso=$steps)")

                    when (hint) {
                        is TangoHint.Place -> {
                            assertEquals(
                                level.solution[hint.cell],
                                hint.symbol,
                                "El símbolo deducido debe coincidir con la solución: celda ${hint.cell} (d=$d, seed=$seed)"
                            )
                            assertEquals(
                                EMPTY,
                                session.state[hint.cell],
                                "La celda objetivo de la pista debe estar vacía: celda ${hint.cell}"
                            )
                        }
                        is TangoHint.Wrong -> {
                            fail("No debería haber errores al aplicar pistas válidas (d=$d, seed=$seed)")
                        }
                    }

                    session.applyHint(hint)
                    steps++
                }

                assertTrue(session.isSolved, "El puzzle debe quedar resuelto en pasos finitos (pasos=$steps, d=$d, seed=$seed)")
                assertNull(TangoHintEngine.nextHint(level, session.state), "Tablero resuelto debe devolver null")
                count++
            }
        }
        assertEquals(30, count)
    }

    // 3: Errores primero (WRONG_CELL)
    @Test
    fun testErrorsFirst() {
        val level = gen.generate(seed = 123L, difficulty = 2)
        val nonGiven = level.puzzle.givens.indices.first { level.puzzle.givens[it] == EMPTY }
        val session = TangoSession(level)

        // Colocar símbolo erróneo en una celda no fija
        val wrongSymbol = if (level.solution[nonGiven] == SUN) MOON else SUN
        val wrongState = session.state.toMutableList()
        wrongState[nonGiven] = wrongSymbol

        val hint = TangoHintEngine.nextHint(level, wrongState)
        assertNotNull(hint)
        assertTrue(hint is TangoHint.Wrong, "Debe devolver Wrong si hay colocaciones incorrectas")
        assertEquals(TangoTechnique.WRONG_CELL, hint.technique)
        assertTrue(nonGiven in hint.cells)

        // Las celdas fijas nunca deben aparecer en Wrong
        for (c in hint.cells) {
            assertEquals(EMPTY, level.puzzle.givens[c], "Una celda fija nunca debe aparecer como errónea")
        }
    }

    // 4: Un caso por técnica (SIGN, PAIR, SANDWICH, COUNT, LINE_CHECK, HYPOTHETICAL)
    @Test
    fun testOneCasePerTechnique() {
        val n = 6
        val emptyGivens = List(n * n) { EMPTY }

        // --- Caso T1 · SIGN ---
        val c1 = TangoConstraint(a = 0, b = 1, equal = true)
        val p1 = TangoPuzzle(n, emptyGivens, listOf(c1))
        val l1 = TangoLevel(1L, 1, p1, List(n * n) { SUN })
        val s1 = MutableList(n * n) { EMPTY }
        s1[0] = SUN // Celda 0 tiene SUN, celda 1 está vacía con relación =
        val h1 = TangoHintEngine.nextHint(l1, s1)
        assertNotNull(h1)
        assertTrue(h1 is TangoHint.Place)
        assertEquals(TangoTechnique.SIGN, h1.technique)
        assertEquals(1, h1.cell)
        assertEquals(SUN, h1.symbol)
        assertEquals(setOf(0, 1), h1.cause)
        assertEquals(c1, h1.constraint)

        // --- Caso T2 · PAIR ---
        val p2 = TangoPuzzle(n, emptyGivens, emptyList())
        val l2 = TangoLevel(2L, 1, p2, List(n * n) { SUN })
        val s2 = MutableList(n * n) { EMPTY }
        s2[1] = SUN
        s2[2] = SUN
        // Celdas contiguas 1 y 2 con SUN -> extremo 0 debe ser MOON
        val h2 = TangoHintEngine.nextHint(l2, s2)
        assertNotNull(h2)
        assertTrue(h2 is TangoHint.Place)
        assertEquals(TangoTechnique.PAIR, h2.technique)
        assertEquals(0, h2.cell)
        assertEquals(MOON, h2.symbol)
        assertEquals(setOf(1, 2), h2.cause)
        assertEquals(listOf(LineRef(LineKind.ROW, 0)), h2.lines)

        // --- Caso T3 · SANDWICH ---
        val p3 = TangoPuzzle(n, emptyGivens, emptyList())
        val l3 = TangoLevel(3L, 1, p3, List(n * n) { SUN })
        val s3 = MutableList(n * n) { EMPTY }
        s3[0] = SUN
        s3[2] = SUN
        // Patrón SUN _ SUN en celdas 0, 1, 2 -> central 1 debe ser MOON
        val h3 = TangoHintEngine.nextHint(l3, s3)
        assertNotNull(h3)
        assertTrue(h3 is TangoHint.Place)
        assertEquals(TangoTechnique.SANDWICH, h3.technique)
        assertEquals(1, h3.cell)
        assertEquals(MOON, h3.symbol)
        assertEquals(setOf(0, 2), h3.cause)
        assertEquals(listOf(LineRef(LineKind.ROW, 0)), h3.lines)

        // --- Caso T4 · COUNT ---
        val p4 = TangoPuzzle(n, emptyGivens, emptyList())
        val sol4 = MutableList(n * n) { SUN }
        sol4[0] = MOON; sol4[1] = SUN; sol4[2] = SUN; sol4[3] = MOON; sol4[4] = SUN; sol4[5] = MOON
        val l4 = TangoLevel(4L, 1, p4, sol4)
        val s4 = MutableList(n * n) { EMPTY }
        // En fila 0: M, S, _, M, S, M (sin pares ni sándwiches vacíos; 3 lunas)
        s4[0] = MOON
        s4[1] = SUN
        s4[3] = MOON
        s4[4] = SUN
        s4[5] = MOON
        // Ya hay 3 MOON en fila 0 -> celda 2 debe ser SUN por COUNT
        val h4 = TangoHintEngine.nextHint(l4, s4)
        assertNotNull(h4)
        assertTrue(h4 is TangoHint.Place)
        assertEquals(TangoTechnique.COUNT, h4.technique)
        assertEquals(2, h4.cell)
        assertEquals(SUN, h4.symbol)
        assertEquals(listOf(LineRef(LineKind.ROW, 0)), h4.lines)

        // --- Caso T5 · LINE_CHECK o casos en niveles reales ---
        val foundTechniques = mutableSetOf<TangoTechnique>()
        for (d in 1..5) {
            for (s in 1..10) {
                val level = gen.generate((d * 50 + s).toLong(), d)
                val session = TangoSession(level)
                while (!session.isSolved) {
                    val hint = TangoHintEngine.nextHint(level, session.state) ?: break
                    foundTechniques.add(hint.technique)
                    session.applyHint(hint)
                }
            }
        }
        assertTrue(foundTechniques.contains(TangoTechnique.SIGN))
        assertTrue(foundTechniques.contains(TangoTechnique.PAIR))
        assertTrue(foundTechniques.contains(TangoTechnique.SANDWICH))
        assertTrue(foundTechniques.contains(TangoTechnique.COUNT))
    }

    // 5: Prioridad (la técnica más simple gana)
    @Test
    fun testPrioritySimpleOverComplex() {
        val n = 6
        val emptyGivens = List(n * n) { EMPTY }
        // Tablero donde aplican SIGN (T1) y PAIR (T2):
        // Celdas 1 y 2 tienen SUN (PAIR sobre celda 0).
        // Y relación celdas 4 y 5 con = donde celda 4 tiene SUN (SIGN sobre celda 5).
        val c = TangoConstraint(a = 4, b = 5, equal = true)
        val p = TangoPuzzle(n, emptyGivens, listOf(c))
        val l = TangoLevel(10L, 1, p, List(n * n) { SUN })
        val s = MutableList(n * n) { EMPTY }
        s[1] = SUN
        s[2] = SUN
        s[4] = SUN

        val hint = TangoHintEngine.nextHint(l, s)
        assertNotNull(hint)
        // SIGN (T1) tiene mayor prioridad que PAIR (T2)
        assertEquals(TangoTechnique.SIGN, hint.technique)
    }

    // 6: Determinismo (mismo tablero -> mismo hint)
    @Test
    fun testDeterminism() {
        val level = gen.generate(seed = 99L, difficulty = 3)
        val state = level.puzzle.givens
        val hint1 = TangoHintEngine.nextHint(level, state)
        val hint2 = TangoHintEngine.nextHint(level, state)
        assertEquals(hint1, hint2)
    }

    // 7: Resuelto (tablero completo y correcto -> null)
    @Test
    fun testSolvedReturnsNull() {
        val level = gen.generate(seed = 88L, difficulty = 1)
        val hint = TangoHintEngine.nextHint(level, level.solution)
        assertNull(hint, "Tablero completo y resuelto debe devolver null")
    }

    // 8: Estadística (informa porcentajes por técnica)
    @Test
    fun testTechniqueUsageStatistics() {
        val stats = mutableMapOf<TangoTechnique, Int>()
        var totalSteps = 0

        for (d in 1..5) {
            val diffStats = mutableMapOf<TangoTechnique, Int>()
            var diffSteps = 0
            for (i in 1..4) {
                val level = gen.generate((d * 1000 + i).toLong(), d)
                val session = TangoSession(level)
                while (!session.isSolved) {
                    val hint = TangoHintEngine.nextHint(level, session.state) ?: break
                    stats[hint.technique] = (stats[hint.technique] ?: 0) + 1
                    diffStats[hint.technique] = (diffStats[hint.technique] ?: 0) + 1
                    totalSteps++
                    diffSteps++
                    session.applyHint(hint)
                }
            }
            if (diffSteps > 0) {
                val breakdown = diffStats.entries.joinToString(", ") { "${it.key}: ${it.value * 100 / diffSteps}%" }
                println("Estadísticas Tango Dif $d ($diffSteps pasos): $breakdown")
            }
        }
        if (totalSteps > 0) {
            val globalBreakdown = stats.entries.joinToString(", ") { "${it.key}: ${it.value * 100 / totalSteps}%" }
            println("Estadísticas Tango Global ($totalSteps pasos): $globalBreakdown")
        }
    }

    // 9: Rendimiento (< 10 ms en 6x6)
    @Test
    fun testPerformanceNextHint() {
        val level = gen.generate(seed = 777L, difficulty = 4)
        val session = TangoSession(level)
        repeat(5) { TangoHintEngine.nextHint(level, session.state) }

        val start = System.nanoTime()
        val hint = TangoHintEngine.nextHint(level, session.state)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        assertNotNull(hint)
        println("nextHint Tango 6x6 tardó: $elapsedMs ms")
        assertTrue(elapsedMs < 10.0, "nextHint tardó $elapsedMs ms, debe ser < 10 ms")
    }
}
