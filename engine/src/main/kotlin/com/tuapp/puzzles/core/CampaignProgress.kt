package com.tuapp.puzzles.core

/**
 * Campaña en JUEGO LIBRE: ningún nivel está bloqueado. El progreso solo se marca visualmente
 * y estas utilidades ayudan a la pantalla a ofrecer "siguiente nivel" y a filtrar por dificultad.
 */
object CampaignProgress {
    fun completedCount(levels: List<CampaignLevel>, done: Set<String>): Int = levels.count { it.id in done }

    /** Primer nivel sin completar (para el botón "Continuar"). null si ya están todos. */
    fun nextToPlay(levels: List<CampaignLevel>, done: Set<String>): CampaignLevel? =
        levels.firstOrNull { it.id !in done }

    /** Primer nivel sin completar de una dificultad concreta. */
    fun nextInDifficulty(levels: List<CampaignLevel>, done: Set<String>, difficulty: Int): CampaignLevel? =
        levels.firstOrNull { it.level.difficulty == difficulty && it.id !in done }

    /** Dificultad -> (completados, total). */
    fun difficultyStats(levels: List<CampaignLevel>, done: Set<String>): Map<Int, Pair<Int, Int>> =
        levels.groupBy { it.level.difficulty }.toSortedMap()
            .mapValues { (_, group) -> group.count { it.id in done } to group.size }
}
