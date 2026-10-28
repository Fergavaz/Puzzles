package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.tuapp.puzzles.engine.session.ZipSession
import kotlin.math.max

/**
 * Trazado continuo por arrastre. Se interpola el movimiento del dedo para no saltarse celdas
 * cuando el gesto es rápido; volver sobre los propios pasos retrocede el camino.
 */
@Composable
fun ZipBoard(
    session: ZipSession,
    revision: Int,
    act: Act,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision
    val puzzle = session.level.puzzle
    val w = puzzle.width
    val h = puzzle.height
    val path = session.state
    val tm = rememberTextMeasurer()
    val cs = MaterialTheme.colorScheme
    val gridColor = cs.outline
    val bg = cs.surfaceVariant
    val pathColor = cs.primary
    val dotColor = cs.onSurface
    val dotText = cs.surface

    Canvas(
        modifier.fillMaxWidth().aspectRatio(w.toFloat() / h)
            .semantics {
                contentDescription = "Tablero Zip de $w por $h. Camino trazado: ${path.size} de ${w * h} celdas. " +
                    "Números en las celdas: " + puzzle.numbers.withIndex().filter { it.value > 0 }
                        .joinToString { "${it.value} en fila ${it.index / w + 1} columna ${it.index % w + 1}" }
            }
            .pointerInput(session) {
                awaitEachGesture {
                    val cell = size.width / w.toFloat()
                    fun cellAt(o: Offset): Int? {
                        val c = (o.x / cell).toInt(); val r = (o.y / cell).toInt()
                        return if (o.x >= 0 && o.y >= 0 && c in 0 until w && r in 0 until h) r * w + c else null
                    }
                    val down = awaitFirstDown()
                    act { session.beginStroke() }
                    try {
                        var last = down.position
                        cellAt(last)?.let { c -> act { session.drag(c) } }
                        drag(down.id) { change ->
                            val cur = change.position
                            val steps = max(1, ((cur - last).getDistance() / (cell / 4f)).toInt())
                            for (s in 1..steps) {
                                val p = last + (cur - last) * (s / steps.toFloat())
                                cellAt(p)?.let { c -> act { session.drag(c) } }
                            }
                            last = cur
                            change.consume()
                        }
                    } finally {
                        act { session.endStroke() }
                    }
                }
            },
    ) {
        val cell = size.width / w
        drawRect(bg, size = size)
        // Resaltado de pista
        if (highlight != null && highlight in 0 until (w * h)) {
            val hx = (highlight % w) * cell
            val hy = (highlight / w) * cell
            drawRect(cs.tertiary.copy(alpha = 0.35f), Offset(hx, hy), Size(cell, cell))
            drawRect(cs.tertiary, Offset(hx, hy), Size(cell, cell), style = Stroke(3f))
        }
        // Celdas ya recorridas
        for (c in path) drawRect(pathColor.copy(alpha = 0.18f), Offset((c % w) * cell, (c / w) * cell), Size(cell, cell))
        // Rejilla
        for (i in 0..w) drawLine(gridColor, Offset(i * cell, 0f), Offset(i * cell, h * cell), 1f)
        for (j in 0..h) drawLine(gridColor, Offset(0f, j * cell), Offset(w * cell, j * cell), 1f)
        // Muros: líneas gruesas entre celdas
        for (e in puzzle.walls) {
            val rb = e.b / w; val cb = e.b % w
            if (e.b == e.a + 1) drawLine(dotColor, Offset(cb * cell, rb * cell), Offset(cb * cell, (rb + 1) * cell), cell * 0.12f, StrokeCap.Round)
            else drawLine(dotColor, Offset(cb * cell, rb * cell), Offset((cb + 1) * cell, rb * cell), cell * 0.12f, StrokeCap.Round)
        }
        // Camino
        fun center(c: Int) = Offset((c % w + 0.5f) * cell, (c / w + 0.5f) * cell)
        if (path.size > 1) {
            val p = Path().apply { moveTo(center(path[0]).x, center(path[0]).y); path.drop(1).forEach { lineTo(center(it).x, center(it).y) } }
            drawPath(p, pathColor, style = Stroke(cell * 0.32f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (path.isNotEmpty()) drawCircle(pathColor, cell * 0.2f, center(path.last()))
        // Números
        val style = TextStyle(fontWeight = FontWeight.Bold, color = dotText)
        for ((i, n) in puzzle.numbers.withIndex()) {
            if (n == 0) continue
            drawCircle(if (i in path) pathColor else dotColor, cell * 0.27f, center(i))
            centeredText(tm, "$n", center(i), style.copy(fontSize = (cell * 0.3f).toSp()))
        }
    }
}
