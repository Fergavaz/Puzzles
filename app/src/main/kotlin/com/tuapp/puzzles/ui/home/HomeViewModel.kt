package com.tuapp.puzzles.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.puzzles.core.CampaignProgress
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.data.CampaignRepository
import com.tuapp.puzzles.data.ProgressRepository
import com.tuapp.puzzles.data.UpdateCheckResult
import com.tuapp.puzzles.data.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    private val _updateResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateResult: StateFlow<UpdateCheckResult?> = _updateResult.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        // Comprobación en segundo plano al iniciar
        checkForUpdates(isUserInitiated = false)
    }

    fun checkForUpdates(isUserInitiated: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                val res = updateChecker.check()
                if (isUserInitiated || res is UpdateCheckResult.Available) {
                    _updateResult.value = res
                }
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateResult.value = null
    }

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
