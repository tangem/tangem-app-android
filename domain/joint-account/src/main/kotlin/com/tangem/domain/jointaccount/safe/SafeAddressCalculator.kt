package com.tangem.domain.jointaccount.safe

import arrow.core.Either

/**
 * The counterfactual Safe address of a composition: known before the contract exists and identical on every
 * supported network. The formula is Safe's own (CREATE2 over the `setup` initializer), and the bytes it hashes are
 * the blockchain SDK's business, so the implementation lives in the data layer.
 *
 * A composition is validated before anything is hashed, because every malformed input has a silent failure mode:
 * an odd-length hex string loses its last nibble, an address that is not 20 bytes shifts the ABI word it is padded
 * into, a negative threshold encodes as a small positive number. All of them produce a well-formed address that
 * simply is not the account's — the worst possible outcome for a value funds are sent to.
 */
interface SafeAddressCalculator {

    fun compute(owners: List<String>, threshold: Int, singleton: SafeSingleton): Either<Error, String>

    /** Errors of a composition that cannot belong to any deployable Safe. */
    sealed interface Error {

        /** A Safe without owners cannot be set up */
        data object NoOwners : Error {
            override fun toString(): String = "${this::class.simpleName}: The owners list is empty"
        }

        /** The owner [address] is not a `0x`-prefixed 20-byte hex address */
        data class MalformedOwnerAddress(val address: String) : Error {
            override fun toString(): String = "${this::class.simpleName}: Not a 20-byte hex address: $address"
        }

        /** The [address] appears among the owners more than once, case-insensitively */
        data class DuplicateOwners(val address: String) : Error {
            override fun toString(): String = "${this::class.simpleName}: Duplicate owner: $address"
        }

        /** The [threshold] is outside `1..ownersCount`, so no signature set can ever satisfy it */
        data class ThresholdOutOfRange(val threshold: Int, val ownersCount: Int) : Error {
            override fun toString(): String =
                "${this::class.simpleName}: Threshold $threshold is not in 1..$ownersCount"
        }
    }
}