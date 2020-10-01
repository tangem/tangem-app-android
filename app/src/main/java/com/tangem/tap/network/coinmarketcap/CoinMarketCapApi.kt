package com.tangem.tap.network.coinmarketcap

import com.tangem.tap.TapConfig
import com.tangem.tap.network.createRetrofitInstance
import okhttp3.Interceptor
import okhttp3.Response
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

        fun create(): CoinMarketCapApi {
            return createRetrofitInstance(
                    baseUrl,
                    listOf(createCoinMarketRequestInterceptor()),
            ).create(CoinMarketCapApi::class.java)
        }
    }
}

private fun createCoinMarketRequestInterceptor(): Interceptor {
    return object : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val requestBuilder = chain.request().newBuilder()
            requestBuilder.addHeader("X-CMC_PRO_API_KEY", TapConfig.coinMarketCapKey)
            return chain.proceed(requestBuilder.build())
        }
    }
}