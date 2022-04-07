package com.tangem.network.api.tangemTech

import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
data class CoinsPricesResponse(
    val prices: List<CoinPrice>
)

data class CoinPrice(
    val name: String,
    val price: BigDecimal,
)

data class CoinsCheckAddressResponse(
    val imageHost: String,
    val tokens: List<Token>,
    val total: Int,
) {
    data class Token(
        val id: String,
        val name: String,
        val symbol: String,
        val active: Boolean,
        val contracts: List<Contract>
    ) {
        data class Contract(
            val networkId: String,
            val address: String,
            val decimalCount: BigDecimal?,
            val active: Boolean
        )
    }
}

data class CoinsCurrenciesResponse(
    val currencies: List<Currency>,
) {
    data class Currency(
        val id: String,
        val code: String,
        val name: String,
        val rateBTC: String,
        val unit: String,
        val type: String,
    )
}

