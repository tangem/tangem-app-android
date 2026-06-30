package com.tangem.domain.addressbook.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.addressbook.model.ContactName.Companion.invoke
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

        private const val MIN_LENGTH = 1
        const val MAX_LENGTH = 50

        /**
         * Allows letters of any locale (`\p{L}`) and their combining marks (`\p{M}`, which also covers emoji
         * variation selectors and keycap marks), digits (`\p{N}`), a regular space, and emoji — symbols (`\p{So}`,
         * including flags / regional indicators), emoji skin-tone modifiers (`\p{Sk}`) and the zero-width joiner
         * (U+200D) used in emoji sequences.
         *
         * Everything else is rejected, which covers the forbidden set: line breaks, tabs and other control
         * characters, invisible/format unicode (zero-width spaces, BOM, …), exotic spaces, and HTML/script symbols.
         *
         * The leading lookahead requires at least one visible "base" character (letter / digit / emoji symbol), so a
         * name made up only of zero-width joiners, combining marks, modifiers or spaces (i.e. effectively invisible)
         * is rejected.
         */
        private val allowedPattern =
            Regex("^(?=.*[\\p{L}\\p{N}\\p{So}])[\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\u0020\\u200D]+$")

        operator fun invoke(value: String): Either<Error, ContactName> = either {
            val trimmed = value.trim()

            ensure(trimmed.length >= MIN_LENGTH) { Error.Empty }
            ensure(trimmed.length <= MAX_LENGTH) { Error.ExceedsMaxLength }
            ensure(allowedPattern.matches(trimmed)) { Error.InvalidCharacters }

            ContactName(trimmed)
        }
    }
}