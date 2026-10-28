package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.puzzles.engine.session.SudokuSession

/** Toque en celda + paleta 1..6. Regiones de 2 filas x 3 columnas marcadas con bordes gruesos. */
@Composable
fun SudokuBoard(
    session: SudokuSession,
    revision: Int,
    act: Act,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision
    val n = 6
    val bad = session.conflicts
    val line = MaterialTheme.colorScheme.onSurface
    val selectedBg = MaterialTheme.colorScheme.primaryContainer
    val highlightBg = MaterialTheme.colorScheme.tertiaryContainer

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SquareGrid(n) { i, cs ->
            val r = i / n; val c = i % n
            val v = session.state[i]
            val given = session.isGiven(i)
            val bg = when {
                i == highlight -> highlightBg
                session.selected == i -> selectedBg
                else -> MaterialTheme.colorScheme.surface
            }
            Box(
                Modifier.fillMaxSize()
                    .background(bg)
                    .drawBehind {
                        val thin = 0.5.dp.toPx(); val thick = 2.5.dp.toPx(); val w = size.width; val h = size.height
                        drawLine(line, Offset(0f, 0f), Offset(w, 0f), if (r % 2 == 0) thick else thin)
                        drawLine(line, Offset(0f, 0f), Offset(0f, h), if (c % 3 == 0) thick else thin)
                        if (r == n - 1) drawLine(line, Offset(0f, h), Offset(w, h), thick)
                        if (c == n - 1) drawLine(line, Offset(w, 0f), Offset(w, h), thick)
                    }
                    .clickable(onClickLabel = "Seleccionar celda") { act { session.select(i) } }
                    .semantics {
                        contentDescription = "Fila ${r + 1}, columna ${c + 1}: " +
                            (if (v == 0) "vacía" else "$v") + (if (given) ", fija" else "") + (if (i in bad) ", en conflicto" else "")
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (v != 0) {
                    Text(
                        "$v", fontSize = (cs.value * 0.55f).sp,
                        fontWeight = if (given) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            i in bad -> MaterialTheme.colorScheme.error
                            given -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (v in 1..6) {
                FilledTonalButton(
                    onClick = { act { session.enter(v) } },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                ) { Text("$v", fontSize = 20.sp) }
            }
            FilledTonalIconButton(onClick = { act { session.enter(0) } }, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Borrar número")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
