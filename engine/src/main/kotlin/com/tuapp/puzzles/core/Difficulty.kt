package com.tuapp.puzzles.core

/** Dificultad común a todos los juegos: 1 (fácil) a 5 (experto). */
object Difficulty {
    const val MIN = 1
    const val MAX = 5
    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)
}

/** Semilla por defecto cuando el usuario no aporta ninguna. */
fun defaultSeed(): Long = System.currentTimeMillis()
