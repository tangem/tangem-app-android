package com.tangem.domain.yield.supply

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.normalizeEvmAddress

/** Builds the token the Yield Module wraps the network coin into, even when it is not in the portfolio */
interface YieldSupplyWrappedNativeFactory {

    /** @return null when the wrapped native token of [network] is not known */
    fun create(network: Network): CryptoCurrency.Token?
}

/**
 * The module holds the Lending position of the network coin under its wrapped native token, so the module status of
 * that token is not a Yield Mode position of a portfolio token.
 *
 * @return true when [currency] is the wrapped native token of its network, compared by contract address
 */
fun YieldSupplyWrappedNativeFactory.isWrappedNative(currency: CryptoCurrency): Boolean {
    val token = currency as? CryptoCurrency.Token ?: return false
    val wrappedNative = create(token.network) ?: return false

    return wrappedNative.contractAddress.normalizeEvmAddress() == token.contractAddress.normalizeEvmAddress()
}