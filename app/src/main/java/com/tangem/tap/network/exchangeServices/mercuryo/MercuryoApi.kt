package com.tangem.tap.network.exchangeServices.mercuryo

import com.squareup.moshi.Json
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

data class MercuryoCurrenciesResponse(
    val status: Int,
    val data: Data,
) {
    data class Data(
        val fiat: List<String>,
        val crypto: List<String>,
        val config: Config,
    )

    data class Config(
        @Json(name = "crypto_currencies")
        val cryptoCurrencies: List<MercuryoCryptoCurrency>,
    )

    data class MercuryoCryptoCurrency(
        @Json(name = "currency")
        val currencySymbol: String,
        @Json(name = "network")
        val network: String,
        @Json(name = "contract")
        val contractAddress: String,
    )
}
