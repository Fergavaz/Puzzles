package com.tuapp.puzzles.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.data.UpdateCheckResult
import com.tuapp.puzzles.ui.difficultyLabel
import com.tuapp.puzzles.ui.emoji
import com.tuapp.puzzles.ui.formatTime
import com.tuapp.puzzles.ui.instructions.GameInstructionsDialog
import com.tuapp.puzzles.ui.title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCampaign: (GameType) -> Unit,
    onNew: (GameType, Int) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val updateResult by vm.updateResult.collectAsStateWithLifecycle()
    val isCheckingUpdate by vm.isCheckingUpdate.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var picking by remember { mutableStateOf<GameType?>(null) }
    var viewingInstructions by remember { mutableStateOf<GameType?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Puzzles") },
                actions = {
                    IconButton(
                        onClick = { vm.checkForUpdates(isUserInitiated = true) },
                        enabled = !isCheckingUpdate,
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = "Comprobar actualizaciones")
                        }
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.streakDays > 0) {
                item { Text("🔥 Racha: ${state.streakDays} ${if (state.streakDays == 1) "día" else "días"}", style = MaterialTheme.typography.titleMedium) }
            }
            items(state.cards, key = { it.game }) { card ->
                GameCard(
                    card,
                    onCampaign = { onCampaign(card.game) },
                    onNew = { picking = card.game },
                    onInstructions = { viewingInstructions = card.game },
                )
            }
        }
    }

    // Diálogo de actualizaciones
    when (val u = updateResult) {
        is UpdateCheckResult.Available -> {
            AlertDialog(
                onDismissRequest = vm::dismissUpdateDialog,
                title = { Text("¡Actualización disponible! 🎉") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hay una nueva versión disponible: v${u.info.latestVersionName}")
                        if (u.info.releaseNotes.isNotBlank()) {
                            Text(
                                text = u.info.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.dismissUpdateDialog()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(u.info.downloadUrl))
                            context.startActivity(intent)
                        },
                    ) {
                        Text("Descargar e instalar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissUpdateDialog) { Text("Más tarde") }
                },
            )
        }
        is UpdateCheckResult.UpToDate -> {
            AlertDialog(
                onDismissRequest = vm::dismissUpdateDialog,
                title = { Text("App actualizada ✅") },
                text = { Text("Ya tienes instalada la última versión disponible (v${u.currentVersion}).") },
                confirmButton = {
                    Button(onClick = vm::dismissUpdateDialog) { Text("Aceptar") }
                },
            )
        }
        is UpdateCheckResult.CheckFailed -> {
            AlertDialog(
                onDismissRequest = vm::dismissUpdateDialog,
                title = { Text("Comprobar versión ℹ️") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Puedes comprobar o descargar la última versión directamente desde los Releases de GitHub.")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            vm.dismissUpdateDialog()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(u.manualDownloadUrl))
                            context.startActivity(intent)
                        },
                    ) {
                        Text("Abrir Releases")
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissUpdateDialog) { Text("Cerrar") }
                },
            )
        }
        null -> Unit
    }

    picking?.let { game ->
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text("${game.title}: elige la dificultad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in 1..5) {
                        OutlinedButton(onClick = { picking = null; onNew(game, d) }, modifier = Modifier.fillMaxWidth()) {
                            Text("$d · ${difficultyLabel(d)}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancelar") } },
        )
    }

    viewingInstructions?.let { game ->
        GameInstructionsDialog(game = game, onDismiss = { viewingInstructions = null })
    }
}

@Composable
private fun GameCard(card: GameCardState, onCampaign: () -> Unit, onNew: () -> Unit, onInstructions: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(card.game.emoji, fontSize = 32.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.game.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${card.campaignDone}/${card.campaignTotal} campaña · ${card.generated} generados" +
                            (card.bestTimeMs?.let { " · mejor ${formatTime(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onInstructions) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Cómo jugar a ${card.game.title}")
                }
            }
            LinearProgressIndicator(
                progress = { if (card.campaignTotal == 0) 0f else card.campaignDone / card.campaignTotal.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCampaign) { Text("Jugar campaña") }
                OutlinedButton(onClick = onNew) { Text("Generar nuevo") }
            }
        }
    }
}
