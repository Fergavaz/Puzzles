package com.tuapp.puzzles.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.puzzles.R
import com.tuapp.puzzles.engine.session.TangoSession
import com.tuapp.puzzles.engine.tango.EMPTY
import com.tuapp.puzzles.engine.tango.LineKind
import com.tuapp.puzzles.engine.tango.LineRef
import com.tuapp.puzzles.engine.tango.MOON
import com.tuapp.puzzles.engine.tango.SUN
import com.tuapp.puzzles.engine.tango.TangoHint
import com.tuapp.puzzles.engine.tango.TangoTechnique
import kotlinx.coroutines.delay

@Composable
fun formatTangoHint(hint: TangoHint): String {
    val context = LocalContext.current
    fun formatLine(l: LineRef): String = when (l.kind) {
        LineKind.ROW -> context.getString(R.string.unit_row, l.index + 1)
        LineKind.COLUMN -> context.getString(R.string.unit_col, l.index + 1)
    }
    fun symbolName(s: Int, plural: Boolean = false): String = when (s) {
        SUN -> context.getString(if (plural) R.string.symbol_suns else R.string.symbol_sun)
        else -> context.getString(if (plural) R.string.symbol_moons else R.string.symbol_moon)
    }

    return when (hint) {
        is TangoHint.Wrong -> context.getString(R.string.tango_hint_wrong_cell)
        is TangoHint.Place -> when (hint.technique) {
            TangoTechnique.SIGN -> {
                val c = hint.constraint
                if (c != null && c.equal) {
                    context.getString(R.string.tango_hint_sign_equal, symbolName(hint.symbol))
                } else {
                    val otherSym = if (hint.symbol == SUN) MOON else SUN
                    context.getString(R.string.tango_hint_sign_diff, symbolName(otherSym), symbolName(hint.symbol))
                }
            }
            TangoTechnique.PAIR -> {
                val otherSym = if (hint.symbol == SUN) MOON else SUN
                context.getString(R.string.tango_hint_pair, symbolName(otherSym, plural = true), symbolName(hint.symbol))
            }
            TangoTechnique.SANDWICH -> {
                val otherSym = if (hint.symbol == SUN) MOON else SUN
                context.getString(R.string.tango_hint_sandwich, symbolName(otherSym, plural = true), symbolName(hint.symbol))
            }
            TangoTechnique.COUNT -> {
                val lineStr = hint.lines.firstOrNull()?.let { formatLine(it) } ?: ""
                val otherSym = if (hint.symbol == SUN) MOON else SUN
                context.getString(R.string.tango_hint_count, lineStr, symbolName(otherSym, plural = true), symbolName(hint.symbol, plural = true))
            }
            TangoTechnique.LINE_CHECK -> {
                val lineStr = hint.lines.firstOrNull()?.let { formatLine(it) } ?: ""
                context.getString(R.string.tango_hint_line_check, lineStr, symbolName(hint.symbol))
            }
            TangoTechnique.HYPOTHETICAL -> {
                val lineStr = hint.lines.firstOrNull()?.let { formatLine(it) } ?: ""
                val otherSym = if (hint.symbol == SUN) MOON else SUN
                context.getString(R.string.tango_hint_hypothetical, symbolName(otherSym), lineStr, symbolName(hint.symbol))
            }
            TangoTechnique.SOLUTION_FALLBACK -> {
                context.getString(R.string.tango_hint_fallback, symbolName(hint.symbol))
            }
            else -> context.getString(R.string.tango_hint_fallback, symbolName(hint.symbol))
        }
    }
}

/** Toques cíclicos vacío -> sol -> luna -> vacío; los signos = y × se dibujan entre celdas; sistema de pistas de dos pasos. */
@Composable
fun TangoBoard(
    session: TangoSession,
    revision: Int,
    act: Act,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    hint: TangoHint? = null,
    onApplyHint: (() -> Unit)? = null,
    onDismissHint: (() -> Unit)? = null,
) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision
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

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SquareGrid(
            n,
            overlay = { cs ->
                val baseBadge = cs * 0.38f
                for (k in session.level.puzzle.constraints) {
                    val isConstraintHighlighted = hint is TangoHint.Place && hint.constraint == k
                    val badge = if (isConstraintHighlighted) baseBadge * 1.25f else baseBadge
                    val ra = k.a / n; val ca = k.a % n; val rb = k.b / n; val cb = k.b % n
                    val x = cs * ((ca + cb) / 2f + 0.5f) - badge / 2
                    val y = cs * ((ra + rb) / 2f + 0.5f) - badge / 2

                    val badgeBg = if (isConstraintHighlighted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                    val badgeBorder = if (isConstraintHighlighted) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }

                    Box(
                        Modifier.offset(x, y).size(badge).clip(CircleShape)
                            .background(badgeBg)
                            .then(badgeBorder)
                            .semantics {
                                contentDescription = if (k.equal) "Las dos celdas son iguales" else "Las dos celdas son opuestas"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (k.equal) "=" else "×",
                            fontSize = (badge.value * 0.8f).sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConstraintHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified,
                        )
                    }
                }
            },
        ) { i, cs ->
            val r = i / n
            val c = i % n

            val isHintWrong = hint is TangoHint.Wrong && i in hint.cells
            val isHintTarget = hint is TangoHint.Place && i == hint.cell
            val isHintCause = hint != null && i in hint.cause
            val isLineHighlighted = hint?.lines?.any { l ->
                (l.kind == LineKind.ROW && r == l.index) || (l.kind == LineKind.COLUMN && c == l.index)
            } == true

            TangoCell(
                i = i,
                n = n,
                cs = cs,
                value = cells[i],
                given = session.isGiven(i),
                bad = i in visibleBad,
                isHighlight = i == highlight,
                isHintWrong = isHintWrong,
                isHintTarget = isHintTarget,
                isHintCause = isHintCause,
                isLineHighlighted = isLineHighlighted,
                hintGhostSymbol = if (isHintTarget) (hint as TangoHint.Place).symbol else null,
            ) {
                act { session.tap(i) }
                lastTappedCell = i
                suppressUntil = System.currentTimeMillis() + 450L
                debounceTick++
            }
        }

        if (hint != null) {
            Spacer(Modifier.height(10.dp))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onApplyHint?.invoke() },
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (hint is TangoHint.Wrong) Icons.Default.Warning else Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (hint is TangoHint.Wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatTangoHint(hint),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onApplyHint?.invoke() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(stringResource(R.string.tango_hint_apply))
                    }
                    IconButton(onClick = { onDismissHint?.invoke() }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.tango_hint_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun TangoCell(
    i: Int,
    n: Int,
    cs: Dp,
    value: Int,
    given: Boolean,
    bad: Boolean,
    isHighlight: Boolean,
    isHintWrong: Boolean,
    isHintTarget: Boolean,
    isHintCause: Boolean,
    isLineHighlighted: Boolean,
    hintGhostSymbol: Int?,
    onClick: () -> Unit,
) {
    val label = when (value) { SUN -> "sol"; MOON -> "luna"; else -> "vacía" }

    val bg = when {
        isHintWrong -> Color.Red.copy(alpha = 0.35f)
        isHintTarget -> Color(0xFFFFD54F).copy(alpha = 0.55f)
        isHintCause -> Color(0xFFFFF59D).copy(alpha = 0.5f)
        isLineHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        bad -> MaterialTheme.colorScheme.errorContainer
        isHighlight -> MaterialTheme.colorScheme.tertiaryContainer
        given -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val highlightBorder = when {
        isHintWrong -> Modifier.border(2.dp, Color.Red, RoundedCornerShape(2.dp))
        isHintTarget -> Modifier.border(2.5.dp, Color(0xFFFFA000), RoundedCornerShape(2.dp))
        isHighlight -> Modifier.border(2.5.dp, MaterialTheme.colorScheme.tertiary)
        else -> Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline)
    }

    Box(
        Modifier.fillMaxSize().padding(1.dp)
            .background(bg)
            .then(highlightBorder)
            .clickable(enabled = !given, onClickLabel = "Cambiar símbolo", onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append("Fila ${i / n + 1}, columna ${i % n + 1}: $label")
                    if (given) append(", fija")
                    if (isHintWrong) append(" (Error detectado por pista)")
                    if (isHintTarget) append(" (Pista: colocar símbolo aquí)")
                    if (isHintCause) append(" (Causa de la pista)")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val text = when {
            value == SUN -> "☀️"
            value == MOON -> "🌙"
            hintGhostSymbol == SUN -> "☀️"
            hintGhostSymbol == MOON -> "🌙"
            else -> ""
        }
        val alpha = if (value == EMPTY && hintGhostSymbol != null) 0.55f else 1.0f

        Text(
            text,
            fontSize = (cs.value * 0.5f).sp,
            color = Color.Unspecified.copy(alpha = alpha),
        )
    }
}
