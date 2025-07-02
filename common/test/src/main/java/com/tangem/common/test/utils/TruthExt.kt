package com.tangem.common.test.utils

import arrow.core.Either
import com.google.common.truth.Truth

fun <B> assertEither(actual: Either<Throwable, B>, expected: Either<Throwable, B>) {
    actual
        .onRight { Truth.assertThat(actual).isEqualTo(expected) }
        .onLeft {
            val expectedError = expected.leftOrNull() ?: error("Actual is Either.Left: $it")

            Truth.assertThat(it::class.java).isEqualTo(expectedError::class.java)
        }
}