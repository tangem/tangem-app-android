package com.tangem.domain.core.lce

import arrow.atomic.Atomic
import arrow.core.raise.Raise
import arrow.core.raise.recover
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import kotlin.experimental.ExperimentalTypeInference

/**
 * A class that wraps a [Raise] instance for [Lce] type.
 * It provides methods to handle [Lce] instances and raise errors.
 *
 * @property raise The [Raise] instance that this class wraps.
 * @property isLoading An [Atomic] boolean flag indicating whether a loading operation is in progress.
 */
class LceRaise<E : Any> @PublishedApi internal constructor(
    private val raise: Raise<Lce<E, Nothing>>,
) : Raise<Lce<E, Nothing>> by raise {

    val isLoading: Atomic<Boolean> = Atomic(false)

    /**
     * Binds the content of this [Lce] instance and handles its state.
     * If this is a [Lce.Loading] state, sets the [isLoading] flag to true and calls the [ifLoading] function.
     * If this is a [Lce.Content] state, returns the content.
     * If this is a [Lce.Error] state, raises the error.
     *
     * @param ifLoading The function to call if this is a [Lce.Loading] state.
     * By default, it raises a new [Lce.Loading] state.
     * @return The content of this [Lce] instance.
     */
    fun <C : Any> Lce<E, C>.bind(ifLoading: (partialContent: C?) -> C = { raise(lceLoading()) }): C = when (this) {
        is Lce.Loading -> {
            isLoading.set(true)

            ifLoading(partialContent)
        }
        is Lce.Content -> content
        is Lce.Error -> raise(r = this)
    }

    /**
     * Binds the content of this [Lce] instance and handles its state.
     * If this is a [Lce.Loading] state, sets the [isLoading] flag to true and returns the partial content.
     * If this is a [Lce.Content] state, returns the content.
     * If this is a [Lce.Error] state, raises the error.
     *
     * @return The content of this [Lce] instance.
     */
    fun <C : Any> Lce<E, C>.bindOrNull(): C? = when (this) {
        is Lce.Loading -> {
            isLoading.set(true)

            partialContent
        }
        is Lce.Content -> content
        is Lce.Error -> raise(r = this)
    }
}

/**
 * Creates a [Lce] instance by executing the given [block] within a [LceRaise] context.
 *
 * @param ifLoading The function to call if the [block] raises a [Lce.Loading] state.
 * By default, it creates a new [Lce.Loading] state with the value returned by the [block].
 * @param block The block to execute within a [LceRaise] context.
 * @return A [Lce] instance representing the result of the [block].
 */
@OptIn(ExperimentalTypeInference::class)
inline fun <E : Any, C : Any> lce(
    ifLoading: LceRaise<E>.(C) -> Lce<E, C> = { lceLoading(partialContent = it) },
    @BuilderInference block: LceRaise<E>.() -> C,
): Lce<E, C> = recover(
    block = {
        val raise = LceRaise(raise = this)
        val value = block(raise)

        if (raise.isLoading.get()) {
            ifLoading(raise, value)
        } else {
            value.lceContent()
        }
    },
    recover = { e: Lce<E, Nothing> -> e },
)
