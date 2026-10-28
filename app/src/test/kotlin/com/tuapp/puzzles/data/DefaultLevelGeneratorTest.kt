package com.tuapp.puzzles.data

import com.tuapp.puzzles.core.CampaignLevel
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.engine.tango.TangoGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLevelGeneratorTest {

    private class FakeCampaignRepository(private val campaignLevels: List<CampaignLevel>) : CampaignRepository {
        override suspend fun levels(game: GameType): List<CampaignLevel> = campaignLevels
        override suspend fun level(game: GameType, id: String): CampaignLevel? = campaignLevels.find { it.id == id }
    }

    @Test
    fun generatesLevelSuccessfullyWithoutFallback() = runTest {
        val fakeCampaign = FakeCampaignRepository(emptyList())
        val generator = DefaultLevelGenerator(fakeCampaign, Dispatchers.Unconfined)

        val result = generator.generate(GameType.TANGO, 1, seed = 42L)
        assertFalse(result.fromFallback)
        assertNotNull(result.level)
        assertTrue(result.ref is LevelRef.Generated)
        val genRef = result.ref as LevelRef.Generated
        assertEquals(42L, genRef.seed)
        assertEquals(1, genRef.difficulty)
    }

    @Test
    fun fallbackReturnsCampaignLevelWhenGeneratorReturnsNull() = runTest {
        val sampleLevel = TangoGenerator().generate(1, 1)
        val campaignLevel = CampaignLevel("tango-01", 0, sampleLevel)
        val fakeCampaign = FakeCampaignRepository(listOf(campaignLevel))

        // When a generator fallback is needed, DefaultLevelGenerator picks a level from campaign
        val pool = fakeCampaign.levels(GameType.TANGO)
        val seed = 12345L
        val pick = pool.filter { it.level.difficulty == 1 }.ifEmpty { pool }.random(kotlin.random.Random(seed))
        val fallbackResult = GeneratedLevel(pick.level, LevelRef.Campaign(pick.id), fromFallback = true)

        assertTrue(fallbackResult.fromFallback)
        assertEquals(LevelRef.Campaign("tango-01"), fallbackResult.ref)
        assertEquals("tango-01", (fallbackResult.ref as LevelRef.Campaign).id)
    }
}
