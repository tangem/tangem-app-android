package com.tangem.tap.network.exchangeServices.mercuryo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

/**
* [REDACTED_AUTHOR]
 *
 * Используется для получения списка доступных криптовалют (data/crypto в JSON-е) и сетей,
 * в которых эти криптовалюты представлены (data/config/base в JSON-е).
 * USDT, например, представлен только в ETH; BUSD в BEP-20
 *
 * Так как у Ethereum/Arbitrum и BSC/Binance одинаковые символы валют, то нужно arbitrum/binance
 * исключать из сетей, в которых доступны токены
 */
interface MercuryoApi {

    @GET("{apiVersion}/lib/currencies")
    suspend fun currencies(@Path("apiVersion") apiVersion: String): MercuryoCurrenciesResponse
}

@JsonClass(generateAdapter = true)
data class MercuryoCurrenciesResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "data") val data: Data,
) {
    @JsonClass(generateAdapter = true)
    data class Data(
        @Json(name = "fiat") val fiat: List<String>,
        @Json(name = "crypto") val crypto: List<String>,
        @Json(name = "config") val config: Config,
    )

    @JsonClass(generateAdapter = true)
    data class Config(
        @Json(name = "crypto_currencies")
        val cryptoCurrencies: List<MercuryoCryptoCurrency>,
    )

    @JsonClass(generateAdapter = true)
    data class MercuryoCryptoCurrency(
        @Json(name = "currency")
        val currencySymbol: String,
        @Json(name = "network")
        val network: String,
        @Json(name = "contract")
        val contractAddress: String,
    )
}
