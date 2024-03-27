package com.tangem.domain.core.utils

import arrow.core.Either
import com.tangem.domain.core.lce.Lce

inline fun <reified E : Any, reified T : Any> Either<E, T>.toLce(isStillLoading: Boolean = false): Lce<E, T> {
    return when (this) {
        is Either.Left -> Lce.Error(value)
        is Either.Right -> {
            if (isStillLoading) {
                Lce.Loading(value)
            } else {
                Lce.Content(value)
            }
        }
    }
}
