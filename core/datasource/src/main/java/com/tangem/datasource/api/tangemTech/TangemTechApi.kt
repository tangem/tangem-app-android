package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.promotion.models.PromotionInfoResponse
import com.tangem.datasource.api.tangemTech.models.*
import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.local.config.environment.models.ProviderModel
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Interface of Tangem Tech API
 *
* [REDACTED_AUTHOR]
 */
@Suppress("TooManyFunctions")
interface TangemTechApi {

    @GET("coins")
    suspend fun getCoins(
        @Header("Cache-Control") cacheControl: String = "max-age=600",
        @Query("contractAddress") contractAddress: String? = null,
        @Query("exchangeable") exchangeable: Boolean? = null,
        @Query("networkIds") networkIds: String? = null,
        @Query("networkId") networkId: String? = null,
        @Query("active") active: Boolean? = null,
        @Query("searchText") searchText: String? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<CoinsResponse>

    @GET("rates")
    suspend fun getRates(@Query("currencyId") currencyId: String, @Query("coinIds") coinIds: String): RatesResponse

    @GET("currencies")
    suspend fun getCurrencyList(
        @Header("Cache-Control") cacheControl: String = "max-age=600",
    ): ApiResponse<CurrenciesResponse>

    @GET("geo")
    suspend fun getUserCountryCode(): GeoResponse

    @GET("user-tokens/{user-id}")
    suspend fun getUserTokens(@Path(value = "user-id") userId: String): ApiResponse<UserTokensResponse>

    @PUT("user-tokens/{user-id}")
    suspend fun saveUserTokens(
        @Path(value = "user-id") userId: String,
        @Body userTokens: UserTokensResponse,
    ): ApiResponse<Unit>

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

    @GET("quotes")
    suspend fun getQuotes(
        @Query("currencyId") currencyId: String,
        @Query("coinIds") coinIds: String,
        @Query("fields") fields: String = "price,priceChange24h,lastUpdatedAt",
    ): ApiResponse<QuotesResponse>

    @GET("promotion")
    suspend fun getPromotionInfo(
        @Query("programName") name: String,
        @Header("Cache-Control") cacheControl: String = "max-age=600",
    ): ApiResponse<PromotionInfoResponse>

    @GET("settings/{wallet_id}")
    suspend fun getUserTokensSettings(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("wallet_id") walletId: String,
    ): ApiResponse<UserTokensSettingsResponse>

    @PUT("settings/{wallet_id}")
    suspend fun saveUserTokensSettings(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("wallet_id") walletId: String,
        @Body userTokensSettings: UserTokensSettingsResponse,
    ): ApiResponse<Unit>

    @POST("user-network-account")
    suspend fun createUserNetworkAccount(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Body body: CreateUserNetworkAccountBody,
    ): ApiResponse<CreateUserNetworkAccountResponse>

    @POST("account")
    suspend fun createUserTokensAccount(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Body body: CreateUserTokensAccountBody,
    ): ApiResponse<UserTokensAccountResponse>

    @PUT("account/{account_id}")
    suspend fun updateUserTokensAccount(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("account_id") accountId: Int,
        @Body body: UpdateUserTokensAccountBody,
    ): ApiResponse<UserTokensAccountResponse>

    @PUT("account/{account_id}/archive")
    suspend fun archiveUserTokensAccount(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("account_id") accountId: Int,
    ): ApiResponse<UserTokensAccountResponse>

    @PUT("account/{account_id}/unarchive")
    suspend fun restoreUserTokensAccount(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("account_id") accountId: Int,
    ): ApiResponse<UserTokensAccountResponse>

    @GET("features")
    suspend fun getFeatures(): ApiResponse<FeaturesResponse>

    @ReadTimeout(duration = 5, unit = TimeUnit.SECONDS)
    @GET("networks/providers")
    suspend fun getBlockchainProviders(): Map<String, List<ProviderModel>>

    companion object {
        val marketsQuoteFields = listOf(
            "price",
            "priceChange24h",
            "priceChange1w",
            "priceChange30d",
        )
    }
}
