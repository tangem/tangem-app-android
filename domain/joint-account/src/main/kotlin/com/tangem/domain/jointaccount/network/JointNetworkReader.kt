package com.tangem.domain.jointaccount.network

import arrow.core.Either
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigInteger

/**
 * On-chain reads against the Safe of the wallet's joint account in [Network] — the inputs of building
 * and vetting a `SafeTx`. Every method resolves the Safe address itself, targets the account's own
 * network provider, and answers a `Left` instead of throwing.
 */
interface JointNetworkReader {

    /** The EIP-155 chain id of [network] */
    suspend fun getChainId(network: Network): Either<Throwable, Long>

    /** The bytecode at the Safe address as a `0x`-prefixed hex string; `0x` while the Safe is not deployed */
    suspend fun getCode(userWalletId: UserWalletId, network: Network): Either<Throwable, String>

    suspend fun isSafeDeployed(userWalletId: UserWalletId, network: Network): Either<Throwable, Boolean> {
        return getCode(userWalletId = userWalletId, network = network).map { code -> code != EMPTY_CODE }
    }

    /** The Safe contract `nonce()`; only meaningful for a deployed Safe */
    suspend fun getContractNonce(userWalletId: UserWalletId, network: Network): Either<Throwable, BigInteger>

    /** `eth_estimateGas` of a call from the Safe address to [to] with [value] wei and [data] calldata */
    suspend fun estimateGasLimit(
        userWalletId: UserWalletId,
        network: Network,
        to: String,
        value: BigInteger,
        data: String,
    ): Either<Throwable, BigInteger>

    /** The network's current legacy gas price estimate, in wei */
    suspend fun getGasPrice(userWalletId: UserWalletId, network: Network): Either<Throwable, BigInteger>

    /** `eth_call` to the Safe contract itself with [data] calldata; returns the raw `0x`-prefixed result */
    suspend fun callSafeContract(userWalletId: UserWalletId, network: Network, data: String): Either<Throwable, String>

    companion object {
        const val EMPTY_CODE = "0x"
    }
}