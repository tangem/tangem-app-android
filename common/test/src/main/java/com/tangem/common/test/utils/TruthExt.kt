package com.tangem.common.test.utils

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import com.google.common.truth.Truth

fun <B> assertEither(actual: Either<Throwable, B>, expected: Either<Throwable, B>) {
    actual
        .onRight { Truth.assertThat(actual).isEqualTo(expected) }
        .onLeft { throwable ->
            val expectedError = expected.leftOrNull() ?: error("Actual is Either.Left: $throwable")

            Truth.assertThat(throwable).isInstanceOf(expectedError::class.java)
            Truth.assertThat(throwable).hasMessageThat().isEqualTo(expectedError.message)
        }
}

fun assertEitherRight(actual: Either<Throwable, Unit>) {
    actual
        .onRight { Truth.assertThat(actual).isEqualTo(Either.Right(Unit)) }
        .onLeft {
            error("Actual is Either.Left: $it")
        }
}

@Suppress("NullableToStringCall")
fun <B> assertEitherLeft(actual: Either<Throwable, B>, expected: Throwable) {
    actual
        .onRight { error("Actual is Either.Right: $it") }
        .onLeft { throwable ->
            Truth.assertThat(throwable::class.java).isEqualTo(expected::class.java)
            Truth.assertThat(throwable).hasMessageThat().isEqualTo(expected.message)
        }
}

fun <B> assertNone(actual: Option<B>) {
    Truth.assertThat(actual).isEqualTo(None)
}

fun <B> assertSome(actual: Option<B>, expected: B) {
    actual
        .onNone { error("Actual is None") }
        .onSome {
            Truth.assertThat(it).isEqualTo(expected)
        }
}