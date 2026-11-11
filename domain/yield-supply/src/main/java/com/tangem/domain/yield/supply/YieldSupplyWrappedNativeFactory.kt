package com.tangem.domain.yield.supply

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/** Builds the token the Yield Module wraps the network coin into, even when it is not in the portfolio */
interface YieldSupplyWrappedNativeFactory {

    /** @return null when the wrapped native token of [network] is not known */
    fun create(network: Network): CryptoCurrency.Token?
}