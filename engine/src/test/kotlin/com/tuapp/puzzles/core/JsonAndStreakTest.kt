package com.tuapp.puzzles.core

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonAndStreakTest {
    @Test fun jsonRoundTrip() {
        val v = mapOf("a" to listOf(1L, 2L, null), "b" to "hola \"x\"\n\\", "c" to true, "d" to mapOf("e" to 2.5))
        assertEquals(v, Json.parse(Json.write(v)))
    }

    @Test fun jsonParsesNumbersAndWhitespace() {
        val p = Json.parse(" { \"x\" : [ -3 , 4.5e1 , 0 ] , \"u\":\"\\u00e1\" } ") as Map<*, *>
        assertEquals(listOf(-3L, 45.0, 0L), p["x"])
        assertEquals("á", p["u"])
    }

    @Test fun streakGrowsOnConsecutiveDaysAndResets() {
        assertEquals(1, Streak.afterSolve(null, 0, 100))
        assertEquals(2, Streak.afterSolve(100, 1, 101))
        assertEquals(2, Streak.afterSolve(101, 2, 101))
        assertEquals(1, Streak.afterSolve(101, 2, 105))
    }

    @Test fun staleStreakReadsAsZero() {
        assertEquals(3, Streak.current(100, 3, 101))
        assertEquals(0, Streak.current(100, 3, 102))
        assertEquals(0, Streak.current(null, 0, 102))
    }
}
