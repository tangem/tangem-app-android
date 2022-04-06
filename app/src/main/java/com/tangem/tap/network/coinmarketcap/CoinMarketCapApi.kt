package com.tangem.tap.network.coinmarketcap

import com.tangem.network.common.createRetrofitInstance
import okhttp3.Interceptor
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
                    baseUrl,
                    interceptors = listOf(createCoinMarketRequestInterceptor(apiKey)),
            ).create(CoinMarketCapApi::class.java)
        }
    }
}

private fun createCoinMarketRequestInterceptor(apiKey: String): Interceptor = Interceptor { chain ->
    val requestBuilder = chain.request().newBuilder()
    requestBuilder.addHeader("X-CMC_PRO_API_KEY", apiKey)
    chain.proceed(requestBuilder.build())
}