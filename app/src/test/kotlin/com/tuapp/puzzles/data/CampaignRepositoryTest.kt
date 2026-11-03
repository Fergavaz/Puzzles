package com.tuapp.puzzles.data

import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.engine.Generators
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CampaignRepositoryTest {

    private lateinit var tempDir: File

    private val sampleBaseJson = """
        {"game":"queens","version":1,"levels":[
            {"id":"queens-00","index":0,"seed":1,"difficulty":1,"size":4,"regions":[0,0,1,1,0,0,1,1,2,2,3,3,2,2,3,3],"solution":[1,3,0,2]},
            {"id":"queens-01","index":1,"seed":2,"difficulty":2,"size":4,"regions":[0,1,1,1,0,0,2,1,0,2,2,3,3,3,3,3],"solution":[2,0,3,1]}
        ]}
    """.trimIndent()

    private val fakeSource = object : CampaignSource {
        override fun read(game: GameType): String = sampleBaseJson
    }

    @Before
    fun setup() {
        tempDir = File.createTempFile("puzzles_test", "").also {
            it.delete()
            it.mkdirs()
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createRepo(): DefaultCampaignRepository =
        DefaultCampaignRepository(
            context = null,
            source = fakeSource,
            io = Dispatchers.Unconfined,
            cpu = Dispatchers.Unconfined,
            overrideFilesDir = tempDir,
        )

    @Test
    fun loadsBaseLevelsInitially() = runTest {
        val repo = createRepo()
        val levels = repo.levels(GameType.QUEENS)
        assertEquals(2, levels.size)
        assertEquals("queens-00", levels[0].id)
        assertEquals("queens-01", levels[1].id)
    }

    @Test
    fun addLevelPersistsAndAppendsToCampaign() = runTest {
        val repo = createRepo()
        val generated = Generators.generate(GameType.QUEENS, 1234L, 2)!!
        val added = repo.addLevel(GameType.QUEENS, generated)

        assertEquals("queens-c2", added.id)
        assertEquals(2, added.index)

        // Verificamos que se recarga con el nivel añadido
        val reloaded = repo.levels(GameType.QUEENS)
        assertEquals(3, reloaded.size)
        assertEquals("queens-c2", reloaded[2].id)

        // Verificamos que una nueva instancia del repo también lo carga del archivo
        val newRepo = createRepo()
        val fromDisk = newRepo.levels(GameType.QUEENS)
        assertEquals(3, fromDisk.size)
        assertEquals("queens-c2", fromDisk[2].id)
    }

    @Test
    fun generate10AdditionalLevelsWhenAllCompleted() = runTest {
        val repo = createRepo()
        val initial = repo.levels(GameType.QUEENS)
        assertEquals(2, initial.size)

        // Si solo se ha completado 1 nivel de 2, no se genera nada adicional
        val notAllDone = repo.generateAdditionalLevelsIfCompleted(GameType.QUEENS, setOf("queens-00"), 10)
        assertTrue(notAllDone.isEmpty())
        assertEquals(2, repo.levels(GameType.QUEENS).size)

        // Si se han completado todos (ambos), se generan 10 adicionales
        val allDone = setOf("queens-00", "queens-01")
        val generated10 = repo.generateAdditionalLevelsIfCompleted(GameType.QUEENS, allDone, 10)
        assertEquals(10, generated10.size)

        val updated = repo.levels(GameType.QUEENS)
        assertEquals(12, updated.size)
        // Índices consecutivos del 2 al 11
        for (i in 0 until 10) {
            assertEquals(2 + i, generated10[i].index)
        }

        // Llamar de nuevo con el mismo set 'allDone' no debe generar otros 10 porque ahora hay 12 niveles
        val duplicateCall = repo.generateAdditionalLevelsIfCompleted(GameType.QUEENS, allDone, 10)
        assertTrue(duplicateCall.isEmpty())
        assertEquals(12, repo.levels(GameType.QUEENS).size)
    }
}
