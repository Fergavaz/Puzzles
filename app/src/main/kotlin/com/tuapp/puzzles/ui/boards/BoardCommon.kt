package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Las acciones del jugador se ejecutan a través de este lambda (lo implementa GameViewModel.perform). */
typealias Act = (() -> Unit) -> Unit

/** Cuadrícula n x n cuadrada con celdas posicionadas de forma absoluta (permite superponer adornos). */
@Composable
fun SquareGrid(
    n: Int,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.(cell: Dp) -> Unit = {},
    cell: @Composable (index: Int, size: Dp) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f)) {
        val cs = maxWidth / n
        for (i in 0 until n * n) {
            Box(Modifier.offset(cs * (i % n), cs * (i / n)).size(cs)) { cell(i, cs) }
        }
        overlay(cs)
    }
}

/** Color pastel distinto por región/parche (ángulo áureo para separar tonos vecinos). */
fun regionColor(region: Int, dark: Boolean): Color =
    Color.hsv((region * 137.5f) % 360f, if (dark) 0.5f else 0.35f, if (dark) 0.5f else 0.95f)

fun DrawScope.centeredText(tm: TextMeasurer, text: String, center: Offset, style: TextStyle) {
    val size = tm.measure(text, style).size
    drawText(tm, text, topLeft = Offset(center.x - size.width / 2f, center.y - size.height / 2f), style = style)
}
