package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.Serializable

/**
 * Represents a derivation index for accounts, ensuring validity and providing utility methods
 *
 * @property value  the integer value of the derivation index
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class DerivationIndex private constructor(
    val value: Int,
) {

    /** Checks if the derivation index corresponds to the main account */
    val isMain: Boolean
        get() = value == MAIN_ACCOUNT_DERIVATION_INDEX

    /**
     * Represents possible errors that can occur when creating a [DerivationIndex]
     */
    @Serializable
    sealed interface Error {

        /** Error indicating that the provided derivation index [derivationIndex] is invalid */
        @Serializable
        data class NegativeDerivationIndex(val derivationIndex: Int) : Error {
            override fun toString(): String {
                return "${this::class.simpleName}: Derivation index cannot be negative: $derivationIndex"
            }
        }
    }

    companion object {

        private const val MAIN_ACCOUNT_DERIVATION_INDEX = 0

        /** Predefined instance of [DerivationIndex] for the main account */
        val Main: DerivationIndex = DerivationIndex(value = MAIN_ACCOUNT_DERIVATION_INDEX)

        /**
         * Factory method to create a [DerivationIndex] instance
         *
         * @param value the integer value of the derivation index
         *
         * @return Either an error if the value is invalid, or a valid [DerivationIndex] instance
         */
        operator fun invoke(value: Int): Either<Error, DerivationIndex> = either {
            ensure(value >= 0) { Error.NegativeDerivationIndex(derivationIndex = value) }

            DerivationIndex(value)
        }
    }
}