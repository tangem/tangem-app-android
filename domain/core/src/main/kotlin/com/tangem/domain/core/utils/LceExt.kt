package com.tangem.domain.core.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.lce.Lce

fun <T : Any> lceLoading(partialContent: T? = null): Lce<Nothing, T> = Lce.Loading(partialContent)

fun <T : Any> T.lceContent(): Lce<Nothing, T> = Lce.Content(content = this)

fun <E : Any> E.lceError(): Lce<E, Nothing> = Lce.Error(error = this)

inline fun <E : Any, T> Lce<E, T>.getOrElse(ifLoading: (maybeContent: T?) -> T, ifError: (error: E) -> T): T = fold(
    ifLoading = ifLoading,
    ifContent = { it },
    ifError = ifError,
)

fun <E : Any, T> Lce<E, T>.getOrNull(): T? = fold(
    ifLoading = { partialContent -> partialContent },
    ifContent = { content -> content },
    ifError = { null },
)

inline fun <E : Any, T : Any> Lce<E, T>.toEither(ifLoading: (maybeContent: T?) -> T): Either<E, T> = fold(
    ifLoading = { ifLoading(it).right() },
    ifContent = { it.right() },
    ifError = { it.left() },
)
