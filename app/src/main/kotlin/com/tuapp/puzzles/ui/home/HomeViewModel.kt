package com.tuapp.puzzles.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.puzzles.core.CampaignProgress
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.data.CampaignRepository
import com.tuapp.puzzles.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class GameCardState(
    val game: GameType,
    val campaignDone: Int,
    val campaignTotal: Int,
    val generated: Int,
    val bestTimeMs: Long?,
)

data class HomeState(val cards: List<GameCardState> = emptyList(), val streakDays: Int = 0)

@HiltViewModel
class HomeViewModel @Inject constructor(
    campaign: CampaignRepository,
    progress: ProgressRepository,
) : ViewModel() {

    val state: StateFlow<HomeState> = progress.progress
        .map { byGame ->
            val cards = GameType.entries.map { g ->
                val p = byGame.getValue(g)
                val levels = campaign.levels(g)
                GameCardState(g, CampaignProgress.completedCount(levels, p.campaignCompletedIds), levels.size, p.generatedCompleted, p.bestTimeMs)
            }
            HomeState(cards, byGame.values.maxOfOrNull { it.streakDays } ?: 0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
