package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.puzzles.engine.session.TangoSession
import com.tuapp.puzzles.engine.tango.MOON
import com.tuapp.puzzles.engine.tango.SUN
import kotlinx.coroutines.delay

/** Toques cíclicos vacío -> sol -> luna -> vacío; los signos = y × se dibujan entre celdas. */
@Composable
fun TangoBoard(session: TangoSession, revision: Int, act: Act, modifier: Modifier = Modifier, highlight: Int? = null) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision   // fuerza la recomposición cuando cambia la sesión
    val n = session.level.puzzle.size
    val cells = session.state
    val bad = session.conflicts

    var lastTappedCell by remember { mutableStateOf<Int?>(null) }
    var suppressUntil by remember { mutableLongStateOf(0L) }
    var debounceTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(debounceTick, suppressUntil) {
        val remaining = suppressUntil - System.currentTimeMillis()
        if (remaining > 0) {
            delay(remaining + 10)
            lastTappedCell = null
        }
    }

    val isDebouncing = lastTappedCell != null && System.currentTimeMillis() < suppressUntil
    val visibleBad = if (isDebouncing && lastTappedCell != null) {
        val tapped = lastTappedCell!!
        val tappedR = tapped / n
        val tappedC = tapped % n
        bad.filter { cell ->
            cell != tapped && cell / n != tappedR && cell % n != tappedC
        }.toSet()
    } else {
        bad
    }

    SquareGrid(
        n, modifier,
        overlay = { cs ->
            val badge = cs * 0.38f
            for (k in session.level.puzzle.constraints) {
                val ra = k.a / n; val ca = k.a % n; val rb = k.b / n; val cb = k.b % n
                val x = cs * ((ca + cb) / 2f + 0.5f) - badge / 2
                val y = cs * ((ra + rb) / 2f + 0.5f) - badge / 2
                Box(
                    Modifier.offset(x, y).size(badge).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .semantics { contentDescription = if (k.equal) "Las dos celdas son iguales" else "Las dos celdas son opuestas" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (k.equal) "=" else "×", fontSize = (badge.value * 0.8f).sp, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { i, cs ->
        TangoCell(i, n, cs, cells[i], session.isGiven(i), i in visibleBad, i == highlight) {
            act { session.tap(i) }
            lastTappedCell = i
            suppressUntil = System.currentTimeMillis() + 450L
            debounceTick++
        }
    }
}

@Composable
private fun TangoCell(i: Int, n: Int, cs: Dp, value: Int, given: Boolean, bad: Boolean, isHighlight: Boolean, onClick: () -> Unit) {
    val label = when (value) { SUN -> "sol"; MOON -> "luna"; else -> "vacía" }
    val bg = when {
        bad -> MaterialTheme.colorScheme.errorContainer
        isHighlight -> MaterialTheme.colorScheme.tertiaryContainer
        given -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val highlightBorder = if (isHighlight) Modifier.border(2.5.dp, MaterialTheme.colorScheme.tertiary) else Modifier
    Box(
        Modifier.fillMaxSize().padding(1.dp)
            .then(highlightBorder)
            .background(bg)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .clickable(enabled = !given, onClickLabel = "Cambiar símbolo", onClick = onClick)
            .semantics {
                contentDescription = "Fila ${i / n + 1}, columna ${i % n + 1}: $label" + if (given) ", fija" else ""
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            when (value) { SUN -> "☀️"; MOON -> "🌙"; else -> "" },
            fontSize = (cs.value * 0.5f).sp,
        )
    }
}
