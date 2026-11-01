package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.puzzles.R
import com.tuapp.puzzles.engine.queens.QueensHint
import com.tuapp.puzzles.engine.queens.QueensTechnique
import com.tuapp.puzzles.engine.queens.UnitKind
import com.tuapp.puzzles.engine.queens.UnitRef
import com.tuapp.puzzles.engine.session.QueensSession

@Composable
fun formatQueensHint(hint: QueensHint): String {
    val context = LocalContext.current
    fun formatUnit(u: UnitRef): String = when (u.kind) {
        UnitKind.REGION -> context.getString(R.string.unit_region, u.index + 1)
        UnitKind.ROW -> context.getString(R.string.unit_row, u.index + 1)
        UnitKind.COLUMN -> context.getString(R.string.unit_col, u.index + 1)
    }

    return when (hint) {
        is QueensHint.Wrong -> when (hint.technique) {
            QueensTechnique.WRONG_QUEEN -> context.getString(R.string.queens_hint_wrong_queen)
            QueensTechnique.WRONG_MARK -> context.getString(R.string.queens_hint_wrong_mark)
            else -> context.getString(R.string.queens_hint_wrong_queen)
        }
        is QueensHint.PlaceQueen -> when (hint.technique) {
            QueensTechnique.GIVEAWAY -> {
                val u = hint.units.firstOrNull()
                when (u?.kind) {
                    UnitKind.REGION -> context.getString(R.string.queens_hint_giveaway_region, u.index + 1)
                    UnitKind.ROW -> context.getString(R.string.queens_hint_giveaway_row, u.index + 1)
                    UnitKind.COLUMN -> context.getString(R.string.queens_hint_giveaway_col, u.index + 1)
                    null -> context.getString(R.string.queens_hint_fallback, 1)
                }
            }
            QueensTechnique.SOLUTION_FALLBACK -> {
                val reg = hint.units.firstOrNull { it.kind == UnitKind.REGION }?.index ?: 0
                context.getString(R.string.queens_hint_fallback, reg + 1)
            }
            else -> context.getString(R.string.queens_hint_fallback, 1)
        }
        is QueensHint.Eliminate -> when (hint.technique) {
            QueensTechnique.KNOCKOUT -> {
                val u = hint.units.firstOrNull()?.let { formatUnit(it) } ?: ""
                context.getString(R.string.queens_hint_knockout, u)
            }
            QueensTechnique.CONFINED -> {
                val u1 = hint.units.getOrNull(0)?.let { formatUnit(it) } ?: ""
                val u2 = hint.units.getOrNull(1)?.let { formatUnit(it) } ?: ""
                context.getString(R.string.queens_hint_confined, u1, u2)
            }
            QueensTechnique.N_IN_N -> context.getString(R.string.queens_hint_n_in_n)
            QueensTechnique.HYPOTHETICAL -> {
                val u = hint.units.firstOrNull()?.let { formatUnit(it) } ?: ""
                context.getString(R.string.queens_hint_hypothetical, u)
            }
            else -> context.getString(R.string.queens_hint_knockout, "")
        }
    }
}

/** Toques cíclicos vacío -> X -> reina -> vacío; arrastre para marcar múltiples X; opción de auto-marcado activa por defecto; sistema de pistas de dos pasos. */
@Composable
fun QueensBoard(
    session: QueensSession,
    revision: Int,
    act: Act,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    hint: QueensHint? = null,
    onApplyHint: ((Boolean) -> Unit)? = null,
    onDismissHint: (() -> Unit)? = null,
) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision
    val puzzle = session.level.puzzle
    val n = puzzle.size
    val regions = puzzle.regions
    val dark = isSystemInDarkTheme()
    val line = MaterialTheme.colorScheme.onSurface
    val bad = session.conflicts

    // Ayuda de auto-marcado de X activa por defecto
    var autoMark by rememberSaveable { mutableStateOf(true) }

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
        val label = when (state) {
            QueensSession.QUEEN -> "reina"
            QueensSession.MARK -> "marcada con X"
            else -> "vacía"
        }

        // Resaltados según la pista activa
        val isHintWrong = hint is QueensHint.Wrong && i in hint.cells
        val isHintPlaceTarget = hint is QueensHint.PlaceQueen && i == hint.cell
        val isHintEliminateTarget = hint is QueensHint.Eliminate && i in hint.cells
        val isHintCause = hint != null && i in hint.cause
        val isUnitHighlighted = hint?.units?.any { u ->
            when (u.kind) {
                UnitKind.REGION -> reg == u.index
                UnitKind.ROW -> r == u.index
                UnitKind.COLUMN -> c == u.index
            }
        } == true

        val hintBgColor = when {
            isHintWrong -> Color.Red.copy(alpha = 0.4f)
            isHintPlaceTarget -> Color(0xFFFFD54F).copy(alpha = 0.55f)
            isHintEliminateTarget && state == QueensSession.EMPTY -> Color(0xFFFFAB91).copy(alpha = 0.45f)
            isHintEliminateTarget && state == QueensSession.MARK -> Color(0xFF81C784).copy(alpha = 0.25f)
            isHintCause -> Color(0xFFFFF59D).copy(alpha = 0.5f)
            isUnitHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            i == highlight -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
            i in bad -> Color.Red.copy(alpha = 0.35f)
            else -> Color.Transparent
        }

        val hintModifier = when {
            isHintWrong -> Modifier.border(2.dp, Color.Red, RoundedCornerShape(2.dp))
            isHintPlaceTarget -> Modifier.border(2.5.dp, Color(0xFFFFA000), RoundedCornerShape(2.dp))
            isHintEliminateTarget && state == QueensSession.EMPTY -> Modifier.border(1.5.dp, Color(0xFFE64A19).copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            else -> Modifier
        }

        Box(
            Modifier.fillMaxSize()
                .background(regionColor(reg, dark))
                .background(hintBgColor)
                .then(hintModifier)
                .drawBehind {
                    val thin = 0.5.dp.toPx()
                    val thick = 2.5.dp.toPx()
                    val w = size.width
                    val h = size.height

                    fun edge(a: Offset, b: Offset, differs: Boolean) =
                        drawLine(line, a, b, strokeWidth = if (differs) thick else thin)

                    edge(Offset(0f, 0f), Offset(w, 0f), r == 0 || regions[i - n] != reg)
                    edge(Offset(0f, h), Offset(w, h), r == n - 1 || regions[i + n] != reg)
                    edge(Offset(0f, 0f), Offset(0f, h), c == 0 || regions[i - 1] != reg)
                    edge(Offset(w, 0f), Offset(w, h), c == n - 1 || regions[i + 1] != reg)
                }
                .semantics {
                    contentDescription = buildString {
                        append("Fila ${r + 1}, columna ${c + 1}, región ${reg + 1}: $label")
                        if (isHintWrong) append(" (Error detectado por pista)")
                        if (isHintPlaceTarget) append(" (Pista: colocar corona aquí)")
                        if (isHintEliminateTarget && state == QueensSession.EMPTY) append(" (Pista: marcar X aquí)")
                        if (isHintEliminateTarget && state == QueensSession.MARK) append(" (Marcada según pista)")
                        if (isHintCause) append(" (Causa de la pista)")
                    }
                    onClick(label = "Cambiar casilla") {
                        act { session.tap(i, autoMark) }
                        true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val symbolColor = if (dark) Color.White else Color.Black
            when {
                state == QueensSession.QUEEN -> Text("♛", fontSize = (cs.value * 0.65f).sp, color = symbolColor)
                state == QueensSession.MARK -> Text("✕", fontSize = (cs.value * 0.4f).sp, color = symbolColor.copy(alpha = 0.6f))
                isHintPlaceTarget -> Text("♛", fontSize = (cs.value * 0.65f).sp, color = Color(0xFFF57F17).copy(alpha = 0.85f))
                isHintEliminateTarget -> Text("✕", fontSize = (cs.value * 0.4f).sp, color = Color.Red.copy(alpha = 0.7f))
            }
        }
    }

    // Tarjeta explicativa de la pista (Paso 1: visualización y explicación; Paso 2: aplicación)
    if (hint != null) {
        Spacer(Modifier.height(10.dp))
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onApplyHint?.invoke(autoMark) },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (hint is QueensHint.Wrong) Icons.Default.Warning else Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if (hint is QueensHint.Wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val remainingText = if (hint is QueensHint.Eliminate) {
                        val remaining = hint.cells.count { session.state[it] != QueensSession.MARK }
                        if (remaining in 1 until hint.cells.size) " ($remaining restantes)" else ""
                    } else ""
                    Text(
                        text = formatQueensHint(hint) + remainingText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onApplyHint?.invoke(autoMark) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(stringResource(R.string.queens_hint_apply))
                }
                IconButton(onClick = { onDismissHint?.invoke() }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.queens_hint_dismiss))
                }
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
