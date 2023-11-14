package com.tangem.core.ui.event

import androidx.compose.runtime.Immutable

/**
 * Represents compose state event, which can be consumed or triggered.
 *
 * This is especially useful for handling one-off UI events like showing snack bars or navigation which should not be
 * re-triggered on recompositions or state changes.
 */
@Immutable
sealed class StateEvent<in A> {

    /**
     * Represents an already consumed state event.
     * Events of this type will not trigger any further actions.
     */
    class Consumed<A> : StateEvent<A>() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Consumed<*>
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    /**
     * Represents a state event that has been triggered but not yet consumed.
     *
     * @property data The data provided by the event.
     * @property onConsume The action to be executed when the event is consumed.
     */
    data class Triggered<A>(
        val data: A,
        val onConsume: () -> Unit,
    ) : StateEvent<A>()
}

/**
 * Creates a [StateEvent.Triggered] instance.
 *
 * @param onConsume The action to be executed when the event is consumed.
 * @return A triggered state event.
 */
fun <A> triggeredEvent(data: A, onConsume: () -> Unit): StateEvent<A> = StateEvent.Triggered(data, onConsume)

/**
 * Creates a [StateEvent.Consumed] instance.
 *
 * @return A consumed state event.
 */
fun <A> consumedEvent(): StateEvent<A> = StateEvent.Consumed()