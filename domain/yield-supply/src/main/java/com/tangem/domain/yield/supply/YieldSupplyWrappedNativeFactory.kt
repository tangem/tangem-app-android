package com.tangem.domain.yield.supply

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/** Builds the token the Yield Module wraps the network coin into, even when it is not in the portfolio */
interface YieldSupplyWrappedNativeFactory {

    /** @return null when the wrapped native token of [network] is not known */
    fun create(network: Network): CryptoCurrency.Token?

    companion object {
        /** WETH9: the module recognises WETH by this address, not by the symbol */
        const val WETH_CONTRACT_ADDRESS = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
        const val WETH_SYMBOL = "WETH"
        const val WETH_NAME = "WETH"
        const val WETH_DECIMALS = 18
        val WETH_RAW_ID = CryptoCurrency.RawID("weth")
    }
}