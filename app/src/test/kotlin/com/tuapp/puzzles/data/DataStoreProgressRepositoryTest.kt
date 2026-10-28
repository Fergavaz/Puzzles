package com.tuapp.puzzles.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.tuapp.puzzles.core.GameType
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreProgressRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo(scope: TestScope, day: () -> Long): DataStoreProgressRepository {
        val store = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
            File(tmp.root, "test.preferences_pb")
        }
        return DataStoreProgressRepository(store, DayClock(day))
    }

    @Test fun campaignCompletionAndBestTimeArePersisted() = runTest {
        val r = repo(this) { 100 }
        r.recordCompletion(GameType.ZIP, LevelRef.Campaign("zip-01"), 90_000)
        r.recordCompletion(GameType.ZIP, LevelRef.Campaign("zip-01"), 60_000)
        r.recordCompletion(GameType.ZIP, LevelRef.Campaign("zip-01"), 75_000)
        val p = r.progress.first().getValue(GameType.ZIP)
        assertEquals(setOf("zip-01"), p.campaignCompletedIds)
        assertEquals(60_000L, p.bestTimeMs)
        assertEquals(1, p.solvedTotal)
    }

    @Test fun generatedLevelsAreCountedAndListed() = runTest {
        val r = repo(this) { 100 }
        r.recordCompletion(GameType.TANGO, LevelRef.Generated(42, 3), 1_000)
        assertEquals(1, r.progress.first().getValue(GameType.TANGO).generatedCompleted)
        assertEquals(listOf(LevelRef.Generated(42, 3)), r.generatedHistory(GameType.TANGO))
    }

    @Test fun streakGrowsAcrossDaysAndExpires() = runTest {
        var day = 100L
        val r = repo(this) { day }
        r.recordCompletion(GameType.QUEENS, LevelRef.Campaign("queens-01"), 1)
        day = 101
        r.recordCompletion(GameType.QUEENS, LevelRef.Campaign("queens-02"), 1)
        assertEquals(2, r.progress.first().getValue(GameType.QUEENS).streakDays)
        day = 104
        assertEquals(0, r.progress.first().getValue(GameType.QUEENS).streakDays)
    }

    @Test fun gamesAreIndependent() = runTest {
        val r = repo(this) { 100 }
        r.recordCompletion(GameType.SUDOKU, LevelRef.Campaign("sudoku-01"), 5)
        assertEquals(0, r.progress.first().getValue(GameType.ZIP).solvedTotal)
    }
}
