package com.tuapp.puzzles.ui.campaign

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.puzzles.core.CampaignProgress
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.ui.difficultyLabel
import com.tuapp.puzzles.ui.formatTime
import com.tuapp.puzzles.ui.title

/**
 * Campaña en JUEGO LIBRE: todos los niveles están disponibles; el progreso solo se marca
 * (✓ y mejor tiempo) y se puede filtrar por dificultad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(
    onBack: () -> Unit,
    onPlay: (GameType, String) -> Unit,
    vm: CampaignViewModel = hiltViewModel(),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<Int?>(null) }
    val shown = ui.levels.filter { filter == null || it.level.difficulty == filter }
    val next = filter?.let { CampaignProgress.nextInDifficulty(ui.levels, ui.completed, it) }
        ?: CampaignProgress.nextToPlay(ui.levels, ui.completed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${vm.game.title} · Campaña") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${CampaignProgress.completedCount(ui.levels, ui.completed)}/${ui.levels.size} completados · elige cualquier nivel",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos") })
                CampaignProgress.difficultyStats(ui.levels, ui.completed).forEach { (d, stat) ->
                    FilterChip(
                        selected = filter == d,
                        onClick = { filter = if (filter == d) null else d },
                        label = { Text("${difficultyLabel(d)} ${stat.first}/${stat.second}") },
                    )
                }
            }
            next?.let { n ->
                Button(onClick = { onPlay(vm.game, n.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar · nivel ${n.index + 1}")
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(84.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.id }) { cl ->
                    val done = cl.id in ui.completed
                    val best = ui.bestTimes[cl.id]
                    Card(
                        onClick = { onPlay(vm.game, cl.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Nivel ${cl.index + 1}, ${difficultyLabel(cl.level.difficulty)}" +
                                (if (done) ", completado" else "") + (best?.let { ", mejor tiempo ${formatTime(it)}" } ?: "")
                        },
                    ) {
                        Column(Modifier.padding(10.dp).fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("${cl.index + 1}", style = MaterialTheme.typography.titleMedium)
                                if (done) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text("●".repeat(cl.level.difficulty) + "○".repeat(5 - cl.level.difficulty), style = MaterialTheme.typography.labelSmall)
                            Text(best?.let { formatTime(it) } ?: " ", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
