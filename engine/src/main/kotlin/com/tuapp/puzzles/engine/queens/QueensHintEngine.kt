package com.tuapp.puzzles.engine.queens

import com.tuapp.puzzles.engine.session.QueensSession
import kotlin.math.abs

enum class QueensTechnique {
    WRONG_QUEEN,
    WRONG_MARK,
    GIVEAWAY,
    KNOCKOUT,
    CONFINED,
    N_IN_N,
    HYPOTHETICAL,
    SOLUTION_FALLBACK,
}

enum class UnitKind { ROW, COLUMN, REGION }

data class UnitRef(val kind: UnitKind, val index: Int)

sealed interface QueensHint {
    val technique: QueensTechnique
    /** Celdas a resaltar como "causa" de la deducción (fondo/borde). */
    val cause: Set<Int>
    /** Unidad(es) implicadas, para destacar la región/fila/columna y construir el texto. */
    val units: List<UnitRef>

    /** Colocaciones erróneas del jugador (reinas, o X puestas sobre la celda correcta). */
    data class Wrong(
        override val technique: QueensTechnique,
        val cells: Set<Int>,
        override val cause: Set<Int> = cells,
        override val units: List<UnitRef> = emptyList(),
    ) : QueensHint

    /** Debe haber una corona en [cell]. */
    data class PlaceQueen(
        val cell: Int,
        override val technique: QueensTechnique,
        override val cause: Set<Int>,
        override val units: List<UnitRef>,
    ) : QueensHint

    /** Estas celdas no pueden tener corona: marcar X. Solo incluye celdas aún sin X. */
    data class Eliminate(
        val cells: Set<Int>,
        override val technique: QueensTechnique,
        override val cause: Set<Int>,
        override val units: List<UnitRef>,
    ) : QueensHint
}

object QueensHintEngine {

    /**
     * Devuelve la siguiente pista lógica o error para el tablero de Queens.
     * Retorna null si el tablero ya está resuelto.
     */
    fun nextHint(level: QueensLevel, cells: List<Int>): QueensHint? {
        val n = level.puzzle.size
        val totalCells = n * n
        val regions = level.puzzle.regions
        val solution = level.solution
        val solQueens = (0 until n).map { r -> r * n + solution[r] }.toSet()

        val playerQueens = cells.indices.filter { cells[it] == QueensSession.QUEEN }.toSet()
        val playerMarks = cells.indices.filter { cells[it] == QueensSession.MARK }.toSet()

        // 0. Si ya está resuelto
        if (playerQueens.size == n && playerQueens == solQueens) {
            return null
        }

        // 1. Errores primero (WRONG_QUEEN y WRONG_MARK)
        val badQueens = playerQueens.filter { it !in solQueens }.toSet()
        if (badQueens.isNotEmpty()) {
            return QueensHint.Wrong(
                technique = QueensTechnique.WRONG_QUEEN,
                cells = badQueens,
            )
        }

        val badMarks = playerMarks.filter { it in solQueens }.toSet()
        if (badMarks.isNotEmpty()) {
            return QueensHint.Wrong(
                technique = QueensTechnique.WRONG_MARK,
                cells = badMarks,
            )
        }

        if (playerQueens.size == n) return null

        // 2. Modelo de candidatos a partir de SOLO las reinas del jugador
        val satisfiedRows = BooleanArray(n)
        val satisfiedCols = BooleanArray(n)
        val satisfiedRegs = BooleanArray(n)
        val cand = BooleanArray(totalCells) { true }

        for (q in playerQueens) {
            val rq = q / n
            val cq = q % n
            val regq = regions[q]
            satisfiedRows[rq] = true
            satisfiedCols[cq] = true
            satisfiedRegs[regq] = true
        }

        // Eliminar fila, columna, región y 8 vecinos de cada reina del jugador
        for (q in playerQueens) {
            val rq = q / n
            val cq = q % n
            val regq = regions[q]
            for (c in 0 until n) cand[rq * n + c] = false
            for (r in 0 until n) cand[r * n + cq] = false
            for (i in 0 until totalCells) {
                if (regions[i] == regq) cand[i] = false
            }
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val nr = rq + dr
                    val nc = cq + dc
                    if (nr in 0 until n && nc in 0 until n) {
                        cand[nr * n + nc] = false
                    }
                }
            }
        }
        for (q in playerQueens) {
            cand[q] = false
        }

        val regionCells = Array(n) { reg ->
            (0 until totalCells).filter { regions[it] == reg }
        }

        fun candOfRow(r: Int): List<Int> = (0 until n).map { r * n + it }.filter { cand[it] }
        fun candOfCol(c: Int): List<Int> = (0 until n).map { it * n + c }.filter { cand[it] }
        fun candOfReg(reg: Int): List<Int> = regionCells[reg].filter { cand[it] }

        fun candOfUnit(u: UnitRef): List<Int> = when (u.kind) {
            UnitKind.ROW -> candOfRow(u.index)
            UnitKind.COLUMN -> candOfCol(u.index)
            UnitKind.REGION -> candOfReg(u.index)
        }

        fun isConfinedUnit(u: UnitRef): Boolean {
            val cands = candOfUnit(u)
            if (cands.size <= 1) return false
            return when (u.kind) {
                UnitKind.REGION -> {
                    val rows = cands.map { it / n }.toSet()
                    val cols = cands.map { it % n }.toSet()
                    rows.size == 1 || cols.size == 1
                }
                UnitKind.ROW, UnitKind.COLUMN -> {
                    val regs = cands.map { regions[it] }.toSet()
                    regs.size == 1
                }
            }
        }

        fun directlyEliminates(queenCell: Int, otherCell: Int): Boolean {
            if (queenCell == otherCell) return false
            val qr = queenCell / n
            val qc = queenCell % n
            val or = otherCell / n
            val oc = otherCell % n
            return qr == or || qc == oc || regions[queenCell] == regions[otherCell] ||
                (abs(qr - or) <= 1 && abs(qc - oc) <= 1)
        }

        // --- T1 · GIVEAWAY ---
        for (reg in 0 until n) {
            if (!satisfiedRegs[reg]) {
                val cands = candOfReg(reg)
                if (cands.size == 1) {
                    val cell = cands.first()
                    return QueensHint.PlaceQueen(
                        cell = cell,
                        technique = QueensTechnique.GIVEAWAY,
                        cause = setOf(cell),
                        units = listOf(UnitRef(UnitKind.REGION, reg)),
                    )
                }
            }
        }

        for (r in 0 until n) {
            if (!satisfiedRows[r]) {
                val cands = candOfRow(r)
                if (cands.size == 1) {
                    val cell = cands.first()
                    return QueensHint.PlaceQueen(
                        cell = cell,
                        technique = QueensTechnique.GIVEAWAY,
                        cause = setOf(cell),
                        units = listOf(UnitRef(UnitKind.ROW, r)),
                    )
                }
            }
        }

        for (c in 0 until n) {
            if (!satisfiedCols[c]) {
                val cands = candOfCol(c)
                if (cands.size == 1) {
                    val cell = cands.first()
                    return QueensHint.PlaceQueen(
                        cell = cell,
                        technique = QueensTechnique.GIVEAWAY,
                        cause = setOf(cell),
                        units = listOf(UnitRef(UnitKind.COLUMN, c)),
                    )
                }
            }
        }

        // --- T2 · KNOCKOUT ---
        // Se buscan celdas cuya colocación dejaría una unidad sin sitio.
        // Si la unidad está confinada en una sola línea o región, esa deducción pertenece a CONFINED (T3).
        for (x in 0 until totalCells) {
            if (!cand[x]) continue
            val xr = x / n
            val xc = x % n
            val xreg = regions[x]

            fun starvesUnit(u: UnitRef): Boolean {
                if (isConfinedUnit(u)) return false
                val cands = candOfUnit(u)
                return cands.isNotEmpty() && cands.all { directlyEliminates(x, it) }
            }

            var starvedUnit: UnitRef? = null
            for (reg in 0 until n) {
                if (!satisfiedRegs[reg] && reg != xreg && starvesUnit(UnitRef(UnitKind.REGION, reg))) {
                    starvedUnit = UnitRef(UnitKind.REGION, reg)
                    break
                }
            }
            if (starvedUnit == null) {
                for (r in 0 until n) {
                    if (!satisfiedRows[r] && r != xr && starvesUnit(UnitRef(UnitKind.ROW, r))) {
                        starvedUnit = UnitRef(UnitKind.ROW, r)
                        break
                    }
                }
            }
            if (starvedUnit == null) {
                for (c in 0 until n) {
                    if (!satisfiedCols[c] && c != xc && starvesUnit(UnitRef(UnitKind.COLUMN, c))) {
                        starvedUnit = UnitRef(UnitKind.COLUMN, c)
                        break
                    }
                }
            }

            if (starvedUnit != null) {
                val targetUnit = starvedUnit
                val targetCands = candOfUnit(targetUnit)
                val allStarving = (0 until totalCells).filter { cell ->
                    cand[cell] && targetCands.all { directlyEliminates(cell, it) }
                }.toSet()

                val newElims = allStarving.filter { cells[it] != QueensSession.MARK }.toSet()
                if (newElims.isNotEmpty()) {
                    return QueensHint.Eliminate(
                        cells = newElims,
                        technique = QueensTechnique.KNOCKOUT,
                        cause = targetCands.toSet(),
                        units = listOf(targetUnit),
                    )
                }
            }
        }

        // --- T3 · CONFINED ---
        // (a1) Región con candidatos en una sola fila
        for (reg in 0 until n) {
            if (!satisfiedRegs[reg]) {
                val cands = candOfReg(reg)
                if (cands.isNotEmpty()) {
                    val rows = cands.map { it / n }.toSet()
                    if (rows.size == 1) {
                        val r = rows.first()
                        val elim = candOfRow(r).filter { regions[it] != reg }
                        val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                        if (newElims.isNotEmpty()) {
                            return QueensHint.Eliminate(
                                cells = newElims,
                                technique = QueensTechnique.CONFINED,
                                cause = cands.toSet(),
                                units = listOf(UnitRef(UnitKind.REGION, reg), UnitRef(UnitKind.ROW, r)),
                            )
                        }
                    }
                }
            }
        }

        // (a2) Región con candidatos en una sola columna
        for (reg in 0 until n) {
            if (!satisfiedRegs[reg]) {
                val cands = candOfReg(reg)
                if (cands.isNotEmpty()) {
                    val cols = cands.map { it % n }.toSet()
                    if (cols.size == 1) {
                        val c = cols.first()
                        val elim = candOfCol(c).filter { regions[it] != reg }
                        val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                        if (newElims.isNotEmpty()) {
                            return QueensHint.Eliminate(
                                cells = newElims,
                                technique = QueensTechnique.CONFINED,
                                cause = cands.toSet(),
                                units = listOf(UnitRef(UnitKind.REGION, reg), UnitRef(UnitKind.COLUMN, c)),
                            )
                        }
                    }
                }
            }
        }

        // (b1) Fila con candidatos en una sola región
        for (r in 0 until n) {
            if (!satisfiedRows[r]) {
                val cands = candOfRow(r)
                if (cands.isNotEmpty()) {
                    val regs = cands.map { regions[it] }.toSet()
                    if (regs.size == 1) {
                        val reg = regs.first()
                        val elim = candOfReg(reg).filter { it / n != r }
                        val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                        if (newElims.isNotEmpty()) {
                            return QueensHint.Eliminate(
                                cells = newElims,
                                technique = QueensTechnique.CONFINED,
                                cause = cands.toSet(),
                                units = listOf(UnitRef(UnitKind.ROW, r), UnitRef(UnitKind.REGION, reg)),
                            )
                        }
                    }
                }
            }
        }

        // (b2) Columna con candidatos en una sola región
        for (c in 0 until n) {
            if (!satisfiedCols[c]) {
                val cands = candOfCol(c)
                if (cands.isNotEmpty()) {
                    val regs = cands.map { regions[it] }.toSet()
                    if (regs.size == 1) {
                        val reg = regs.first()
                        val elim = candOfReg(reg).filter { it % n != c }
                        val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                        if (newElims.isNotEmpty()) {
                            return QueensHint.Eliminate(
                                cells = newElims,
                                technique = QueensTechnique.CONFINED,
                                cause = cands.toSet(),
                                units = listOf(UnitRef(UnitKind.COLUMN, c), UnitRef(UnitKind.REGION, reg)),
                            )
                        }
                    }
                }
            }
        }

        // --- T4 · N_IN_N (k = 2..min(4, n-1)) ---
        fun combinations(items: List<Int>, k: Int): List<List<Int>> {
            val result = mutableListOf<List<Int>>()
            fun recurse(start: Int, chosen: MutableList<Int>) {
                if (chosen.size == k) {
                    result.add(chosen.toList())
                    return
                }
                for (i in start until items.size) {
                    chosen.add(items[i])
                    recurse(i + 1, chosen)
                    chosen.removeAt(chosen.size - 1)
                }
            }
            recurse(0, mutableListOf())
            return result
        }

        val pendingRegs = (0 until n).filter { !satisfiedRegs[it] }
        val pendingRows = (0 until n).filter { !satisfiedRows[it] }
        val pendingCols = (0 until n).filter { !satisfiedCols[it] }

        for (k in 2..minOf(4, n - 1)) {
            // (1) k regiones en k filas
            if (pendingRegs.size >= k) {
                for (sub in combinations(pendingRegs, k)) {
                    val allCands = sub.flatMap { candOfReg(it) }.distinct()
                    if (allCands.isNotEmpty()) {
                        val rows = allCands.map { it / n }.toSet()
                        if (rows.size == k) {
                            val elim = rows.flatMap { candOfRow(it) }.filter { regions[it] !in sub }.toSet()
                            val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                            if (newElims.isNotEmpty()) {
                                return QueensHint.Eliminate(
                                    cells = newElims,
                                    technique = QueensTechnique.N_IN_N,
                                    cause = allCands.toSet(),
                                    units = sub.map { UnitRef(UnitKind.REGION, it) } + rows.sorted().map { UnitRef(UnitKind.ROW, it) },
                                )
                            }
                        }
                    }
                }
            }

            // (2) k regiones en k columnas
            if (pendingRegs.size >= k) {
                for (sub in combinations(pendingRegs, k)) {
                    val allCands = sub.flatMap { candOfReg(it) }.distinct()
                    if (allCands.isNotEmpty()) {
                        val cols = allCands.map { it % n }.toSet()
                        if (cols.size == k) {
                            val elim = cols.flatMap { candOfCol(it) }.filter { regions[it] !in sub }.toSet()
                            val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                            if (newElims.isNotEmpty()) {
                                return QueensHint.Eliminate(
                                    cells = newElims,
                                    technique = QueensTechnique.N_IN_N,
                                    cause = allCands.toSet(),
                                    units = sub.map { UnitRef(UnitKind.REGION, it) } + cols.sorted().map { UnitRef(UnitKind.COLUMN, it) },
                                )
                            }
                        }
                    }
                }
            }

            // (3) k filas en k regiones
            if (pendingRows.size >= k) {
                for (sub in combinations(pendingRows, k)) {
                    val allCands = sub.flatMap { candOfRow(it) }.distinct()
                    if (allCands.isNotEmpty()) {
                        val regs = allCands.map { regions[it] }.toSet()
                        if (regs.size == k) {
                            val elim = regs.flatMap { candOfReg(it) }.filter { it / n !in sub }.toSet()
                            val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                            if (newElims.isNotEmpty()) {
                                return QueensHint.Eliminate(
                                    cells = newElims,
                                    technique = QueensTechnique.N_IN_N,
                                    cause = allCands.toSet(),
                                    units = sub.map { UnitRef(UnitKind.ROW, it) } + regs.sorted().map { UnitRef(UnitKind.REGION, it) },
                                )
                            }
                        }
                    }
                }
            }

            // (4) k columnas en k regiones
            if (pendingCols.size >= k) {
                for (sub in combinations(pendingCols, k)) {
                    val allCands = sub.flatMap { candOfCol(it) }.distinct()
                    if (allCands.isNotEmpty()) {
                        val regs = allCands.map { regions[it] }.toSet()
                        if (regs.size == k) {
                            val elim = regs.flatMap { candOfReg(it) }.filter { it % n !in sub }.toSet()
                            val newElims = elim.filter { cells[it] != QueensSession.MARK }.toSet()
                            if (newElims.isNotEmpty()) {
                                return QueensHint.Eliminate(
                                    cells = newElims,
                                    technique = QueensTechnique.N_IN_N,
                                    cause = allCands.toSet(),
                                    units = sub.map { UnitRef(UnitKind.COLUMN, it) } + regs.sorted().map { UnitRef(UnitKind.REGION, it) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- T5 · HYPOTHETICAL (profundidad 1 + T1) ---
        for (x in 0 until totalCells) {
            if (!cand[x]) continue
            if (cells[x] == QueensSession.MARK) continue

            val simCand = cand.clone()
            val simSatRows = satisfiedRows.clone()
            val simSatCols = satisfiedCols.clone()
            val simSatRegs = satisfiedRegs.clone()
            val chain = linkedSetOf(x)

            fun simPlace(cell: Int) {
                val r = cell / n
                val c = cell % n
                val reg = regions[cell]
                simSatRows[r] = true
                simSatCols[c] = true
                simSatRegs[reg] = true
                for (i in 0 until totalCells) {
                    if (!simCand[i]) continue
                    val ir = i / n
                    val ic = i % n
                    if (ir == r || ic == c || regions[i] == reg || (abs(ir - r) <= 1 && abs(ic - c) <= 1)) {
                        simCand[i] = false
                    }
                }
            }

            simPlace(x)

            var contradictionUnit: UnitRef? = null
            while (true) {
                for (reg in 0 until n) {
                    if (!simSatRegs[reg]) {
                        val count = regionCells[reg].count { simCand[it] }
                        if (count == 0) {
                            contradictionUnit = UnitRef(UnitKind.REGION, reg)
                            break
                        }
                    }
                }
                if (contradictionUnit == null) {
                    for (r in 0 until n) {
                        if (!simSatRows[r]) {
                            val count = (0 until n).count { simCand[r * n + it] }
                            if (count == 0) {
                                contradictionUnit = UnitRef(UnitKind.ROW, r)
                                break
                            }
                        }
                    }
                }
                if (contradictionUnit == null) {
                    for (c in 0 until n) {
                        if (!simSatCols[c]) {
                            val count = (0 until n).count { simCand[it * n + c] }
                            if (count == 0) {
                                contradictionUnit = UnitRef(UnitKind.COLUMN, c)
                                break
                            }
                        }
                    }
                }

                if (contradictionUnit != null) {
                    break
                }

                var forced: Int? = null
                for (reg in 0 until n) {
                    if (!simSatRegs[reg]) {
                        val cands = regionCells[reg].filter { simCand[it] }
                        if (cands.size == 1) {
                            forced = cands.first()
                            break
                        }
                    }
                }
                if (forced == null) {
                    for (r in 0 until n) {
                        if (!simSatRows[r]) {
                            val cands = (0 until n).map { r * n + it }.filter { simCand[it] }
                            if (cands.size == 1) {
                                forced = cands.first()
                                break
                            }
                        }
                    }
                }
                if (forced == null) {
                    for (c in 0 until n) {
                        if (!simSatCols[c]) {
                            val cands = (0 until n).map { it * n + c }.filter { simCand[it] }
                            if (cands.size == 1) {
                                forced = cands.first()
                                break
                            }
                        }
                    }
                }

                if (forced != null) {
                    chain.add(forced)
                    simPlace(forced)
                } else {
                    break
                }
            }

            if (contradictionUnit != null) {
                return QueensHint.Eliminate(
                    cells = setOf(x),
                    technique = QueensTechnique.HYPOTHETICAL,
                    cause = chain.toSet(),
                    units = listOf(contradictionUnit),
                )
            }
        }

        // --- RESPALDO · SOLUTION_FALLBACK ---
        if (pendingRegs.isNotEmpty()) {
            val bestReg = pendingRegs.minByOrNull { candOfReg(it).size } ?: pendingRegs.first()
            val bestCands = candOfReg(bestReg)
            val solCell = (0 until n).map { r -> r * n + solution[r] }.first { regions[it] == bestReg }
            return QueensHint.PlaceQueen(
                cell = solCell,
                technique = QueensTechnique.SOLUTION_FALLBACK,
                cause = bestCands.toSet(),
                units = listOf(UnitRef(UnitKind.REGION, bestReg)),
            )
        }

        return null
    }
}
