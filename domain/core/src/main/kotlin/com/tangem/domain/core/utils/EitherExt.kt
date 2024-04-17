package com.tangem.domain.core.utils

import arrow.core.Either
import com.tangem.domain.core.lce.Lce
import kotlinx.coroutines.flow.Flow

/**
 * [Flow] of [Either]
 *
 * @param E type of left value
 * @param A type of right value
 * */
typealias EitherFlow<E, A> = Flow<Either<E, A>>

/**
 * Converts an [Either] instance to a [Lce] instance.
 * If this is a [Either.Left], it is converted to a [Lce.Error] with the same error.
 * If this is a [Either.Right], it is converted to a [Lce.Content] or [Lce.Loading] with the same content,
 * depending on the [isStillLoading] parameter.
 *
 * @param isStillLoading A flag indicating whether the content is still loading.
 * If true, the [Either.Right] is converted to a [Lce.Loading].
 * @return A [Lce] instance containing the same content or error as this [Either],
 * and possibly indicating a loading state.
 */
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