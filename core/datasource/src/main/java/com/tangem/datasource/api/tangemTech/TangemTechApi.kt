package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.api.tangemTech.models.GeoResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
* [REDACTED_AUTHOR]
 */
interface TangemTechApi {

    @GET("coins")
    suspend fun coins(
        @Query("contractAddress") contractAddress: String? = null,
        @Query("exchangeable") exchangeable: Boolean? = null,
        @Query("networkIds") networkIds: String? = null,
        @Query("active") active: Boolean? = null,
        @Query("searchText") searchText: String? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
    ): CoinsResponse

    @GET("rates")
    suspend fun getRates(
        @Query("currencyId") currencyId: String,
        @Query("coinIds") coinIds: String,
    ): RatesResponse

    @GET("currencies")
    suspend fun getCurrencyList(): CurrenciesResponse

    @GET("geo")
    suspend fun getUserCountryCode(): GeoResponse

    @GET("user-tokens/{user-id}")
    suspend fun getUserTokens(@Path(value = "user-id") userId: String): UserTokensResponse

    @PUT("user-tokens/{user-id}")
    suspend fun saveUserTokens(@Path(value = "user-id") userId: String, @Body userTokens: UserTokensResponse)
}
