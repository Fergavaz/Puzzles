package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.puzzles.engine.session.QueensSession

/** Toques cíclicos vacío -> X -> reina -> vacío; arrastre para marcar múltiples X; opción de auto-marcado. */
@Composable
fun QueensBoard(session: QueensSession, revision: Int, act: Act, modifier: Modifier = Modifier, highlight: Int? = null) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision
    val puzzle = session.level.puzzle
    val n = puzzle.size
    val regions = puzzle.regions
    val dark = isSystemInDarkTheme()
    val line = MaterialTheme.colorScheme.onSurface
    val bad = session.conflicts

    var autoMark by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SquareGrid(
            n,
            modifier = Modifier.pointerInput(session, autoMark) {
                awaitEachGesture {
                    val cellPx = size.width / n.toFloat()
                    fun cellAt(o: Offset): Int? {
                        val c = (o.x / cellPx).toInt(); val r = (o.y / cellPx).toInt()
                        return if (o.x >= 0 && o.y >= 0 && c in 0 until n && r in 0 until n) r * n + c else null
                    }
                    val down = awaitFirstDown()
                    val startPos = down.position
                    val startCell = cellAt(startPos) ?: return@awaitEachGesture
                    val startState = session.state[startCell]
                    // Si se inicia el gesto en una casilla con X, arrastrar desmarca; si estaba vacía, marca con X
                    val isUnmarkMode = startState == QueensSession.MARK

                    var dragged = false
                    val touchedCells = mutableSetOf<Int>()
                    val slop = viewConfiguration.touchSlop

                    try {
                        drag(down.id) { change ->
                            val dist = (change.position - startPos).getDistance()
                            if (dist > slop) {
                                dragged = true
                            }
                            val curCell = cellAt(change.position)
                            if (curCell != null && (curCell != startCell || dragged)) {
                                dragged = true
                                if (startCell !in touchedCells) {
                                    touchedCells.add(startCell)
                                    act {
                                        if (isUnmarkMode) session.unmark(startCell)
                                        else session.markEmpty(startCell)
                                    }
                                }
                                if (curCell !in touchedCells) {
                                    touchedCells.add(curCell)
                                    act {
                                        if (isUnmarkMode) session.unmark(curCell)
                                        else session.markEmpty(curCell)
                                    }
                                }
                            }
                            change.consume()
                        }
                    } finally {
                        if (!dragged) {
                            act { session.tap(startCell, autoMark) }
                        }
                    }
                }
            },
        ) { i, cs ->
            val r = i / n; val c = i % n
            val reg = regions[i]
            val state = session.state[i]
            val label = when (state) { QueensSession.QUEEN -> "reina"; QueensSession.MARK -> "marcada con X"; else -> "vacía" }
            Box(
                Modifier.fillMaxSize()
                    .background(regionColor(reg, dark))
                    .background(when {
                        i == highlight -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                        i in bad -> Color.Red.copy(alpha = 0.35f)
                        else -> Color.Transparent
                    })
                    .drawBehind {
                        val thin = 0.5.dp.toPx(); val thick = 2.5.dp.toPx(); val w = size.width; val h = size.height
                        fun edge(a: Offset, b: Offset, differs: Boolean) =
                            drawLine(line, a, b, strokeWidth = if (differs) thick else thin)
                        edge(Offset(0f, 0f), Offset(w, 0f), r == 0 || regions[i - n] != reg)
                        edge(Offset(0f, h), Offset(w, h), r == n - 1 || regions[i + n] != reg)
                        edge(Offset(0f, 0f), Offset(0f, h), c == 0 || regions[i - 1] != reg)
                        edge(Offset(w, 0f), Offset(w, h), c == n - 1 || regions[i + 1] != reg)
                    }
                    .semantics {
                        contentDescription = "Fila ${r + 1}, columna ${c + 1}, región ${reg + 1}: $label"
                        onClick(label = "Cambiar casilla") {
                            act { session.tap(i, autoMark) }
                            true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val symbolColor = if (dark) Color.White else Color.Black
                when (state) {
                    QueensSession.QUEEN -> Text("♛", fontSize = (cs.value * 0.65f).sp, color = symbolColor)
                    QueensSession.MARK -> Text("✕", fontSize = (cs.value * 0.4f).sp, color = symbolColor.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        FilterChip(
            selected = autoMark,
            onClick = { autoMark = !autoMark },
            label = { Text("Auto-marcar X con corona") },
            leadingIcon = {
                Icon(
                    if (autoMark) Icons.Default.Check else Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}
