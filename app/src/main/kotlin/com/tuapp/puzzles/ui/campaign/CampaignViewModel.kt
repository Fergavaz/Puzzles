package com.tuapp.puzzles.ui.campaign

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.puzzles.core.CampaignLevel
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.data.CampaignRepository
import com.tuapp.puzzles.data.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CampaignUi(
    val levels: List<CampaignLevel> = emptyList(),
    val completed: Set<String> = emptySet(),
    val bestTimes: Map<String, Long> = emptyMap(),
)

@HiltViewModel
class CampaignViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val campaign: CampaignRepository,
    progress: ProgressRepository,
) : ViewModel() {

    val game: GameType = GameType.fromId(checkNotNull(savedState.get<String>("game")))

    val state: StateFlow<CampaignUi> = progress.progress
        .map { all ->
            val p = all.getValue(game)
            campaign.generateAdditionalLevelsIfCompleted(game, p.campaignCompletedIds, 10)
            val currentLevels = campaign.levels(game)
            CampaignUi(currentLevels, p.campaignCompletedIds, p.bestTimesById)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CampaignUi())
}
