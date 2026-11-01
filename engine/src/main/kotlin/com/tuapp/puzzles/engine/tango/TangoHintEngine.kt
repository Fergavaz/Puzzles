package com.tuapp.puzzles.engine.tango

enum class TangoTechnique {
    WRONG_CELL,
    SIGN,
    PAIR,
    SANDWICH,
    COUNT,
    LINE_CHECK,
    HYPOTHETICAL,
    SOLUTION_FALLBACK,
}

enum class LineKind { ROW, COLUMN }

data class LineRef(val kind: LineKind, val index: Int)

sealed interface TangoHint {
    val technique: TangoTechnique
    /** Celdas que justifican la deducción (se resaltan). */
    val cause: Set<Int>
    /** Fila/columna implicada, si procede. */
    val lines: List<LineRef>

    /** Celdas que el jugador ha rellenado mal (no coinciden con la solución). */
    data class Wrong(val cells: Set<Int>) : TangoHint {
        override val technique = TangoTechnique.WRONG_CELL
        override val cause: Set<Int> get() = cells
        override val lines: List<LineRef> = emptyList()
    }

    /** La celda [cell] debe contener [symbol]. */
    data class Place(
        val cell: Int,
        val symbol: Int,
        override val technique: TangoTechnique,
        override val cause: Set<Int>,
        override val lines: List<LineRef> = emptyList(),
        /** Relación =/× implicada (técnica SIGN o cadenas), para dibujarla destacada. */
        val constraint: TangoConstraint? = null,
    ) : TangoHint
}

object TangoHintEngine {

    private fun opp(s: Int): Int = if (s == SUN) MOON else SUN

    /**
     * Calcula la siguiente pista lógica o error para el tablero de Tango.
     * Devuelve null si el tablero ya está resuelto.
     */
    fun nextHint(level: TangoLevel, cells: List<Int>): TangoHint? {
        val puzzle = level.puzzle
        val n = puzzle.size
        val totalCells = n * n
        val givens = puzzle.givens
        val solution = level.solution

        // 0. Si ya está resuelto y coincide con la solución
        if (cells.none { it == EMPTY } && cells == solution) {
            return null
        }

        // 1. Errores primero (WRONG_CELL)
        // Celdas no fijas que no estén vacías y difieran de la solución
        val wrongCells = cells.indices.filter { i ->
            givens[i] == EMPTY && cells[i] != EMPTY && cells[i] != solution[i]
        }.toSet()

        if (wrongCells.isNotEmpty()) {
            return TangoHint.Wrong(wrongCells)
        }

        if (cells.none { it == EMPTY }) {
            return null
        }

        // 2. Deducciones lógicas (T1 a T6)

        // --- T1 · SIGN ---
        // Relación = o × con una celda conocida y la otra vacía.
        val sortedConstraints = puzzle.constraints.sortedWith(compareBy({ it.a }, { it.b }))
        for (k in sortedConstraints) {
            val aVal = cells[k.a]
            val bVal = cells[k.b]
            if (aVal != EMPTY && bVal == EMPTY) {
                val sym = if (k.equal) aVal else opp(aVal)
                return TangoHint.Place(
                    cell = k.b,
                    symbol = sym,
                    technique = TangoTechnique.SIGN,
                    cause = setOf(k.a, k.b),
                    lines = emptyList(),
                    constraint = k,
                )
            } else if (bVal != EMPTY && aVal == EMPTY) {
                val sym = if (k.equal) bVal else opp(bVal)
                return TangoHint.Place(
                    cell = k.a,
                    symbol = sym,
                    technique = TangoTechnique.SIGN,
                    cause = setOf(k.b, k.a),
                    lines = emptyList(),
                    constraint = k,
                )
            }
        }

        // --- T2 · PAIR ---
        // Dos celdas contiguas iguales: la celda siguiente a cada extremo (si está vacía) es opp(s).
        // Filas:
        for (r in 0 until n) {
            for (c in 0 until n - 1) {
                val c1 = r * n + c
                val c2 = r * n + c + 1
                val v = cells[c1]
                if (v != EMPTY && v == cells[c2]) {
                    val o = opp(v)
                    // Extremo izquierdo
                    if (c - 1 >= 0 && cells[r * n + (c - 1)] == EMPTY) {
                        return TangoHint.Place(
                            cell = r * n + (c - 1),
                            symbol = o,
                            technique = TangoTechnique.PAIR,
                            cause = setOf(c1, c2),
                            lines = listOf(LineRef(LineKind.ROW, r)),
                        )
                    }
                    // Extremo derecho
                    if (c + 2 < n && cells[r * n + (c + 2)] == EMPTY) {
                        return TangoHint.Place(
                            cell = r * n + (c + 2),
                            symbol = o,
                            technique = TangoTechnique.PAIR,
                            cause = setOf(c1, c2),
                            lines = listOf(LineRef(LineKind.ROW, r)),
                        )
                    }
                }
            }
        }

        // Columnas:
        for (c in 0 until n) {
            for (r in 0 until n - 1) {
                val c1 = r * n + c
                val c2 = (r + 1) * n + c
                val v = cells[c1]
                if (v != EMPTY && v == cells[c2]) {
                    val o = opp(v)
                    // Extremo superior
                    if (r - 1 >= 0 && cells[(r - 1) * n + c] == EMPTY) {
                        return TangoHint.Place(
                            cell = (r - 1) * n + c,
                            symbol = o,
                            technique = TangoTechnique.PAIR,
                            cause = setOf(c1, c2),
                            lines = listOf(LineRef(LineKind.COLUMN, c)),
                        )
                    }
                    // Extremo inferior
                    if (r + 2 < n && cells[(r + 2) * n + c] == EMPTY) {
                        return TangoHint.Place(
                            cell = (r + 2) * n + c,
                            symbol = o,
                            technique = TangoTechnique.PAIR,
                            cause = setOf(c1, c2),
                            lines = listOf(LineRef(LineKind.COLUMN, c)),
                        )
                    }
                }
            }
        }

        // --- T3 · SANDWICH ---
        // Patrón s _ s: la central es opp(s).
        // Filas:
        for (r in 0 until n) {
            for (c in 0 until n - 2) {
                val c1 = r * n + c
                val mid = r * n + c + 1
                val c2 = r * n + c + 2
                val v = cells[c1]
                if (v != EMPTY && v == cells[c2] && cells[mid] == EMPTY) {
                    return TangoHint.Place(
                        cell = mid,
                        symbol = opp(v),
                        technique = TangoTechnique.SANDWICH,
                        cause = setOf(c1, c2),
                        lines = listOf(LineRef(LineKind.ROW, r)),
                    )
                }
            }
        }

        // Columnas:
        for (c in 0 until n) {
            for (r in 0 until n - 2) {
                val c1 = r * n + c
                val mid = (r + 1) * n + c
                val c2 = (r + 2) * n + c
                val v = cells[c1]
                if (v != EMPTY && v == cells[c2] && cells[mid] == EMPTY) {
                    return TangoHint.Place(
                        cell = mid,
                        symbol = opp(v),
                        technique = TangoTechnique.SANDWICH,
                        cause = setOf(c1, c2),
                        lines = listOf(LineRef(LineKind.COLUMN, c)),
                    )
                }
            }
        }

        // --- T4 · COUNT ---
        // Línea con n/2 celdas de un símbolo s: las vacías son opp(s).
        // Elegir la de menos celdas vacías (a igualdad, índice menor, filas antes que columnas).
        data class CountCandidate(
            val emptyCount: Int,
            val isRow: Boolean,
            val lineIndex: Int,
            val targetCell: Int,
            val symbol: Int,
            val cause: Set<Int>,
        )

        val countCandidates = mutableListOf<CountCandidate>()

        for (r in 0 until n) {
            val lineCells = (0 until n).map { r * n + it }
            val empties = lineCells.filter { cells[it] == EMPTY }
            if (empties.isNotEmpty()) {
                val suns = lineCells.filter { cells[it] == SUN }
                val moons = lineCells.filter { cells[it] == MOON }
                if (suns.size == n / 2) {
                    countCandidates.add(
                        CountCandidate(
                            emptyCount = empties.size,
                            isRow = true,
                            lineIndex = r,
                            targetCell = empties.first(),
                            symbol = MOON,
                            cause = suns.toSet(),
                        )
                    )
                } else if (moons.size == n / 2) {
                    countCandidates.add(
                        CountCandidate(
                            emptyCount = empties.size,
                            isRow = true,
                            lineIndex = r,
                            targetCell = empties.first(),
                            symbol = SUN,
                            cause = moons.toSet(),
                        )
                    )
                }
            }
        }

        for (c in 0 until n) {
            val lineCells = (0 until n).map { it * n + c }
            val empties = lineCells.filter { cells[it] == EMPTY }
            if (empties.isNotEmpty()) {
                val suns = lineCells.filter { cells[it] == SUN }
                val moons = lineCells.filter { cells[it] == MOON }
                if (suns.size == n / 2) {
                    countCandidates.add(
                        CountCandidate(
                            emptyCount = empties.size,
                            isRow = false,
                            lineIndex = c,
                            targetCell = empties.first(),
                            symbol = MOON,
                            cause = suns.toSet(),
                        )
                    )
                } else if (moons.size == n / 2) {
                    countCandidates.add(
                        CountCandidate(
                            emptyCount = empties.size,
                            isRow = false,
                            lineIndex = c,
                            targetCell = empties.first(),
                            symbol = SUN,
                            cause = moons.toSet(),
                        )
                    )
                }
            }
        }

        if (countCandidates.isNotEmpty()) {
            val best = countCandidates.minWithWithOrNull(
                compareBy<CountCandidate> { it.emptyCount }
                    .thenByDescending { it.isRow }
                    .thenBy { it.lineIndex }
            ) ?: countCandidates.first()

            val kind = if (best.isRow) LineKind.ROW else LineKind.COLUMN
            return TangoHint.Place(
                cell = best.targetCell,
                symbol = best.symbol,
                technique = TangoTechnique.COUNT,
                cause = best.cause,
                lines = listOf(LineRef(kind, best.lineIndex)),
            )
        }

        // --- T5 · LINE_CHECK ---
        // Enumerar completaciones válidas para cada fila/columna con celdas vacías.
        data class LineCheckCandidate(
            val validCompletionsCount: Int,
            val isRow: Boolean,
            val lineIndex: Int,
            val targetCell: Int,
            val symbol: Int,
            val cause: Set<Int>,
        )

        fun findLineCheckCandidate(isRow: Boolean, index: Int, currentCells: List<Int>): LineCheckCandidate? {
            val lineIndices = if (isRow) (0 until n).map { index * n + it } else (0 until n).map { it * n + index }
            val empties = lineIndices.filter { currentCells[it] == EMPTY }
            if (empties.isEmpty()) return null

            val knowns = lineIndices.filter { currentCells[it] != EMPTY }.toSet()
            val validCompletions = getValidCompletions(puzzle, lineIndices, currentCells)
            if (validCompletions.isEmpty()) return null

            // Buscar si alguna celda vacía toma el mismo valor en todas las completaciones
            val forcedCells = mutableListOf<Pair<Int, Int>>() // cellGlobal to symbol
            for (cellGlobal in empties) {
                val pos = lineIndices.indexOf(cellGlobal)
                val firstVal = validCompletions.first()[pos]
                if (validCompletions.all { it[pos] == firstVal }) {
                    forcedCells.add(cellGlobal to firstVal)
                }
            }

            if (forcedCells.isNotEmpty()) {
                val (targetCell, sym) = forcedCells.minByOrNull { it.first } ?: forcedCells.first()
                return LineCheckCandidate(
                    validCompletionsCount = validCompletions.size,
                    isRow = isRow,
                    lineIndex = index,
                    targetCell = targetCell,
                    symbol = sym,
                    cause = knowns,
                )
            }
            return null
        }

        val lineCheckCandidates = mutableListOf<LineCheckCandidate>()
        for (r in 0 until n) {
            findLineCheckCandidate(true, r, cells)?.let { lineCheckCandidates.add(it) }
        }
        for (c in 0 until n) {
            findLineCheckCandidate(false, c, cells)?.let { lineCheckCandidates.add(it) }
        }

        if (lineCheckCandidates.isNotEmpty()) {
            val best = lineCheckCandidates.minWithWithOrNull(
                compareBy<LineCheckCandidate> { it.validCompletionsCount }
                    .thenByDescending { it.isRow }
                    .thenBy { it.lineIndex }
            ) ?: lineCheckCandidates.first()

            val kind = if (best.isRow) LineKind.ROW else LineKind.COLUMN
            return TangoHint.Place(
                cell = best.targetCell,
                symbol = best.symbol,
                technique = TangoTechnique.LINE_CHECK,
                cause = best.cause,
                lines = listOf(LineRef(kind, best.lineIndex)),
            )
        }

        // --- T6 · HYPOTHETICAL (profundidad 1) ---
        // Para cada celda vacía c y símbolo s: colocar hipotéticamente y propagar con T1-T5.
        // Si aparece contradicción, c debe ser opp(s).
        for (c in 0 until totalCells) {
            if (cells[c] != EMPTY) continue

            for (s in listOf(SUN, MOON)) {
                val contradiction = testHypothesisContradiction(puzzle, cells, c, s)
                if (contradiction != null) {
                    val row = c / n
                    return TangoHint.Place(
                        cell = c,
                        symbol = opp(s),
                        technique = TangoTechnique.HYPOTHETICAL,
                        cause = setOf(c) + contradiction,
                        lines = listOf(LineRef(LineKind.ROW, row)),
                    )
                }
            }
        }

        // --- RESPALDO · SOLUTION_FALLBACK ---
        val firstEmpty = cells.indices.firstOrNull { cells[it] == EMPTY }
        if (firstEmpty != null) {
            return TangoHint.Place(
                cell = firstEmpty,
                symbol = solution[firstEmpty],
                technique = TangoTechnique.SOLUTION_FALLBACK,
                cause = emptySet(),
                lines = listOf(LineRef(LineKind.ROW, firstEmpty / n)),
            )
        }

        return null
    }

    /** Enumerar todas las completaciones de una línea que respetan reglas internas y relaciones con celdas conocidas. */
    internal fun getValidCompletions(puzzle: TangoPuzzle, lineIndices: List<Int>, currentCells: List<Int>): List<IntArray> {
        val n = puzzle.size
        val emptyPosList = lineIndices.indices.filter { currentCells[lineIndices[it]] == EMPTY }
        val results = mutableListOf<IntArray>()

        val template = IntArray(n) { currentCells[lineIndices[it]] }
        val totalEmpty = emptyPosList.size

        fun isValidLine(lineArr: IntArray): Boolean {
            var sunCount = 0
            var moonCount = 0
            for (i in 0 until n) {
                if (lineArr[i] == SUN) sunCount++ else if (lineArr[i] == MOON) moonCount++
            }
            if (sunCount != n / 2 || moonCount != n / 2) return false

            for (i in 0 until n - 2) {
                if (lineArr[i] == lineArr[i + 1] && lineArr[i] == lineArr[i + 2]) return false
            }

            for (pos in 0 until n) {
                val cellGlobal = lineIndices[pos]
                puzzle.byCell[cellGlobal]?.forEach { k ->
                    val otherGlobal = if (k.a == cellGlobal) k.b else k.a
                    val otherPos = lineIndices.indexOf(otherGlobal)
                    if (otherPos != -1) {
                        val v1 = lineArr[pos]
                        val v2 = lineArr[otherPos]
                        if ((v1 == v2) != k.equal) return false
                    } else {
                        val otherVal = currentCells[otherGlobal]
                        if (otherVal != EMPTY && ((lineArr[pos] == otherVal) != k.equal)) return false
                    }
                }
            }
            return true
        }

        fun generate(idx: Int) {
            if (idx == totalEmpty) {
                if (isValidLine(template)) {
                    results.add(template.clone())
                }
                return
            }
            val pos = emptyPosList[idx]
            template[pos] = SUN
            generate(idx + 1)
            template[pos] = MOON
            generate(idx + 1)
            template[pos] = EMPTY
        }

        generate(0)
        return results
    }

    /** Simula colocar el símbolo [s] en [c] y propaga con deducciones directas (T1-T5). Devuelve la celda en contradicción si existe. */
    private fun testHypothesisContradiction(puzzle: TangoPuzzle, cells: List<Int>, c: Int, s: Int): Set<Int>? {
        val n = puzzle.size
        val sim = cells.toIntArray()
        sim[c] = s

        if (!TangoRules.consistentAt(puzzle, sim, c)) {
            return setOf(c)
        }

        while (true) {
            // 1. ¿Alguna celda viola consistencia?
            for (i in sim.indices) {
                if (sim[i] != EMPTY && !TangoRules.consistentAt(puzzle, sim, i)) {
                    return setOf(i)
                }
            }

            // 2. ¿Alguna línea se quedó con 0 completaciones válidas?
            val simList = sim.toList()
            for (r in 0 until n) {
                val rowCells = (0 until n).map { r * n + it }
                if (rowCells.any { sim[it] == EMPTY }) {
                    val comps = getValidCompletions(puzzle, rowCells, simList)
                    if (comps.isEmpty()) {
                        return setOf(rowCells.first { sim[it] == EMPTY })
                    }
                }
            }
            for (col in 0 until n) {
                val colCells = (0 until n).map { it * n + col }
                if (colCells.any { sim[it] == EMPTY }) {
                    val comps = getValidCompletions(puzzle, colCells, simList)
                    if (comps.isEmpty()) {
                        return setOf(colCells.first { sim[it] == EMPTY })
                    }
                }
            }

            // 3. Propagación simple: T1 (Sign), T2 (Pair), T3 (Sandwich), T4 (Count)
            var progress = false

            // T1
            for (k in puzzle.constraints) {
                if (sim[k.a] != EMPTY && sim[k.b] == EMPTY) {
                    sim[k.b] = if (k.equal) sim[k.a] else opp(sim[k.a])
                    progress = true
                    break
                } else if (sim[k.b] != EMPTY && sim[k.a] == EMPTY) {
                    sim[k.a] = if (k.equal) sim[k.b] else opp(sim[k.b])
                    progress = true
                    break
                }
            }
            if (progress) continue

            // T2
            for (r in 0 until n) {
                for (col in 0 until n - 1) {
                    val c1 = r * n + col; val c2 = r * n + col + 1
                    if (sim[c1] != EMPTY && sim[c1] == sim[c2]) {
                        val o = opp(sim[c1])
                        if (col - 1 >= 0 && sim[r * n + (col - 1)] == EMPTY) {
                            sim[r * n + (col - 1)] = o; progress = true; break
                        }
                        if (col + 2 < n && sim[r * n + (col + 2)] == EMPTY) {
                            sim[r * n + (col + 2)] = o; progress = true; break
                        }
                    }
                }
                if (progress) break
            }
            if (progress) continue

            // T3
            for (r in 0 until n) {
                for (col in 0 until n - 2) {
                    val c1 = r * n + col; val mid = r * n + col + 1; val c2 = r * n + col + 2
                    if (sim[c1] != EMPTY && sim[c1] == sim[c2] && sim[mid] == EMPTY) {
                        sim[mid] = opp(sim[c1]); progress = true; break
                    }
                }
                if (progress) break
            }
            if (progress) continue

            // T4
            for (r in 0 until n) {
                val rowCells = (0 until n).map { r * n + it }
                val empties = rowCells.filter { sim[it] == EMPTY }
                if (empties.isNotEmpty()) {
                    val suns = rowCells.count { sim[it] == SUN }
                    val moons = rowCells.count { sim[it] == MOON }
                    if (suns == n / 2) {
                        sim[empties.first()] = MOON; progress = true; break
                    } else if (moons == n / 2) {
                        sim[empties.first()] = SUN; progress = true; break
                    }
                }
            }
            if (progress) continue

            for (col in 0 until n) {
                val colCells = (0 until n).map { it * n + col }
                val empties = colCells.filter { sim[it] == EMPTY }
                if (empties.isNotEmpty()) {
                    val suns = colCells.count { sim[it] == SUN }
                    val moons = colCells.count { sim[it] == MOON }
                    if (suns == n / 2) {
                        sim[empties.first()] = MOON; progress = true; break
                    } else if (moons == n / 2) {
                        sim[empties.first()] = SUN; progress = true; break
                    }
                }
            }

            if (!progress) {
                // Punto fijo alcanzado sin contradicción
                break
            }
        }
        return null
    }

    private fun <T> Iterable<T>.minWithWithOrNull(comparator: Comparator<in T>): T? {
        val it = iterator()
        if (!it.hasNext()) return null
        var min = it.next()
        while (it.hasNext()) {
            val next = it.next()
            if (comparator.compare(next, min) < 0) min = next
        }
        return min
    }
}
