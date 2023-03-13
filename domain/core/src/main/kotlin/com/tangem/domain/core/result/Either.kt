package com.tangem.domain.core.result

sealed class Either<out L, out R> {

    data class Left<out L>(val value: L) : Either<L, Nothing>()

    data class Right<out R>(val value: R) : Either<Nothing, R>()

    inline fun <C> fold(left: (L) -> C, right: (R) -> C): C {
        return when (this) {
            is Left -> left(value)
            is Right -> right(value)
        }
    }

    inline fun <R2> map(transform: (R) -> R2): Either<L, R2> {
        return fold(left = { Left(it) }, right = { Right(transform(it)) })
    }

    inline fun <L2> mapLeft(transform: (L) -> L2): Either<L2, R> {
        return fold(left = { Left(transform(it)) }, right = { Right(it) })
    }

    inline fun tap(block: (R) -> Unit): Either<L, R> {
        return also { if (it is Right) block(it.value) }
    }

    inline fun tapLeft(block: (L) -> Unit): Either<L, R> {
        return also { if (it is Left) block(it.value) }
    }
}

inline fun <R> catch(block: () -> R): Either<Throwable, R> {
    return try {
        Either.Right(block())
    } catch (e: Throwable) {
        Either.Left(e)
    }
}

inline fun <L, R, R2> Either<L, R>.flatMap(transform: (R) -> Either<L, R2>): Either<L, R2> {
    return when (this) {
        is Either.Left -> this.copy()
        is Either.Right -> transform(value)
    }
}
