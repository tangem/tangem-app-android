package com.tangem.data.walletmanager

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.SafeEthereumWalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Lazy
import java.math.BigInteger
import javax.inject.Inject

/**
 * [JointNetworkReader] on top of the joint network's [SafeEthereumWalletManager]: the same manager the balance
 * pipeline keeps, so every read here is made for the Safe address. The facade is lazy because it sits behind the
 * accounts fetcher, which reaches it through the tokens saver.
 */
internal class DefaultJointNetworkReader @Inject constructor(
    private val walletManagersFacade: Lazy<WalletManagersFacade>,
) : JointNetworkReader {

    override suspend fun getChainId(network: Network): Either<Throwable, Long> {
        val chainId = network.toBlockchain().getChainId()
            ?: return IllegalArgumentException("The network ${network.id} has no EVM chain id").left()

        return chainId.toLong().right()
    }

    override suspend fun getCode(userWalletId: UserWalletId, network: Network): Either<Throwable, String> {
        return withManager(userWalletId, network) { manager -> manager.getCode() }
    }

    override suspend fun getContractNonce(
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, BigInteger> {
        return withManager(userWalletId, network) { manager -> manager.getContractNonce() }
    }

    override suspend fun estimateGasLimit(
        userWalletId: UserWalletId,
        network: Network,
        to: String,
        value: BigInteger,
        data: String,
    ): Either<Throwable, BigInteger> {
        return withManager(userWalletId, network) { manager ->
            manager.estimateGasLimit(
                to = to,
                value = "0x${value.toString(HEX_RADIX)}",
                data = data.takeIf { it != EMPTY_CALLDATA },
            )
        }
    }

    override suspend fun getGasPrice(userWalletId: UserWalletId, network: Network): Either<Throwable, BigInteger> {
        return withManager(userWalletId, network) { manager -> manager.getGasPrice() }
    }

    override suspend fun callSafeContract(
        userWalletId: UserWalletId,
        network: Network,
        data: String,
    ): Either<Throwable, String> {
        return withManager(userWalletId, network) { manager -> manager.callSafeContract(data = data) }
    }

    /** No manager means the account has no Safe address yet — an operation cannot be built for it */
    private suspend fun <T> withManager(
        userWalletId: UserWalletId,
        network: Network,
        read: suspend (SafeEthereumWalletManager) -> Result<T>,
    ): Either<Throwable, T> = either {
        val manager = walletManagersFacade.get().getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = network,
        )
        val safeManager = ensureNotNull(manager as? SafeEthereumWalletManager) {
            IllegalStateException("No Safe wallet manager for the joint network ${network.id}")
        }

        when (val result = read(safeManager)) {
            is Result.Success -> result.data
            is Result.Failure -> raise(r = result.error)
        }
    }

    private companion object {
        const val HEX_RADIX = 16
        const val EMPTY_CALLDATA = "0x"
    }
}