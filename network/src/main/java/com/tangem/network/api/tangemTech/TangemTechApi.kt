package com.tangem.network.api.tangemTech

import retrofit2.http.GET
import retrofit2.http.Query

/**
[REDACTED_AUTHOR]
 */
interface TangemTechApi {

    @GET("coins")
    suspend fun coins(
        @Query("contractAddress") contractAddress: String? = null,
        @Query("networkId") networkId: String? = null,
        @Query("active") active: Boolean? = null,
    ): CoinsResponse

    @GET("rates")
    suspend fun rates(
        @Query("currencyId") currencyId: String,
        @Query("coinIds") coinIds: String,
    ): RatesResponse

    @GET("currencies")
    suspend fun currencies(): CurrenciesResponse

}