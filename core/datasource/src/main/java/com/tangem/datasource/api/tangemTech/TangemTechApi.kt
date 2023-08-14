package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.tangemTech.models.*
import retrofit2.http.*

/**
 * Interface of Tangem Tech API
 *
[REDACTED_AUTHOR]
 */
interface TangemTechApi {

    @GET("coins")
    suspend fun getCoins(
        @Query("contractAddress") contractAddress: String? = null,
        @Query("exchangeable") exchangeable: Boolean? = null,
        @Query("networkIds") networkIds: String? = null,
        @Query("active") active: Boolean? = null,
        @Query("searchText") searchText: String? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
    ): CoinsResponse

    @GET("rates")
    suspend fun getRates(@Query("currencyId") currencyId: String, @Query("coinIds") coinIds: String): RatesResponse

    @GET("currencies")
    suspend fun getCurrencyList(): CurrenciesResponse

    @GET("geo")
    suspend fun getUserCountryCode(): GeoResponse

    @GET("user-tokens/{user-id}")
    suspend fun getUserTokens(@Path(value = "user-id") userId: String): UserTokensResponse

    @PUT("user-tokens/{user-id}")
    suspend fun saveUserTokens(@Path(value = "user-id") userId: String, @Body userTokens: UserTokensResponse)

    /** Returns referral status by [walletId] */
    @GET("referral/{walletId}")
    suspend fun getReferralStatus(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("walletId") walletId: String,
    ): ReferralResponse

    /** Make user referral, requires [StartReferralBody] */
    @POST("referral")
    suspend fun startReferral(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Body startReferralBody: StartReferralBody,
    ): ReferralResponse

    @GET("shops")
    suspend fun getShopInfo(@Query(value = "name") name: String): ShopResponse

    @GET("sales")
    suspend fun getSalesInfo(
        @Query(value = "locale") locale: String,
        @Query(value = "shops") shops: String,
    ): SalesResponse

    @GET("quotes")
    suspend fun getQuotes(
        @Query("currencyId") currencyId: String,
        @Query("coinIds") coinIds: String,
        @Query("fields") fields: String = "price,priceChange24h,lastUpdatedAt",
    ): QuotesResponse
}