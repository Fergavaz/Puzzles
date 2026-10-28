package com.tuapp.puzzles

import android.util.Log
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.engine.Generators
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class GenerationBenchmarkTest {

    @Test
    fun benchmarkGenerationTimesOnEmulator() {
        val games = GameType.entries
        val difficulties = listOf(1, 3, 5)
        val seeds = listOf(101L, 202L, 303L, 404L, 505L)

        val report = StringBuilder("\n=== BENCHMARK DE GENERACIÓN EN EMULADOR ===\n")
        report.append(String.format("%-12s | %-10s | %-10s | %-10s\n", "Juego", "Dificultad", "Media (ms)", "Máx (ms)"))
        report.append("--------------------------------------------------\n")

        for (game in games) {
            for (diff in difficulties) {
                val times = mutableListOf<Long>()
                for (seed in seeds) {
                    val time = measureTimeMillis {
                        val level = Generators.generate(game, seed, diff)
                        // Verify non-null or fallback
                    }
                    times.add(time)
                }
                val avg = times.average()
                val max = times.maxOrNull() ?: 0L
                report.append(String.format("%-12s | %-10d | %-10.1f | %-10d\n", game.name, diff, avg, max))
                Log.i("GENERATION_BENCHMARK", "${game.name} d=$diff: avg=${avg}ms, max=${max}ms")
                // Target: < 3000 ms
                assertTrue("${game.name} dificultad $diff tardó más de 3 s ($max ms)", max < 3000L)
            }
        }
        report.append("==================================================\n")
        println(report.toString())
        Log.i("GENERATION_BENCHMARK", report.toString())
    }
}
