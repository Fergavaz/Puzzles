package com.tuapp.puzzles.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.PuzzleLevel
import com.tuapp.puzzles.data.CampaignRepository
import com.tuapp.puzzles.data.LevelGenerator
import com.tuapp.puzzles.data.LevelRef
import com.tuapp.puzzles.data.ProgressRepository
import com.tuapp.puzzles.engine.patches.PatchesLevel
import com.tuapp.puzzles.engine.queens.QueensLevel
import com.tuapp.puzzles.engine.session.PatchesSession
import com.tuapp.puzzles.engine.session.QueensSession
import com.tuapp.puzzles.engine.session.Snapshotted
import com.tuapp.puzzles.engine.session.SudokuSession
import com.tuapp.puzzles.engine.session.TangoSession
import com.tuapp.puzzles.engine.session.ZipSession
import com.tuapp.puzzles.engine.sudoku.SudokuLevel
import com.tuapp.puzzles.engine.tango.TangoLevel
import com.tuapp.puzzles.engine.zip.ZipLevel
import com.tuapp.puzzles.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GameUi {
    data object Loading : GameUi
    data class Failed(val message: String) : GameUi
    /** [session] es una de las sesiones del motor (TangoSession, ZipSession, ...). */
    data class Ready(val title: String, val session: Any) : GameUi
}

@HiltViewModel
class GameViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val campaign: CampaignRepository,
    private val generator: LevelGenerator,
    private val progress: ProgressRepository,
) : ViewModel() {

    val game: GameType = GameType.fromId(checkNotNull(savedState.get<String>("game")))
    private val levelId: String? = savedState["levelId"]
    private val seedArg: Long = savedState["seed"] ?: -1L
    private val difficultyArg: Int = savedState["difficulty"] ?: 3

    private val _ui = MutableStateFlow<GameUi>(GameUi.Loading)
    val uiState: StateFlow<GameUi> = _ui.asStateFlow()

    /** Se incrementa tras cada acción: las sesiones del motor son mutables y la UI se recompone con esto. */
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _solved = MutableStateFlow(false)
    val solved: StateFlow<Boolean> = _solved.asStateFlow()

    private val _hintHighlight = MutableStateFlow<Int?>(null)
    val hintHighlight: StateFlow<Int?> = _hintHighlight.asStateFlow()

    private var paused = false
    private var level: PuzzleLevel? = null
    private var ref: LevelRef? = null
    private var title = ""

    init {
        load()
        // Temporizador: cuenta segundos mientras la pantalla está visible y el puzzle sin resolver
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!paused && !_solved.value && _ui.value is GameUi.Ready) _elapsedMs.update { it + 1000 }
            }
        }
    }

    private fun load() = viewModelScope.launch {
        try {
            val id = levelId
            if (id != null) {
                val cl = campaign.level(game, id) ?: error("Nivel no encontrado")
                start(cl.level, LevelRef.Campaign(cl.id), "Nivel ${cl.index + 1}")
            } else {
                val g = if (seedArg >= 0) generator.generate(game, difficultyArg, seedArg) else generator.generate(game, difficultyArg)
                val label = if (g.ref is LevelRef.Campaign) "Nivel de respaldo" else "Generado"
                start(g.level, g.ref, label)
            }
        } catch (e: Exception) {
            _ui.value = GameUi.Failed(e.message ?: "No se pudo cargar el nivel")
        }
    }

    private fun start(l: PuzzleLevel, r: LevelRef, t: String) {
        level = l; ref = r; title = t
        _elapsedMs.value = 0
        _solved.value = false
        _ui.value = GameUi.Ready(t, createSession(l))
    }

    private fun createSession(l: PuzzleLevel): Any = when (l) {
        is ZipLevel -> ZipSession(l)
        is PatchesLevel -> PatchesSession(l)
        is SudokuLevel -> SudokuSession(l)
        is TangoLevel -> TangoSession(l)
        is QueensLevel -> QueensSession(l)
        else -> error("Nivel no soportado")
    }

    fun setPaused(value: Boolean) { paused = value }

    /** Toda acción del jugador pasa por aquí: se ejecuta, se refresca la UI y se comprueba si está resuelto. */
    fun perform(block: () -> Unit) {
        val session = (_ui.value as? GameUi.Ready)?.session ?: return
        if (_solved.value) return
        block()
        _revision.update { it + 1 }
        if (isSolved(session)) onSolved()
    }

    fun undo() = withSession { (it as Snapshotted<*>).undo() }

    fun clear() = withSession { s ->
        when (s) {
            is TangoSession -> s.clear()
            is QueensSession -> s.clear()
            is SudokuSession -> s.clear()
            is ZipSession -> s.clear()
            is PatchesSession -> s.clear()
            else -> Unit
        }
    }

    fun hint() = withSession { s ->
        val cell = when (s) {
            is TangoSession -> s.hint()
            is QueensSession -> s.hint()
            is SudokuSession -> s.hint()?.first
            is ZipSession -> s.hint()
            is PatchesSession -> s.hint()
            else -> null
        }
        if (cell != null) {
            _hintHighlight.value = cell
            viewModelScope.launch {
                delay(1500)
                if (_hintHighlight.value == cell) _hintHighlight.value = null
            }
        }
    }

    /** Reiniciar: sesión nueva del mismo nivel y cronómetro a cero. */
    fun restart() {
        val l = level ?: return
        start(l, checkNotNull(ref), title)
        _revision.update { it + 1 }
    }

    private fun withSession(action: (Any) -> Unit) = perform { action((_ui.value as GameUi.Ready).session) }

    private fun isSolved(s: Any): Boolean = when (s) {
        is TangoSession -> s.isSolved
        is QueensSession -> s.isSolved
        is SudokuSession -> s.isSolved
        is ZipSession -> s.isSolved
        is PatchesSession -> s.isSolved
        else -> false
    }

    private fun onSolved() {
        _solved.value = true
        val r = ref ?: return
        val time = _elapsedMs.value
        viewModelScope.launch { progress.recordCompletion(game, r, time) }
    }

    /** Ruta del siguiente nivel: el siguiente de campaña, o uno nuevo de la misma dificultad si era generado. */
    suspend fun nextRoute(): String? = when (val r = ref) {
        is LevelRef.Campaign -> {
            val levels = campaign.levels(game)
            val i = levels.indexOfFirst { it.id == r.id }
            levels.getOrNull(i + 1)?.let { Routes.playLevel(game, it.id) }
        }
        is LevelRef.Generated -> Routes.playNew(game, r.difficulty)
        null -> null
    }
}
