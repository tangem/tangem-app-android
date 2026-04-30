package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.Serializable

@Serializable
@ConsistentCopyVisibility
data class CardDisplayName private constructor(val value: String) {

    @Serializable
    sealed interface Error {
        @Serializable
        data object Empty : Error

        @Serializable
        data object ExceedsMaxLength : Error

        @Serializable
        data object InvalidCharacters : Error
    }

    companion object {
        const val MAX_LENGTH = 20
        private val allowedPattern = Regex("^[\\p{L}\\p{N} ]+$")

        operator fun invoke(name: String): Either<Error, CardDisplayName> = either {
            val trimmed = name.trim()
            ensure(trimmed.isNotEmpty()) { Error.Empty }
            ensure(trimmed.length <= MAX_LENGTH) { Error.ExceedsMaxLength }
            ensure(allowedPattern.matches(trimmed)) { Error.InvalidCharacters }
            CardDisplayName(trimmed)
        }
    }
}