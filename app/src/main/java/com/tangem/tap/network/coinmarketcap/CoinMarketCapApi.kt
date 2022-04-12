package com.tangem.tap.network.coinmarketcap

import com.tangem.network.common.AddHeaderInterceptor
import com.tangem.network.common.createRetrofitInstance
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinMarketCapApi {

    @GET("v1/tools/price-conversion")
    suspend fun getRateInfo(
        @Query("amount") amount: Int,
        @Query("symbol") cryptoCurrencyName: String,
        @Query("convert") fiatCurrencyName: String? = null
    ): RateInfoResponse

    @GET("v1/fiat/map")
    suspend fun getFiatMap(): FiatMapResponse


    companion object {
        private const val baseUrl = "https://pro-api.coinmarketcap.com/"

        fun create(apiKey: String): CoinMarketCapApi {
            return createRetrofitInstance(
                baseUrl = baseUrl,
                interceptors = listOf(
                    AddHeaderInterceptor(mapOf("X-CMC_PRO_API_KEY" to apiKey)),
                ),
            ).create(CoinMarketCapApi::class.java)
        }
    }
}