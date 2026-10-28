package com.tuapp.puzzles.core

/**
 * JSON mínimo (sin dependencias, usable en el módulo JVM puro y en Android).
 * Tipos: Map<String, Any?>, List<Any?>, String, Long, Double, Boolean, null.
 */
object Json {

    fun parse(text: String): Any? {
        val p = Parser(text)
        val v = p.value()
        p.skipWs()
        require(p.i == text.length) { "Texto sobrante en posición ${p.i}" }
        return v
    }

    fun write(value: Any?): String = StringBuilder().also { writeTo(it, value) }.toString()

    fun writeTo(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is Boolean -> sb.append(v)
            is Int, is Long, is Short, is Byte -> sb.append(v.toString())
            is Number -> sb.append(v.toString())
            is String -> quote(sb, v)
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, x) in v) {
                    if (!first) sb.append(',')
                    first = false
                    quote(sb, k.toString()); sb.append(':'); writeTo(sb, x)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                for (x in v) { if (!first) sb.append(','); first = false; writeTo(sb, x) }
                sb.append(']')
            }
            else -> throw IllegalArgumentException("Tipo no soportado: ${v::class}")
        }
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) when (ch) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
        }
        sb.append('"')
    }

    private class Parser(val s: String) {
        var i = 0

        fun skipWs() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun value(): Any? {
            skipWs()
            require(i < s.length) { "JSON incompleto" }
            return when (val c = s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c.isDigit()) num() else error("Carácter inesperado '$c' en $i")
            }
        }

        fun lit(word: String, v: Any?): Any? {
            require(s.startsWith(word, i)) { "Literal inválido en $i" }
            i += word.length
            return v
        }

        fun num(): Any {
            val st = i
            while (i < s.length && s[i] in "+-0123456789.eE") i++
            val t = s.substring(st, i)
            return if (t.any { it == '.' || it == 'e' || it == 'E' }) t.toDouble() else t.toLong()
        }

        fun str(): String {
            i++ // comilla inicial
            val sb = StringBuilder()
            while (true) {
                require(i < s.length) { "Cadena sin cerrar" }
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                            'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> error("Escape inválido \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        fun arr(): List<Any?> {
            i++
            val out = ArrayList<Any?>()
            skipWs()
            if (s[i] == ']') { i++; return out }
            while (true) {
                out.add(value())
                skipWs()
                when (s[i++]) { ',' -> continue; ']' -> return out; else -> error("Se esperaba , o ] en ${i - 1}") }
            }
        }

        fun obj(): Map<String, Any?> {
            i++
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (s[i] == '}') { i++; return out }
            while (true) {
                skipWs()
                require(s[i] == '"') { "Se esperaba clave en $i" }
                val k = str()
                skipWs()
                require(s[i++] == ':') { "Se esperaba : en ${i - 1}" }
                out[k] = value()
                skipWs()
                when (s[i++]) { ',' -> continue; '}' -> return out; else -> error("Se esperaba , o } en ${i - 1}") }
            }
        }
    }
}
