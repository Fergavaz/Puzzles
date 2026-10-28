package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.tuapp.puzzles.engine.patches.PatchType
import com.tuapp.puzzles.engine.patches.Rect as PatchRect
import com.tuapp.puzzles.engine.session.PatchesSession
import kotlinx.coroutines.delay

/**
 * Se arrastra entre dos esquinas para dibujar un rectángulo que contenga una pista.
 * Las pistas 1x1 están marcadas permanentemente.
 * Doble toque sobre un parche lo elimina (se resalta en rojo tras el primer toque).
 * Arrastrar desde cualquier casilla de un parche redimensiona desde la esquina opuesta.
 */
@Composable
fun PatchesBoard(
    session: PatchesSession,
    revision: Int,
    act: Act,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision
    val puzzle = session.level.puzzle
    val n = puzzle.size
    val dark = isSystemInDarkTheme()
    val tm = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    var dragRect by remember { mutableStateOf<PatchRect?>(null) }
    var selectedPatchForDeletion by remember { mutableStateOf<Int?>(null) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(selectedPatchForDeletion, lastTapTime) {
        if (selectedPatchForDeletion != null) {
            delay(750)
            selectedPatchForDeletion = null
        }
    }

    Canvas(
        modifier.fillMaxWidth().aspectRatio(1f)
            .semantics {
                contentDescription = "Tablero Patches de $n por $n con ${puzzle.clues.size} pistas. " +
                    "${session.state.size} parches colocados."
            }
            .pointerInput(session) {
                awaitEachGesture {
                    val cell = size.width / n.toFloat()
                    fun cellAt(o: Offset): Int? {
                        val c = (o.x / cell).toInt(); val r = (o.y / cell).toInt()
                        return if (o.x >= 0 && o.y >= 0 && c in 0 until n && r in 0 until n) r * n + c else null
                    }
                    val down = awaitFirstDown()
                    val startPos = down.position
                    val start = cellAt(startPos) ?: return@awaitEachGesture
                    val startR = start / n
                    val startC = start % n
                    val existingPl = session.placementAt(start)
                    val isFixed1x1 = existingPl != null && existingPl.clueIndex in session.fixed1x1

                    // Si se empieza a arrastrar desde un parche existente (no 1x1), fijamos la esquina opuesta para redimensionar
                    val anchorR: Int
                    val anchorC: Int
                    if (existingPl != null && !isFixed1x1) {
                        val r0 = existingPl.rect.row
                        val r1 = existingPl.rect.row + existingPl.rect.height - 1
                        val c0 = existingPl.rect.col
                        val c1 = existingPl.rect.col + existingPl.rect.width - 1
                        anchorR = if (kotlin.math.abs(startR - r0) < kotlin.math.abs(startR - r1)) r1 else r0
                        anchorC = if (kotlin.math.abs(startC - c0) < kotlin.math.abs(startC - c1)) c1 else c0
                    } else {
                        anchorR = startR
                        anchorC = startC
                    }

                    var isDragging = false
                    val slop = viewConfiguration.touchSlop

                    try {
                        drag(down.id) { change ->
                            val dist = (change.position - startPos).getDistance()
                            if (dist > slop) {
                                isDragging = true
                            }
                            if (isDragging && !isFixed1x1) {
                                val cur = cellAt(change.position)
                                if (cur != null) {
                                    dragRect = PatchRect.fromCorners(anchorR, anchorC, cur / n, cur % n)
                                }
                            }
                            change.consume()
                        }
                    } finally {
                        val rect = dragRect
                        dragRect = null
                        if (isDragging && rect != null && !isFixed1x1) {
                            act {
                                if (existingPl != null) {
                                    if (!session.commit(existingPl.clueIndex, rect)) {
                                        session.commitRect(rect)
                                    }
                                } else {
                                    session.commitRect(rect)
                                }
                            }
                        } else if (!isDragging && existingPl != null && !isFixed1x1) {
                            val now = System.currentTimeMillis()
                            if (selectedPatchForDeletion == existingPl.clueIndex && (now - lastTapTime) < 800L) {
                                act { session.remove(existingPl.clueIndex) }
                                selectedPatchForDeletion = null
                                lastTapTime = 0L
                            } else {
                                selectedPatchForDeletion = existingPl.clueIndex
                                lastTapTime = now
                            }
                        }
                    }
                }
            },
    ) {
        val cell = size.width / n
        drawRect(scheme.surface, size = size)
        // Casillas de pistas coloreadas con el color del parche desde el principio
        for ((ci, clue) in puzzle.clues.withIndex()) {
            val cr = clue.cell / n
            val cc = clue.cell % n
            drawRect(regionColor(ci, dark), Offset(cc * cell, cr * cell), Size(cell, cell))
        }
        // Parches colocados
        for (pl in session.state) {
            val r = pl.rect
            drawRect(regionColor(pl.clueIndex, dark), Offset(r.col * cell, r.row * cell), Size(r.width * cell, r.height * cell))
            val isHl = pl.clueIndex == highlight
            val isPendingDelete = pl.clueIndex == selectedPatchForDeletion
            val strokeColor = when {
                isPendingDelete -> scheme.error
                isHl -> scheme.tertiary
                else -> scheme.onSurface
            }
            val strokeWidth = if (isPendingDelete || isHl) 5f else 2.5f
            drawRect(strokeColor, Offset(r.col * cell, r.row * cell), Size(r.width * cell, r.height * cell), style = Stroke(strokeWidth))
        }
        // Rejilla
        for (i in 0..n) {
            drawLine(scheme.outline.copy(alpha = 0.5f), Offset(i * cell, 0f), Offset(i * cell, size.height), 1f)
            drawLine(scheme.outline.copy(alpha = 0.5f), Offset(0f, i * cell), Offset(size.width, i * cell), 1f)
        }
        // Pistas: símbolo de forma + número (si lo tiene)
        val style = TextStyle(fontSize = (cell * 0.3f).toSp(), fontWeight = FontWeight.Bold, color = scheme.onSurface)
        for (clue in puzzle.clues) {
            val symbol = when (clue.type) {
                PatchType.SQUARE -> "■"; PatchType.WIDE -> "↔"; PatchType.TALL -> "↕"; PatchType.FREE -> "┅"
            }
            val label = symbol + (clue.area?.let { " $it" } ?: "")
            centeredText(tm, label, Offset((clue.cell % n + 0.5f) * cell, (clue.cell / n + 0.5f) * cell), style)
        }
        // Vista previa del rectángulo que se arrastra
        dragRect?.let { r ->
            val pv = session.previewRect(r)
            val color = if (pv.valid) Color(0xFF2E9E4F) else Color(0xFFD93025)
            drawRect(color.copy(alpha = 0.18f), Offset(r.col * cell, r.row * cell), Size(r.width * cell, r.height * cell))
            drawRect(color, Offset(r.col * cell, r.row * cell), Size(r.width * cell, r.height * cell), style = Stroke(4f))
            centeredText(
                tm, "${pv.cells}",
                Offset((r.col + r.width / 2f) * cell, (r.row + 0.3f) * cell),
                style.copy(color = color, fontSize = (cell * 0.38f).toSp()),
            )
        }
    }
}
