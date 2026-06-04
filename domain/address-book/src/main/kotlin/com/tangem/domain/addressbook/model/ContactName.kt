package com.tangem.domain.addressbook.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.Serializable

/**
 * Validated name of a [Contact].
 *
 * The only way to obtain an instance is the validating [invoke] factory, which enforces the
 * address-book naming rules. Uniqueness within a wallet is **not** enforced here — it requires
 * access to the repository and lives in `ValidateContactNameUseCase`.
 */
@Serializable
@ConsistentCopyVisibility
data class ContactName private constructor(val value: String) {

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

        const val MIN_LENGTH = 1
        const val MAX_LENGTH = 50

        /** Letters, numbers and spaces only — forbids emoji, new lines, tabs, special symbols and html/scripts. */
        private val allowedPattern = Regex("^[\\p{L}\\p{N} ]+$")

        operator fun invoke(value: String): Either<Error, ContactName> = either {
            val trimmed = value.trim()

            ensure(trimmed.length >= MIN_LENGTH) { Error.Empty }
            ensure(trimmed.length <= MAX_LENGTH) { Error.ExceedsMaxLength }
            ensure(allowedPattern.matches(trimmed)) { Error.InvalidCharacters }

            ContactName(trimmed)
        }
    }
}