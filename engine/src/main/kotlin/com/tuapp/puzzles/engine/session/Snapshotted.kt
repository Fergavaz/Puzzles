package com.tuapp.puzzles.engine.session

/**
 * Base de las sesiones de juego: estado inmutable + historial de instantáneas para Deshacer.
 * Sin dependencias de Android: se prueba con JUnit y el ViewModel solo reenvía las acciones.
 */
abstract class Snapshotted<S>(initial: S) {
    var state: S = initial
        protected set
    private val history = ArrayDeque<S>()

    val canUndo: Boolean get() = history.isNotEmpty()

    /** Cambia el estado guardando el anterior para poder deshacer (ignora cambios nulos). */
    protected fun change(new: S) {
        if (new != state) { history.addLast(state); state = new }
    }

    /** Registra [before] como punto de deshacer (para gestos que mutan el estado en varios pasos). */
    protected fun recordHistory(before: S) {
        if (before != state) history.addLast(before)
    }

    fun undo(): Boolean {
        val prev = history.removeLastOrNull() ?: return false
        state = prev
        return true
    }
}
