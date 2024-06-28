package com.tangem.domain.core.utils

import arrow.core.Either
import arrow.core.identity
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.lce.Lce

/**
 * Creates a [Lce.Loading] instance with optional partial content.
 *
 * @param partialContent The partial content that has been loaded so far, if any.
 * @return A [Lce.Loading] instance.
 */
fun <C : Any> lceLoading(partialContent: C? = null): Lce<Nothing, C> = Lce.Loading(partialContent)

/**
 * Wraps the receiver object in a [Lce.Content] instance.
 *
 * @return A [Lce.Content] instance containing the receiver object.
 */
fun <C : Any> C.lceContent(): Lce<Nothing, C> = Lce.Content(content = this)

/**
 * Wraps the receiver object in a [Lce.Error] instance.
 *
 * @return A [Lce.Error] instance containing the receiver object.
 */
fun <E : Any> E.lceError(): Lce<E, Nothing> = Lce.Error(error = this)

/**
 * Transforms this [Lce] instance by applying the given function into a new [Lce] instance.
 *
 * @param block The function to apply to the content of this [Lce].
 * @return A new [Lce] instance containing the result of applying the function.
 * */
inline fun <E : Any, C : Any, T : Any> Lce<E, C>.flatMap(
    block: (content: C, isLoading: Boolean) -> Lce<E, T>,
): Lce<E, T> = when (this) {
    is Lce.Loading -> partialContent?.let { block(it, true) } ?: lceLoading()
    is Lce.Content -> block(content, false)
    is Lce.Error -> this
}

/**
 * Returns the content of this [Lce] if it's a [Lce.Content] or applies the given functions if it's a [Lce.Loading] or [Lce.Error].
 *
 * @param ifLoading The function to apply if this is a [Lce.Loading] state.
 * @param ifError The function to apply if this is a [Lce.Error] state.
 * @return The content of this [Lce] or the result of applying the corresponding function.
 */
inline fun <E : Any, C : Any> Lce<E, C>.getOrElse(ifLoading: (maybeContent: C?) -> C, ifError: (error: E) -> C): C {
    return fold(
        ifLoading = ifLoading,
        ifContent = ::identity,
        ifError = ifError,
    )
}

/**
 * Transforms this [Lce] into an [Either] instance.
 * If this is a [Lce.Content], the content is wrapped in a [Either.Right].
 * If this is a [Lce.Error], the error is wrapped in a [Either.Left].
 * If this is a [Lce.Loading], the [ifLoading] function is applied to the partial content and the result is wrapped in a
 * [Either.Right].
 *
 * @param ifLoading The function to apply if this is a [Lce.Loading] state.
 * @return An [Either] instance containing the content or error of this [Lce],
 * or the result of applying the [ifLoading] function to the partial content.
 */
inline fun <E : Any, C : Any> Lce<E, C>.toEither(ifLoading: (maybeContent: C?) -> C): Either<E, C> = fold(
    ifLoading = { ifLoading(it).right() },
    ifContent = { it.right() },
    ifError = { it.left() },
)