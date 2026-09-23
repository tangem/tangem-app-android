package com.tangem.domain.jointaccount.safe

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigInteger

/**
 * Proves that the composition the app knows controls the Safe the funds sit on: the Safe address must be
 * the composition's CREATE2 address, and a deployed Safe must also answer `getOwners()` and `getThreshold()`
 * with the same composition. Any mismatch blocks the creation.
 */
class SafeCompositionChecker(
    private val networkReader: JointNetworkReader,
    private val safeAddressCalculator: SafeAddressCalculator,
) {

    suspend fun ensureMatches(
        userWalletId: UserWalletId,
        network: Network,
        safeAddress: String,
        owners: List<String>,
        threshold: Int,
    ): Either<JointOperationCreateError, SafeDeployment> = either {
        checkAddress(safeAddress = safeAddress, owners = owners, threshold = threshold).bind()

        val code = networkReader
            .getCode(userWalletId = userWalletId, network = network)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        if (code.length <= EMPTY_CODE.length) return@either SafeDeployment.NOT_DEPLOYED

        checkDeployed(userWalletId = userWalletId, network = network, owners = owners, threshold = threshold).bind()
        SafeDeployment.DEPLOYED
    }

    private suspend fun checkDeployed(
        userWalletId: UserWalletId,
        network: Network,
        owners: List<String>,
        threshold: Int,
    ): Either<JointOperationCreateError, Unit> = either {
        val ownersResult = networkReader
            .callSafeContract(userWalletId = userWalletId, network = network, data = GET_OWNERS_SELECTOR)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()
        // A node answer the decoder cannot read is a failure of the read, not an on-chain mismatch
        val onChainOwners = Either.catch { decodeAddressArray(result = ownersResult) }
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        // The set sizes are compared too: a duplicated local owner must not collapse into a match
        ensure(onChainOwners.size == owners.size && onChainOwners == owners.map(String::lowercase).toSet()) {
            JointOperationCreateError.CompositionMismatch
        }

        val thresholdResult = networkReader
            .callSafeContract(userWalletId = userWalletId, network = network, data = GET_THRESHOLD_SELECTOR)
            .mapLeft(JointOperationCreateError::Failed)
            .bind()
        val onChainThreshold = Either.catch { decodeUint(result = thresholdResult) }
            .mapLeft(JointOperationCreateError::Failed)
            .bind()

        ensure(onChainThreshold == BigInteger.valueOf(threshold.toLong())) {
            JointOperationCreateError.CompositionMismatch
        }
    }

    private fun checkAddress(
        safeAddress: String,
        owners: List<String>,
        threshold: Int,
    ): Either<JointOperationCreateError, Unit> = either {
        val isAnySingletonMatching = SafeSingleton.entries.any { singleton ->
            safeAddressCalculator.compute(owners = owners, threshold = threshold, singleton = singleton)
                // A composition that cannot produce an address at all is broken data, not an on-chain mismatch
                .mapLeft { error ->
                    JointOperationCreateError.Failed(cause = IllegalStateException("Invalid composition: $error"))
                }
                .bind()
                .equals(safeAddress, ignoreCase = true)
        }

        ensure(isAnySingletonMatching) { JointOperationCreateError.CompositionMismatch }
    }

    /** `address[]` return data: one offset word, one length word, then one left-padded address per word */
    private fun decodeAddressArray(result: String): Set<String> {
        val words = result.removePrefix(HEX_PREFIX).chunked(size = WORD_HEX_LENGTH)
        val count = checkNotNull(words.getOrNull(index = 1)) { "getOwners() returned no length word: $result" }
            .toBigInteger(radix = HEX_RADIX)
            .toInt()
        val addresses = words.drop(n = 2)
        check(addresses.size >= count) { "getOwners() returned $count owners but ${addresses.size} words" }

        return addresses
            .take(count)
            .map { word -> "0x${word.takeLast(ADDRESS_HEX_LENGTH)}".lowercase() }
            .toSet()
    }

    private fun decodeUint(result: String): BigInteger {
        val hex = result.removePrefix(HEX_PREFIX)
        check(hex.isNotEmpty()) { "getThreshold() returned no data" }

        return BigInteger(hex, HEX_RADIX)
    }

    private companion object {
        const val GET_OWNERS_SELECTOR = "0xa0e67e2b"
        const val GET_THRESHOLD_SELECTOR = "0xe75235b8"

        const val EMPTY_CODE = "0x"
        const val HEX_PREFIX = "0x"
        const val HEX_RADIX = 16
        const val WORD_HEX_LENGTH = 64
        const val ADDRESS_HEX_LENGTH = 40
    }
}