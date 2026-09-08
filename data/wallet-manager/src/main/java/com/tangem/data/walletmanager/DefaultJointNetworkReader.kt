package com.tangem.data.walletmanager

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.blockchain.blockchains.ethereum.ExternalAddressBalanceProvider
import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import dagger.Lazy
import java.math.BigInteger
import javax.inject.Inject

/**
 * [JointNetworkReader] on top of the joint account's [ExternalAddressBalanceProvider]. A provider already

 * reads and deliberately NOT stored — the balance pipeline owns the cache, and racing it with a provider
 * that carries no balance state would clobber a stateful one.
 */
internal class DefaultJointNetworkReader @Inject constructor(
    private val externalAddressBalanceProvidersStore: ExternalAddressBalanceProvidersStore,
    private val jointAccountSafeAddressResolver: Lazy<JointAccountSafeAddressResolver>,
    blockchainSDKFactory: BlockchainSDKFactory,
) : JointNetworkReader {

    private val walletManagerFactory by lazy { WalletManagerFactory(blockchainSDKFactory) }

    override suspend fun getChainId(network: Network): Either<Throwable, Long> {
        val chainId = network.toBlockchain().getChainId()
            ?: return IllegalArgumentException("The network ${network.id} has no EVM chain id").left()

        return chainId.toLong().right()
    }

    override suspend fun getCode(userWalletId: UserWalletId, network: Network): Either<Throwable, String> {
        return withProvider(userWalletId, network) { provider -> provider.getCode(address = provider.address) }
    }

    override suspend fun getContractNonce(
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, BigInteger> {
        return withProvider(userWalletId, network) { provider ->
            provider.getContractNonce(address = provider.address)
        }
    }

    override suspend fun estimateGasLimit(
        userWalletId: UserWalletId,
        network: Network,
        to: String,
        value: BigInteger,
        data: String,
    ): Either<Throwable, BigInteger> {
        return withProvider(userWalletId, network) { provider ->
            provider.getGasLimit(
                to = to,
                from = provider.address,
                value = "0x${value.toString(HEX_RADIX)}",
                data = data.takeIf { it != EMPTY_CALLDATA },
            )
        }
    }

    override suspend fun getGasPrice(userWalletId: UserWalletId, network: Network): Either<Throwable, BigInteger> {
        return withProvider(userWalletId, network) { provider -> provider.getGasPrice() }
    }

    override suspend fun callSafeContract(
        userWalletId: UserWalletId,
        network: Network,
        data: String,
    ): Either<Throwable, String> {
        return withProvider(userWalletId, network) { provider ->
            provider.callContract(data = ContractCallData(to = provider.address, data = data))
        }
    }

    private suspend fun <T> withProvider(
        userWalletId: UserWalletId,
        network: Network,
        read: suspend (ExternalAddressBalanceProvider) -> Result<T>,
    ): Either<Throwable, T> = either {
        val provider = externalAddressBalanceProvidersStore.getSyncOrNull(userWalletId, network)
            ?: createTransientProvider(userWalletId = userWalletId, network = network).bind()

        when (val result = read(provider)) {
            is Result.Success -> result.data
            is Result.Failure -> raise(r = result.error)
        }
    }

    private suspend fun createTransientProvider(
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, ExternalAddressBalanceProvider> = either {
        val safeAddress = jointAccountSafeAddressResolver.get()
            .resolve(userWalletId, setOf(network))[network]
        ensureNotNull(safeAddress) {
            IllegalStateException("No Safe address for the joint network ${network.id}")
        }

        val provider = walletManagerFactory.createExternalAddressBalanceProvider(
            blockchain = network.toBlockchain(),
            address = safeAddress,
        )

        ensureNotNull(provider) {
            IllegalStateException("Unable to create a balance provider for the joint network ${network.id}")
        }
    }

    private companion object {
        const val HEX_RADIX = 16
        const val EMPTY_CALLDATA = "0x"
    }
}