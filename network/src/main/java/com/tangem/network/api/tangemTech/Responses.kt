package com.tangem.network.api.tangemTech

import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface HttpResponse
sealed interface TangemTechResponse : HttpResponse

sealed class Coins : TangemTechResponse {
    data class PricesResponse(val prices: Map<String, Double>) : Coins()

    data class CheckAddressResponse(val imageHost: String?, val tokens: List<Token>, val total: Int) : Coins() {
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
            val code: String,       // this is an uppercase id
            val name: String,
            val rateBTC: String,
            val unit: String,       // $, €, ₽
            val type: String,
        )

        enum class CurrencyType(val type: String) {
            Fiat("fiat"), Crypto("crypto")
        }
    }
}