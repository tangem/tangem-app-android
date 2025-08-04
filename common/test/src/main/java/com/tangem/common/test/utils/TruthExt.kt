package com.tangem.common.test.utils

import arrow.core.Either
import com.google.common.truth.Truth

fun <B> assertEither(actual: Either<Throwable, B>, expected: Either<Throwable, B>) {
    actual
        .onRight { Truth.assertThat(actual).isEqualTo(expected) }
        .onLeft {
            val expectedError = expected.leftOrNull() ?: error("Actual is Either.Left: $it")

            Truth.assertThat(it).isInstanceOf(expectedError::class.java)
            Truth.assertThat(it).hasMessageThat().isEqualTo(expectedError.message)
        }
}

fun assertEitherRight(actual: Either<Throwable, Unit>) {
    actual
        .onRight { Truth.assertThat(actual).isEqualTo(Either.Right(Unit)) }
        .onLeft {
            error("Actual is Either.Left: $it")
        }
}

fun <B> assertEitherLeft(actual: Either<Throwable, B>, expected: Throwable) {
    actual
        .onRight { error("Actual is Either.Right: $it") }
        .onLeft {
            Truth.assertThat(it::class.java).isEqualTo(expected::class.java)
            Truth.assertThat(it).hasMessageThat().isEqualTo(expected.message)
        }
}