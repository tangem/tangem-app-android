package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.Serializable

/**
 * Represents an account name
 *
 * @property value the validated account name as a string
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountName private constructor(
    val value: String,
) {

    /**
     * Represents possible validation errors
     */
    @Serializable
    sealed interface Error {

        /**
         * Error indicating that the account name is blank
         */
        @Serializable
        data object Empty : Error {
            override fun toString(): String = "${Empty::class.simpleName}: Account name cannot be blank"
        }

        /**
         * Error indicating that the account name exceeds the maximum allowed length
         */
        @Serializable
        data object ExceedsMaxLength : Error {
            override fun toString(): String {
                return "${ExceedsMaxLength::class.simpleName}: Account name cannot exceed $MAX_LENGTH characters"
            }
        }
    }

    companion object {

        private const val MAX_LENGTH = 20

        /**
         * Factory method to create an `AccountName` instance.
         * Validates the input string to ensure it is not blank and does not exceed the maximum length.
         *
         * @param value the input string representing the account name
         */
        operator fun invoke(value: String): Either<Error, AccountName> = either {
            val trimmedValue = value.trim()

            ensure(trimmedValue.isNotBlank()) { Error.Empty }
            ensure(trimmedValue.length <= MAX_LENGTH) { Error.ExceedsMaxLength }

            AccountName(value = trimmedValue)
        }
    }
}