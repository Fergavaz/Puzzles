package com.tuapp.puzzles.ui.game

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.puzzles.engine.session.PatchesSession
import com.tuapp.puzzles.engine.session.QueensSession
import com.tuapp.puzzles.engine.session.SudokuSession
import com.tuapp.puzzles.engine.session.TangoSession
import com.tuapp.puzzles.engine.session.ZipSession
import com.tuapp.puzzles.ui.boards.PatchesBoard
import com.tuapp.puzzles.ui.boards.QueensBoard
import com.tuapp.puzzles.ui.boards.SudokuBoard
import com.tuapp.puzzles.ui.boards.TangoBoard
import com.tuapp.puzzles.ui.boards.ZipBoard
import com.tuapp.puzzles.ui.formatTime
import com.tuapp.puzzles.ui.instructions.GameInstructionsDialog
import com.tuapp.puzzles.ui.title
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    vm: GameViewModel = hiltViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val revision by vm.revision.collectAsStateWithLifecycle()
    val elapsed by vm.elapsedMs.collectAsStateWithLifecycle()
    val solved by vm.solved.collectAsStateWithLifecycle()
    val hintHighlight by vm.hintHighlight.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // El cronómetro se detiene mientras la app no está en primer plano
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.setPaused(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.setPaused(false) }

    val subtitle = (ui as? GameUi.Ready)?.title ?: ""
    val boardModifier = Modifier.widthIn(max = 520.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${vm.game.title} · $subtitle", style = MaterialTheme.typography.titleMedium)
                        Text(formatTime(elapsed), style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Cómo jugar") }
                    IconButton(onClick = vm::undo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer") }
                    IconButton(onClick = vm::clear) { Icon(Icons.Filled.DeleteSweep, contentDescription = "Borrar todo") }
                    IconButton(onClick = vm::hint) { Icon(Icons.Filled.Lightbulb, contentDescription = "Pista") }
                    IconButton(onClick = vm::restart) { Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar") }
                },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            when (val u = ui) {
                GameUi.Loading -> CircularProgressIndicator()
                is GameUi.Failed -> Text(u.message, color = MaterialTheme.colorScheme.error)
                is GameUi.Ready -> when (val s = u.session) {
                    is TangoSession -> TangoBoard(s, revision, vm::perform, boardModifier, hintHighlight)
                    is QueensSession -> QueensBoard(s, revision, vm::perform, boardModifier, hintHighlight)
                    is SudokuSession -> SudokuBoard(s, revision, vm::perform, boardModifier, hintHighlight)
                    is ZipSession -> ZipBoard(s, revision, vm::perform, boardModifier, hintHighlight)
                    is PatchesSession -> PatchesBoard(s, revision, vm::perform, boardModifier, hintHighlight)
                    else -> Unit
                }
            }
        }
    }

    if (solved) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¡Resuelto! 🎉") },
            text = { Text("Tiempo: ${formatTime(elapsed)}") },
            confirmButton = {
                Button(onClick = { scope.launch { vm.nextRoute()?.let(onNavigate) ?: onBack() } }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = onBack) { Text("Salir") } },
        )
    }

    if (showHelp) {
        GameInstructionsDialog(game = vm.game, onDismiss = { showHelp = false })
    }
}
