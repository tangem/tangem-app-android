package com.tangem.datasource.api.tangemTech

import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 02/04/2022.
 */
interface HttpResponse
sealed interface TangemTechResponse : HttpResponse

data class CoinsResponse(
    val imageHost: String?,
    val coins: List<Coin>,
    val total: Int,
) : TangemTechResponse {

    data class Coin(
        val id: String,
        val name: String,
        val symbol: String,
        val active: Boolean,
        val networks: List<Network> = listOf(),
    ) : TangemTechResponse {

        data class Network(
            val networkId: String,
            val contractAddress: String? = null,
            val decimalCount: BigDecimal? = null,
            val exchangeable: Boolean? = false,
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
        val type: String,
    ) : TangemTechResponse
}

data class GeoResponse(
    val code: String,
) : TangemTechResponse

data class UserTokensResponse(
    val version: Int = 0,
    val group: String? = null,
    val sort: String? = null,
    val tokens: List<TokenResponse> = emptyList(),
) : TangemTechResponse

data class TokenResponse(
    val id: String? = null,
    val networkId: String,
    val derivationPath: String? = null,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val contractAddress: String?,
) : TangemTechResponse

data class TangemTechError(
    val code: Int,
    val description: String,
)
