package com.tangem.network.api.tangemTech

import retrofit2.http.GET
import retrofit2.http.Query

/**
[REDACTED_AUTHOR]
 */
interface TangemTechApi {

    @GET("coins/prices")
    suspend fun coinsPrices(
        @Query("currency") currency: String,
        @Query("ids") ids: String,
    ): Coins.PricesResponse

    @GET("coins/check-address")
    suspend fun coinsCheckAddress(
        @Query("contractAddress") contractAddress: String,
        @Query("networkId") networkId: String? = null,
    ): Coins.CheckAddressResponse

    @GET("coins/currencies")
    suspend fun coinsCurrencies(): Coins.CurrenciesResponse

    @GET("coins/tokens")
    suspend fun coinsTokens(): Coins.TokensResponse

}