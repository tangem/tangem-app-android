package com.tangem.domain.core.lce

sealed class Lce<out E : Any, out T> {

    data class Loading<T>(val partialContent: T?) : Lce<Nothing, T>()

    data class Content<T>(val content: T) : Lce<Nothing, T>()

    data class Error<E : Any>(val error: E) : Lce<E, Nothing>()

    inline fun <C> fold(
        ifLoading: (partialContent: T?) -> C,
        ifContent: (content: T) -> C,
        ifError: (error: E) -> C,
    ): C = when (this) {
        is Loading -> ifLoading(partialContent)
        is Error -> ifError(error)
        is Content -> ifContent(content)
    }

    inline fun <C : Any> map(ifContent: (T) -> C): Lce<E, C> = fold(
        ifLoading = { Loading(it?.let(ifContent)) },
        ifContent = { Content(ifContent(it)) },
        ifError = { Error(it) },
    )

    inline fun <C : Any> mapError(ifError: (E) -> C): Lce<C, T> = fold(
        ifLoading = { Loading(it) },
        ifContent = { Content(it) },
        ifError = { Error(ifError(it)) },
    )
}
