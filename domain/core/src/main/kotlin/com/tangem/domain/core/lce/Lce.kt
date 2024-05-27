package com.tangem.domain.core.lce

import arrow.core.identity
import com.tangem.domain.core.utils.flatMap
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading

/**
 * A sealed class representing the three states of a data load operation: Loading, Content, and Error.
 *
 * @param E The type of the error object.
 * @param C The type of the content object.
 */
sealed class Lce<out E : Any, out C : Any> {

    /**
     * Represents the loading state, which may contain partial content.
     *
     * @param partialContent The partial content that has been loaded so far, if any.
     */
    data class Loading<C : Any>(val partialContent: C?) : Lce<Nothing, C>()

    /**
     * Represents the content state, which contains the loaded content.
     *
     * @param content The loaded content.
     */
    data class Content<C : Any>(val content: C) : Lce<Nothing, C>()

    /**
     * Represents the error state, which contains an error object.
     *
     * @param error The error that occurred during loading.
     */
    data class Error<E : Any>(val error: E) : Lce<E, Nothing>()

    /**
     * Applies the given functions to the content, error, or partial content of this Lce, depending on its state.
     *
     * @param ifLoading The function to apply if this is a [Loading] state.
     * @param ifContent The function to apply if this is a [Content] state.
     * @param ifError The function to apply if this is an [Error] state.
     * @return The result of applying the corresponding function.
     */
    inline fun <T> fold(
        ifLoading: (partialContent: C?) -> T,
        ifContent: (content: C) -> T,
        ifError: (error: E) -> T,
    ): T = when (this) {
        is Loading -> ifLoading(partialContent)
        is Error -> ifError(error)
        is Content -> ifContent(content)
    }

    /**
     * Transforms the content of this [Lce] by applying the given function.
     * If this is a [Content] state, the function is applied to the [Content.content].
     * If this is a [Loading] state and partialContent is present,
     * the function is applied to the [Loading.partialContent].
     *
     * @param ifContent The function to apply to the content or partial content.
     * @return A new [Lce] instance containing the result of applying the function.
     */
    inline fun <T : Any> map(ifContent: (C) -> T): Lce<E, T> = flatMap { content, isLoading ->
        if (isLoading) {
            lceLoading(ifContent(content))
        } else {
            ifContent(content).lceContent()
        }
    }

    /**
     * Transforms the error of this [Lce] by applying the given function, if this is an [Error] state.
     *
     * @param ifError The function to apply to the error.
     * @return A new [Lce] with the transformed error, or this [Lce] unchanged if it is not an [Error] state.
     */
    inline fun <T : Any> mapError(ifError: (E) -> T): Lce<T, C> = when (this) {
        is Loading -> this
        is Content -> this
        is Error -> ifError(error).lceError()
    }

    /**
     * Returns the content of this [Lce] if it's a [Lce.Content] or partial content if it's a [Lce.Loading],
     * `null` if it's a [Lce.Error].
     *
     * @return The content of this [Lce] or `null`.
     */
    fun getOrNull(): C? = fold(
        ifLoading = ::identity,
        ifContent = ::identity,
        ifError = { null },
    )
}
