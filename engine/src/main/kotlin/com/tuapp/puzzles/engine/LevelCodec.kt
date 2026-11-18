package com.tuapp.puzzles.engine

import com.tuapp.puzzles.core.CampaignLevel
import com.tuapp.puzzles.core.GameType
import com.tuapp.puzzles.core.Json
import com.tuapp.puzzles.core.PuzzleLevel
import com.tuapp.puzzles.engine.patches.*
import com.tuapp.puzzles.engine.queens.*
import com.tuapp.puzzles.engine.sudoku.*
import com.tuapp.puzzles.engine.sumbreak.*
import com.tuapp.puzzles.engine.tango.*
import com.tuapp.puzzles.engine.zip.*

/** Conversión nivel <-> estructura JSON (mapas y listas). */
object LevelCodec {

    fun encode(level: PuzzleLevel): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        m["seed"] = level.seed
        m["difficulty"] = level.difficulty
        when (level) {
            is ZipLevel -> {
                m["width"] = level.puzzle.width; m["height"] = level.puzzle.height
                m["numbers"] = level.puzzle.numbers
                m["walls"] = level.puzzle.walls.map { listOf(it.a, it.b) }
                m["solution"] = level.solution
            }
            is PatchesLevel -> {
                m["size"] = level.puzzle.size
                m["clues"] = level.puzzle.clues.map { listOf(it.cell, it.type.name, it.area) }
                m["solution"] = level.solution.map { listOf(it.row, it.col, it.height, it.width) }
            }
            is SudokuLevel -> {
                m["givens"] = level.puzzle.givens
                m["solution"] = level.solution
            }
            is TangoLevel -> {
                m["size"] = level.puzzle.size
                m["givens"] = level.puzzle.givens
                m["constraints"] = level.puzzle.constraints.map { listOf(it.a, it.b, if (it.equal) 1 else 0) }
                m["solution"] = level.solution
            }
            is QueensLevel -> {
                m["size"] = level.puzzle.size
                m["regions"] = level.puzzle.regions
                m["solution"] = level.solution
            }
            is SumbreakLevel -> {
                m["size"] = level.puzzle.size
                m["targetNumber"] = level.targetNumber
                m["tiles"] = level.puzzle.initialTiles.map { tile ->
                    when (tile) {
                        is SumbreakTile.Empty -> listOf(0, 0, 0)
                        is SumbreakTile.Number -> listOf(1, tile.value, 0)
                        is SumbreakTile.Modifier -> listOf(2, tile.op.ordinal, tile.value)
                    }
                }
                m["solution"] = level.solutionMoves.map { listOf(it.from, it.to) }
            }
            else -> throw IllegalArgumentException("Nivel desconocido: ${level::class}")
        }
        return m
    }

    fun decode(game: GameType, m: Map<String, Any?>): PuzzleLevel {
        val seed = m["seed"].long()
        val d = m["difficulty"].int()
        return when (game) {
            GameType.ZIP -> ZipLevel(
                seed, d,
                ZipPuzzle(
                    m["width"].int(), m["height"].int(), m["numbers"].ints(),
                    m["walls"].lists().map { Edge(it[0].int(), it[1].int()) },
                ),
                m["solution"].ints(),
            )
            GameType.PATCHES -> PatchesLevel(
                seed, d,
                PatchesPuzzle(
                    m["size"].int(),
                    m["clues"].lists().map { PatchClue(it[0].int(), PatchType.valueOf(it[1] as String), it[2]?.int()) },
                ),
                m["solution"].lists().map { Rect(it[0].int(), it[1].int(), it[2].int(), it[3].int()) },
            )
            GameType.SUDOKU -> SudokuLevel(seed, d, SudokuPuzzle(m["givens"].ints()), m["solution"].ints())
            GameType.TANGO -> TangoLevel(
                seed, d,
                TangoPuzzle(
                    m["size"].int(), m["givens"].ints(),
                    m["constraints"].lists().map { TangoConstraint(it[0].int(), it[1].int(), it[2].int() == 1) },
                ),
                m["solution"].ints(),
            )
            GameType.QUEENS -> QueensLevel(
                seed, d, QueensPuzzle(m["size"].int(), m["regions"].ints()), m["solution"].ints(),
            )
            GameType.SUMBREAK -> {
                val size = m["size"].int()
                val targetNumber = m["targetNumber"].int()
                val rawTiles = m["tiles"].lists()
                val tiles = rawTiles.map { t ->
                    when (t[0].int()) {
                        1 -> SumbreakTile.Number(t[1].int())
                        2 -> SumbreakTile.Modifier(ModifierOp.entries[t[1].int()], t[2].int())
                        else -> SumbreakTile.Empty
                    }
                }
                val solMoves = m["solution"]?.let { sol ->
                    (sol as List<*>).map { (it as List<*>).let { move -> SumbreakMove((move[0] as Number).toInt(), (move[1] as Number).toInt()) } }
                } ?: emptyList()
                SumbreakLevel(seed, d, SumbreakPuzzle(size, tiles), targetNumber, solMoves)
            }
        }
    }

    private fun Any?.int(): Int = (this as Number).toInt()
    private fun Any?.long(): Long = (this as Number).toLong()
    private fun Any?.ints(): List<Int> = (this as List<*>).map { it.int() }
    private fun Any?.lists(): List<List<Any?>> = (this as List<*>).map { it as List<Any?> }
}

/** Fichero de campaña de un juego: {"game":..,"version":1,"levels":[{id,index,...}]}. */
object CampaignCodec {

    fun encode(game: GameType, levels: List<CampaignLevel>): String {
        val sb = StringBuilder()
        sb.append("{\"game\":\"").append(game.id).append("\",\"version\":1,\"levels\":[\n")
        levels.forEachIndexed { i, cl ->
            val m = LinkedHashMap<String, Any?>()
            m["id"] = cl.id
            m["index"] = cl.index
            m.putAll(LevelCodec.encode(cl.level))
            Json.writeTo(sb, m)
            if (i < levels.size - 1) sb.append(',')
            sb.append('\n')
        }
        sb.append("]}\n")
        return sb.toString()
    }

    @Suppress("UNCHECKED_CAST")
    fun decode(game: GameType, text: String): List<CampaignLevel> {
        val root = Json.parse(text) as Map<String, Any?>
        require(root["game"] == game.id) { "El fichero no es de ${game.id}" }
        return (root["levels"] as List<Map<String, Any?>>).map {
            CampaignLevel(it["id"] as String, (it["index"] as Number).toInt(), LevelCodec.decode(game, it))
        }
    }
}
