package com.tuapp.puzzles.core

/** Racha de días consecutivos con al menos un puzzle resuelto (días como epochDay). */
object Streak {
    fun afterSolve(lastDay: Long?, streak: Int, today: Long): Int = when {
        lastDay == null -> 1
        today == lastDay -> maxOf(streak, 1)
        today == lastDay + 1 -> streak + 1
        else -> 1
    }

    /** Racha vigente: si ya pasó más de un día sin jugar, se ha perdido. */
    fun current(lastDay: Long?, streak: Int, today: Long): Int =
        if (lastDay == null || today - lastDay > 1) 0 else streak
}
