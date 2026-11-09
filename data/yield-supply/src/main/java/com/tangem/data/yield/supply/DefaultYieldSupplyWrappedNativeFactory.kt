package com.tangem.data.yield.supply

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory.Companion.WETH_CONTRACT_ADDRESS
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory.Companion.WETH_DECIMALS
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory.Companion.WETH_NAME
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory.Companion.WETH_RAW_ID
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory.Companion.WETH_SYMBOL
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
        if (network.toBlockchain() != Blockchain.Ethereum) return null

        return cryptoCurrencyFactory.createToken(
            network = network,
            rawId = WETH_RAW_ID,
            name = WETH_NAME,
            symbol = WETH_SYMBOL,
            decimals = WETH_DECIMALS,
            contractAddress = WETH_CONTRACT_ADDRESS,
        )
    }
}