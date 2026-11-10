package com.tangem.data.yield.supply

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultYieldSupplyWrappedNativeFactory @Inject constructor(
    excludedBlockchains: ExcludedBlockchains,
) : YieldSupplyWrappedNativeFactory {

    private val cryptoCurrencyFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        CryptoCurrencyFactory(excludedBlockchains)
    }

    override fun create(network: Network): CryptoCurrency.Token? {
        val wrappedNative = WRAPPED_NATIVES[network.toBlockchain()] ?: return null

        return cryptoCurrencyFactory.createToken(
            network = network,
            rawId = CryptoCurrency.RawID(wrappedNative.rawId),
            name = wrappedNative.symbol,
            symbol = wrappedNative.symbol,
            decimals = wrappedNative.decimals,
            contractAddress = wrappedNative.contractAddress,
        )
    }

    /** The module recognises the wrapped coin by its address, not by the symbol */
    private data class WrappedNative(
        val contractAddress: String,
        val symbol: String,
        val rawId: String,
        val decimals: Int,
    )

    private companion object {

        /**
         * Kept in the app rather than read from the module: whether Lending is offered must be known without a network
         * call. A new network is one more row
         */
        val WRAPPED_NATIVES = mapOf(
            Blockchain.Ethereum to WrappedNative(
                contractAddress = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2",
                symbol = "WETH",
                rawId = "weth",
                decimals = 18,
            ),
        )
    }
}