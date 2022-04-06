package com.tangem.network.api.tangemTech

import com.tangem.common.services.Result
import retrofit2.http.GET
import retrofit2.http.Query

/**
[REDACTED_AUTHOR]
 */
interface TangemTechApi {

    @GET("coins/prices")
    suspend fun coinsPrices(
        @Query("currency") currency: String,
        @Query("ids") ids: List<String>,
    ): Result<CoinsPricesResponse>

    @GET("coins/check-address")
    suspend fun coinsCheckAddress(
        @Query("contractAddress") contractAddress: String,
        @Query("networkId") networkId: String? = null,
    ): Result<CoinsCheckAddressResponse>

    @GET("coins/currencies")
    suspend fun coinsCurrencies(): Result<CoinsCurrenciesResponse>

    @GET("coins/tokens")
    suspend fun coinsTokens(): Result<CoinsCurrenciesResponse>

}