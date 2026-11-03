package com.tuapp.puzzles.data

import android.content.Context
import com.tuapp.puzzles.core.CampaignLevel
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel
import com.tuapp.puzzles.di.DefaultDispatcher
import com.tuapp.puzzles.di.IoDispatcher
import com.tuapp.puzzles.engine.CampaignCodec
import com.tuapp.puzzles.engine.Generators
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Añade un nivel generado a la campaña y lo guarda de forma persistente. */
    suspend fun addLevel(game: GameType, level: PuzzleLevel): CampaignLevel

    /** Añade múltiples niveles a la campaña y los guarda de forma persistente. */
    suspend fun addLevels(game: GameType, newLevels: List<PuzzleLevel>): List<CampaignLevel>

    /** Genera [count] niveles adicionales (por defecto 10) equilibrados en dificultad y los guarda. */
    suspend fun generateAdditionalLevels(game: GameType, count: Int = 10): List<CampaignLevel>

    /** Si todos los niveles de la campaña están completados, genera [count] adicionales de forma atómica. */
    suspend fun generateAdditionalLevelsIfCompleted(game: GameType, completedIds: Set<String>, count: Int = 10): List<CampaignLevel>
}

@Singleton
class DefaultCampaignRepository(
    private val context: Context?,
    private val source: CampaignSource,
    private val io: CoroutineDispatcher,
    private val cpu: CoroutineDispatcher,
    private val overrideFilesDir: File? = null,
) : CampaignRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        source: CampaignSource,
        @IoDispatcher io: CoroutineDispatcher,
        @DefaultDispatcher cpu: CoroutineDispatcher,
    ) : this(context, source, io, cpu, null)

    private val cache = ConcurrentHashMap<GameType, List<CampaignLevel>>()
    private val mutex = Mutex()

    private fun customFile(game: GameType): File {
        val dir = overrideFilesDir ?: checkNotNull(context).filesDir
        return File(dir, "campaign_custom_${game.id}.json")
    }

    override suspend fun levels(game: GameType): List<CampaignLevel> {
        val cached = cache[game]
        if (cached != null) return cached
        return mutex.withLock {
            cache[game] ?: loadAllLevels(game).also { cache[game] = it }
        }
    }

    private suspend fun loadAllLevels(game: GameType): List<CampaignLevel> = withContext(io) {
        val baseText = source.read(game)
        val base = CampaignCodec.decode(game, baseText)
        val file = customFile(game)
        val custom = if (file.exists()) {
            try {
                CampaignCodec.decode(game, file.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        val combined = ArrayList<CampaignLevel>(base.size + custom.size)
        combined.addAll(base)
        custom.forEachIndexed { i, cl ->
            val idx = base.size + i
            combined.add(CampaignLevel(cl.id, idx, cl.level))
        }
        combined
    }

    override suspend fun level(game: GameType, id: String): CampaignLevel? =
        levels(game).firstOrNull { it.id == id }

    override suspend fun addLevel(game: GameType, level: PuzzleLevel): CampaignLevel =
        addLevels(game, listOf(level)).first()

    override suspend fun addLevels(game: GameType, newLevels: List<PuzzleLevel>): List<CampaignLevel> =
        mutex.withLock {
            saveNewLevelsLocked(game, newLevels)
        }

    private suspend fun saveNewLevelsLocked(game: GameType, newLevels: List<PuzzleLevel>): List<CampaignLevel> {
        val current = (cache[game] ?: loadAllLevels(game)).toMutableList()
        val baseSize = withContext(io) { CampaignCodec.decode(game, source.read(game)).size }
        val added = ArrayList<CampaignLevel>(newLevels.size)
        for (lvl in newLevels) {
            val nextIndex = current.size
            val id = "${game.id}-c$nextIndex"
            val newCl = CampaignLevel(id, nextIndex, lvl)
            current.add(newCl)
            added.add(newCl)
        }
        cache[game] = current
        val customOnly = current.drop(baseSize)
        withContext(io) {
            val file = customFile(game)
            file.parentFile?.mkdirs()
            file.writeText(CampaignCodec.encode(game, customOnly))
        }
        return added
    }

    override suspend fun generateAdditionalLevels(game: GameType, count: Int): List<CampaignLevel> {
        val current = levels(game)
        val startIndex = current.size
        val newLevels = withContext(cpu) {
            val list = mutableListOf<PuzzleLevel>()
            val perDiff = (count / 5).coerceAtLeast(1)
            val diffs = (1..5).flatMap { d -> List(perDiff) { d } }.take(count)
            var seedOffset = System.currentTimeMillis() + startIndex * 1000L
            for (d in diffs) {
                var lvl: PuzzleLevel? = null
                while (lvl == null) {
                    lvl = Generators.generate(game, seedOffset, d)
                    seedOffset++
                }
                list.add(lvl)
            }
            list
        }
        return addLevels(game, newLevels)
    }

    override suspend fun generateAdditionalLevelsIfCompleted(
        game: GameType,
        completedIds: Set<String>,
        count: Int,
    ): List<CampaignLevel> = mutex.withLock {
        val current = cache[game] ?: loadAllLevels(game)
        if (current.isNotEmpty() && current.all { it.id in completedIds }) {
            val startIndex = current.size
            val newLevels = withContext(cpu) {
                val list = mutableListOf<PuzzleLevel>()
                val perDiff = (count / 5).coerceAtLeast(1)
                val diffs = (1..5).flatMap { d -> List(perDiff) { d } }.take(count)
                var seedOffset = System.currentTimeMillis() + startIndex * 1000L
                for (d in diffs) {
                    var lvl: PuzzleLevel? = null
                    while (lvl == null) {
                        lvl = Generators.generate(game, seedOffset, d)
                        seedOffset++
                    }
                    list.add(lvl)
                }
                list
            }
            saveNewLevelsLocked(game, newLevels)
        } else {
            emptyList()
        }
    }
}
