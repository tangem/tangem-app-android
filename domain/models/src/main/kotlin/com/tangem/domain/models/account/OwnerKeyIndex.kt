package com.tangem.domain.models.account

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.Serializable

/**
 * The index of the participant's owner key of a joint account: the last node of `m/44'/60'/888888'/0/{index}`.
 *
 * Not a [DerivationIndex]. That one indexes the wallet's own accounts, names a main one and is substituted into the
 * account node of a currency path; none of it applies to an owner key. The two spaces are independent, and equal
 * values in them mean nothing.
 *
 * @property value the integer value of the index
 */
@Serializable
data class OwnerKeyIndex private constructor(
    val value: Int,
) {

    /** Represents possible errors that can occur when creating an [OwnerKeyIndex] */
    @Serializable
    sealed interface Error {

        /** Error indicating that the provided index [ownerKeyIndex] is negative */
        @Serializable
        data class NegativeOwnerKeyIndex(val ownerKeyIndex: Int) : Error {
            override fun toString(): String {
                return "${this::class.simpleName}: Owner key index cannot be negative: $ownerKeyIndex"
            }
        }
    }

    companion object {

        /**
         * Factory method to create an [OwnerKeyIndex] instance
         *
         * @param value the integer value of the index
         *
         * @return Either an error if the value is invalid, or a valid [OwnerKeyIndex] instance
         */
        operator fun invoke(value: Int): Either<Error, OwnerKeyIndex> = either {
            ensure(value >= 0) { Error.NegativeOwnerKeyIndex(ownerKeyIndex = value) }

            OwnerKeyIndex(value)
        }
    }
}