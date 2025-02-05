package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.promotion.models.PromotionInfoResponse
import com.tangem.datasource.api.promotion.models.StoryContentResponse
import com.tangem.datasource.api.tangemTech.models.*
import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.local.config.providers.models.ProviderModel
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Interface of Tangem Tech API
 *
[REDACTED_AUTHOR]
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

    @POST("user-tokens")
    suspend fun markUserWallerWasCreated(@Body body: MarkUserWalletWasCreatedBody): ApiResponse<Unit>

    /** Returns referral status by [walletId] */
    @GET("referral/{walletId}")
    suspend fun getReferralStatus(@Path("walletId") walletId: String): ReferralResponse

    /** Make user referral, requires [StartReferralBody] */
    @POST("referral")
    suspend fun startReferral(@Body startReferralBody: StartReferralBody): ReferralResponse

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
    suspend fun getUserTokensSettings(@Path("wallet_id") walletId: String): ApiResponse<UserTokensSettingsResponse>

    @PUT("settings/{wallet_id}")
    suspend fun saveUserTokensSettings(
        @Path("wallet_id") walletId: String,
        @Body userTokensSettings: UserTokensSettingsResponse,
    ): ApiResponse<Unit>

    @POST("user-network-account")
    suspend fun createUserNetworkAccount(
        @Body body: CreateUserNetworkAccountBody,
    ): ApiResponse<CreateUserNetworkAccountResponse>

    @POST("account")
    suspend fun createUserTokensAccount(
        @Body body: CreateUserTokensAccountBody,
    ): ApiResponse<UserTokensAccountResponse>

    @PUT("account/{account_id}")
    suspend fun updateUserTokensAccount(
        @Path("account_id") accountId: Int,
        @Body body: UpdateUserTokensAccountBody,
    ): ApiResponse<UserTokensAccountResponse>

    @PUT("account/{account_id}/archive")
    suspend fun archiveUserTokensAccount(@Path("account_id") accountId: Int): ApiResponse<UserTokensAccountResponse>

    @PUT("account/{account_id}/unarchive")
    suspend fun restoreUserTokensAccount(@Path("account_id") accountId: Int): ApiResponse<UserTokensAccountResponse>

    @GET("features")
    suspend fun getFeatures(): ApiResponse<FeaturesResponse>

    @ReadTimeout(duration = 5, unit = TimeUnit.SECONDS)
    @GET("networks/providers")
    suspend fun getBlockchainProviders(): Map<String, List<ProviderModel>>

    @GET("seedphrase-notification/{wallet_id}")
    suspend fun getSeedPhraseNotificationStatus(
        @Path("wallet_id") walletId: String,
    ): ApiResponse<SeedPhraseNotificationDTO>

    @PUT("seedphrase-notification/{wallet_id}")
    suspend fun updateSeedPhraseNotificationStatus(
        @Path("wallet_id") walletId: String,
        @Body body: SeedPhraseNotificationDTO,
    ): ApiResponse<Unit>

    @GET("hot_crypto")
    suspend fun getHotCrypto(@Query("currency") currencyId: String): ApiResponse<HotCryptoResponse>

    @GET("stories/{story_id}")
    suspend fun getStoryById(
        @Path("story_id") storyId: String,
        @Query("language") language: String,
    ): ApiResponse<StoryContentResponse>

    companion object {
        val marketsQuoteFields = listOf(
            "price",
            "priceChange24h",
            "priceChange1w",
            "priceChange30d",
        )
    }
}