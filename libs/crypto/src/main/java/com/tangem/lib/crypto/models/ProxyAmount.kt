package com.tangem.lib.crypto.models

import java.math.BigDecimal

/**
 * Proxy amount is always has a Coin type
 *
 * @property currencySymbol
 * @property value amount in [BigDecimal]
 * @property decimals count for token
 */
data class ProxyAmount(
    val currencySymbol: String,
    var value: BigDecimal,
    val decimals: Int,
) {

    companion object {
        fun empty(): ProxyAmount {
            return ProxyAmount("", BigDecimal.ZERO, 0)
        }
    }
}