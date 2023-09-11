package com.tangem.core.ui.event

import androidx.compose.runtime.Immutable

/**
 * Represents compose state event, which can be consumed or triggered.
 *
 * This is especially useful for handling one-off UI events like showing snack bars or navigation which should not be
 * re-triggered on recompositions or state changes.
 */
@Immutable
sealed class StateEvent {

    /** Defines the action to be executed when the event is consumed. */
    protected abstract val onConsume: () -> Unit

    /**
     * Represents an already consumed state event.
     * Events of this type will not trigger any further actions.
     */
    object Consumed : StateEvent() {
        override val onConsume: () -> Unit = {}
    }

    /**
     * Represents a state event that has been triggered but not yet consumed.
     *
     * @property onConsume The action to be executed when the event is consumed.
     */
    data class Triggered(override val onConsume: () -> Unit) : StateEvent()

    /**
     * Consumes the event, triggering any associated action.
     */
    fun consume() {
        onConsume()
    }
}

/**
 * Creates a [StateEvent.Triggered] instance.
 *
 * @param onConsume The action to be executed when the event is consumed.
 * @return A triggered state event.
 */
fun triggered(onConsume: () -> Unit): StateEvent.Triggered = StateEvent.Triggered(onConsume)

/**
 * Represents a statically defined [StateEvent.Consumed] event.
 */
val consumed: StateEvent.Consumed = StateEvent.Consumed