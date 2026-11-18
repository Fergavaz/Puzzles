package com.tuapp.puzzles.ui.boards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuapp.puzzles.R
import com.tuapp.puzzles.engine.session.SumbreakSession
import com.tuapp.puzzles.engine.sumbreak.ModifierOp
import com.tuapp.puzzles.engine.sumbreak.SumbreakHint
import com.tuapp.puzzles.engine.sumbreak.SumbreakTile
import kotlin.math.abs

/** Paleta de colores atractiva según el valor del número. */
private fun numberColor(value: Int): Color = when (value) {
    1 -> Color(0xFFE0E0E0)
    2 -> Color(0xFFBBDEFB)
    3 -> Color(0xFFFFE0B2)
    4 -> Color(0xFFC8E6C9)
    5 -> Color(0xFFFFCDD2)
    6 -> Color(0xFFB2DFDB)
    8 -> Color(0xFFFFF9C4)
    10 -> Color(0xFFD1C4E9)
    12 -> Color(0xFFDCEDC8)
    16 -> Color(0xFFFFCCBC)
    20 -> Color(0xFFB3E5FC)
    24 -> Color(0xFFE1BEE7)
    32 -> Color(0xFFFFAB91)
    else -> Color.hsv((abs(value.hashCode()) * 67f) % 360f, 0.35f, 0.95f)
}

@Composable
fun SumbreakBoard(
    session: SumbreakSession,
    revision: Int,
    act: Act,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
    hint: SumbreakHint? = null,
    onApplyHint: (() -> Unit)? = null,
    onDismissHint: (() -> Unit)? = null,
) {
    @Suppress("UNUSED_VARIABLE") val refresh = revision
    val n = session.size
    val tiles = session.state

    var selectedCell by remember { mutableIntStateOf(-1) }
    var draggingFrom by remember { mutableIntStateOf(-1) }
    var dragTarget by remember { mutableIntStateOf(-1) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(session, revision) {
                    awaitEachGesture {
                        val cellSizePx = size.width / n.toFloat()
                        fun cellAt(pos: Offset): Int? {
                            val c = (pos.x / cellSizePx).toInt()
                            val r = (pos.y / cellSizePx).toInt()
                            return if (c in 0 until n && r in 0 until n && pos.x >= 0 && pos.y >= 0) r * n + c else null
                        }

                        val down = awaitFirstDown()
                        val startCell = cellAt(down.position)

                        if (startCell != null && tiles[startCell] !is SumbreakTile.Empty) {
                            draggingFrom = startCell
                            var currentTarget: Int? = null

                            try {
                                drag(down.id) { change ->
                                    val curCell = cellAt(change.position)
                                    if (curCell != null && curCell != startCell && session.canMove(startCell, curCell)) {
                                        currentTarget = curCell
                                        dragTarget = curCell
                                    } else {
                                        currentTarget = null
                                        dragTarget = -1
                                    }
                                    change.consume()
                                }
                            } finally {
                                val target = currentTarget
                                if (target != null && session.canMove(startCell, target)) {
                                    act { session.makeMove(startCell, target) }
                                    selectedCell = -1
                                }
                                draggingFrom = -1
                                dragTarget = -1
                            }
                        }
                    }
                },
        ) {
            val cs = maxWidth / n

            for (i in 0 until n * n) {
                val col = i % n
                val row = i / n
                val tile = tiles[i]

                val isSelected = (selectedCell == i) || (draggingFrom == i)
                val isValidTarget = (selectedCell >= 0 && session.canMove(selectedCell, i)) ||
                    (draggingFrom >= 0 && session.canMove(draggingFrom, i))
                val isDragTarget = (dragTarget == i)

                val isHintFrom = hint is SumbreakHint.SuggestMove && hint.move.from == i
                val isHintTo = hint is SumbreakHint.SuggestMove && hint.move.to == i
                val isDeadEndTile = hint is SumbreakHint.DeadEnd && tile !is SumbreakTile.Empty

                Box(
                    modifier = Modifier
                        .offset(cs * col, cs * row)
                        .size(cs)
                        .padding(3.dp),
                ) {
                    SumbreakCellView(
                        tile = tile,
                        cellSize = cs,
                        isSelected = isSelected,
                        isValidTarget = isValidTarget,
                        isDragTarget = isDragTarget,
                        isHintFrom = isHintFrom,
                        isHintTo = isHintTo,
                        isDeadEnd = isDeadEndTile,
                        onClick = {
                            if (selectedCell == -1) {
                                if (tile !is SumbreakTile.Empty) {
                                    selectedCell = i
                                }
                            } else if (selectedCell == i) {
                                selectedCell = -1
                            } else if (session.canMove(selectedCell, i)) {
                                act { session.makeMove(selectedCell, i) }
                                selectedCell = -1
                            } else if (tile !is SumbreakTile.Empty) {
                                selectedCell = i
                            } else {
                                selectedCell = -1
                            }
                        },
                    )
                }
            }
        }

        // 2-Step Hint Card
        if (hint != null) {
            Spacer(Modifier.height(12.dp))
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
                        if (hint is SumbreakHint.DeadEnd) Icons.Default.Warning else Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (hint is SumbreakHint.DeadEnd) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hint.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onApplyHint?.invoke() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            if (hint is SumbreakHint.DeadEnd) {
                                stringResource(R.string.undo)
                            } else {
                                stringResource(R.string.sumbreak_hint_apply)
                            },
                        )
                    }
                    IconButton(onClick = { onDismissHint?.invoke() }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.sumbreak_hint_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun SumbreakCellView(
    tile: SumbreakTile,
    cellSize: Dp,
    isSelected: Boolean,
    isValidTarget: Boolean,
    isDragTarget: Boolean,
    isHintFrom: Boolean,
    isHintTo: Boolean,
    isDeadEnd: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isDragTarget -> 1.08f
            isSelected -> 1.04f
            else -> 1.0f
        },
        label = "scale",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isDragTarget -> Color(0xFF2E7D32) // Dark green
            isValidTarget -> Color(0xFF4CAF50) // Green
            isSelected -> MaterialTheme.colorScheme.primary
            isHintTo -> Color(0xFF2E7D32)
            isHintFrom -> Color(0xFFFFA000) // Amber
            isDeadEnd -> MaterialTheme.colorScheme.error
            else -> Color.Transparent
        },
        label = "border",
    )

    val borderWidth = when {
        isDragTarget || isSelected || isHintTo || isHintFrom -> 3.dp
        isValidTarget -> 2.dp
        else -> 0.dp
    }

    when (tile) {
        is SumbreakTile.Empty -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable(onClick = onClick)
                    .semantics {
                        contentDescription = "Casilla vacía"
                    },
            )
        }

        is SumbreakTile.Number -> {
            val bg = numberColor(tile.value)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .then(
                        if (borderWidth > 0.dp) {
                            Modifier.border(borderWidth, borderColor, RoundedCornerShape(10.dp))
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        },
                    )
                    .clickable(onClick = onClick)
                    .semantics {
                        contentDescription = "Número ${tile.value}" +
                            (if (isSelected) ", seleccionado" else "") +
                            (if (isValidTarget) ", objetivo válido" else "")
                    },
                color = bg,
                shape = RoundedCornerShape(10.dp),
                tonalElevation = if (isSelected || isDragTarget) 6.dp else 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = tile.value.toString(),
                        fontSize = (cellSize.value * 0.42f).coerceIn(16f, 36f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B), // Dark slate for clear contrast on pastel chips
                    )
                }
            }
        }

        is SumbreakTile.Modifier -> {
            val (modBg, modTextColor, modSymbol) = when (tile.op) {
                ModifierOp.ADD -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "+${tile.value}")
                ModifierOp.SUB -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "-${tile.value}")
                ModifierOp.MUL -> Triple(Color(0xFFEDE7F6), Color(0xFF512DA8), "×${tile.value}")
                ModifierOp.WILDCARD -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "★")
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .clip(CircleShape)
                    .background(modBg)
                    .then(
                        if (borderWidth > 0.dp) {
                            Modifier.border(borderWidth, borderColor, CircleShape)
                        } else {
                            Modifier.border(2.dp, modTextColor.copy(alpha = 0.7f), CircleShape)
                        },
                    )
                    .clickable(onClick = onClick)
                    .semantics {
                        contentDescription = "Modificador $modSymbol" +
                            (if (isSelected) ", seleccionado" else "") +
                            (if (isValidTarget) ", objetivo válido" else "")
                    },
                color = modBg,
                shape = CircleShape,
                tonalElevation = if (isSelected || isDragTarget) 6.dp else 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = modSymbol,
                        fontSize = (cellSize.value * 0.38f).coerceIn(14f, 32f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = modTextColor,
                    )
                }
            }
        }
    }
}
