package com.tangem.domain.core.chain

import com.tangem.domain.core.result.Either

sealed class ChainResult<out R> {
    object Initial : ChainResult<Nothing>()
    data class Success<out R>(val result: R) : ChainResult<R>()
    data class Failure(val error: Throwable) : ChainResult<Nothing>()

    inline fun <L> toEither(onFailure: (Throwable) -> L): Either<L, R> = when (this) {
        is Initial -> Either.Left(onFailure(EmptyChains))
        is Failure -> Either.Left(onFailure(error))
        is Success -> Either.Right(result)
    }
}

inline fun <R> ChainResult<R>.successOr(block: () -> R): R {
    return when (this) {
        is ChainResult.Initial -> block()
        is ChainResult.Failure -> block()
        is ChainResult.Success -> result
    }
}

fun <R> Result<R>.toChainResult(): ChainResult<R> {
    return this.fold(
        onSuccess = { ChainResult.Success(it) },
        onFailure = { ChainResult.Failure(it) },
    )
}

object EmptyChains : Exception()
