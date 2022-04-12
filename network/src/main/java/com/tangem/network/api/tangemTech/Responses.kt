package com.tangem.network.api.tangemTech

import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface HttpResponse
interface TangemTechResponse : HttpResponse

sealed class Coins : TangemTechResponse {
    data class PricesResponse(val prices: List<Price>) : Coins() {
        data class Price(
            val name: String,
            val price: BigDecimal,
        )
    }

    data class CheckAddressResponse(val imageHost: String, val tokens: List<Token>, val total: Int) : Coins() {
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

    data class TokensResponse(val imageHost: String, val tokens: List<Token>, val total: Int) : Coins() {
        data class Token(
            val id: String,
            val name: String,
            val symbol: String,
            val contracts: List<Contract>?
        ) {
            data class Contract(
                val networkId: String,
                val address: String,
                val decimalCount: BigDecimal?,
            )
        }
    }

    data class CurrenciesResponse(val currencies: List<Currency>) {
        data class Currency(
            val id: String,
            val code: String,
            val name: String,
            val rateBTC: String,
            val unit: String,
            val type: String,
        )
    }
}