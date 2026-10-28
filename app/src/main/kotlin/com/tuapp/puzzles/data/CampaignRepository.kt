package com.tuapp.puzzles.data

import android.content.Context
import com.tuapp.puzzles.core.CampaignLevel
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.di.IoDispatcher
import com.tuapp.puzzles.engine.CampaignCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Origen del JSON de campaña (assets en producción, texto fijo en pruebas). */
interface CampaignSource {
    fun read(game: GameType): String
}

class AssetCampaignSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : CampaignSource {
    override fun read(game: GameType): String =
        context.assets.open("campaign/${game.id}.json").bufferedReader().use { it.readText() }
}

interface CampaignRepository {
    /** Niveles de campaña ordenados por dificultad creciente. */
    suspend fun levels(game: GameType): List<CampaignLevel>

    suspend fun level(game: GameType, id: String): CampaignLevel?
}

@Singleton
class DefaultCampaignRepository @Inject constructor(
    private val source: CampaignSource,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CampaignRepository {

    private val cache = ConcurrentHashMap<GameType, List<CampaignLevel>>()

    override suspend fun levels(game: GameType): List<CampaignLevel> =
        cache[game] ?: withContext(io) { CampaignCodec.decode(game, source.read(game)) }
            .also { cache[game] = it }

    override suspend fun level(game: GameType, id: String): CampaignLevel? =
        levels(game).firstOrNull { it.id == id }
}
