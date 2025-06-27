package com.tangem.common.test.utils

import arrow.core.Either
import com.google.common.truth.Truth

fun <B> assertEither(actual: Either<Throwable, B>, expected: Either<Throwable, B>) {
    actual
        .onRight { Truth.assertThat(it).isEqualTo(expected) }
        .onLeft {
            val expectedError = expected.leftOrNull() ?: error("Expected must be Either.Left")

            Truth.assertThat(it::class.java).isEqualTo(expectedError::class.java)
        }
}