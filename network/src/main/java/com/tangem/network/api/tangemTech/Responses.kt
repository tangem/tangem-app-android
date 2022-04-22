package com.tangem.network.api.tangemTech

import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface HttpResponse
sealed interface TangemTechResponse : HttpResponse

data class CoinsResponse(
    val imageHost: String,
    val coins: List<Coin>,
    val total: Int
) : TangemTechResponse {

    data class Coin(
        val id: String,
        val name: String,
        val symbol: String,
        val active: Boolean,
        val networks: List<Network> = listOf()
    ) : TangemTechResponse {

        data class Network(
            val networkId: String,
            val contractAddress: String? = null,
            val decimalCount: BigDecimal? = null,
        ) : TangemTechResponse
    }
}

//rates.keys = networkId's
data class RatesResponse(val rates: Map<String, Double>) : TangemTechResponse

data class CurrenciesResponse(val currencies: List<Currency>) {

    data class Currency(
        val id: String,
        val code: String,       // this is an uppercase id
        val name: String,
        val rateBTC: String,
        val unit: String,       // $, €, ₽
        val type: CurrencyType,
    ) : TangemTechResponse

    enum class CurrencyType(val type: String) {
        Fiat("fiat"), Crypto("crypto")
    }
}