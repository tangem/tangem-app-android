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

    @GET("v1/coins")
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

    @GET("v1/rates")
    suspend fun getRates(@Query("currencyId") currencyId: String, @Query("coinIds") coinIds: String): RatesResponse

    @GET("v1/currencies")
    suspend fun getCurrencyList(
        @Header("Cache-Control") cacheControl: String = "max-age=600",
    ): ApiResponse<CurrenciesResponse>

    @GET("v1/geo")
    suspend fun getUserCountryCode(): GeoResponse

    @GET("v1/user-tokens/{user-id}")
    suspend fun getUserTokens(@Path(value = "user-id") userId: String): ApiResponse<UserTokensResponse>

    @PUT("v1/user-tokens/{user-id}")
    suspend fun saveUserTokens(
        @Path(value = "user-id") userId: String,
        @Body userTokens: UserTokensResponse,
    ): ApiResponse<Unit>

    @POST("v1/user-tokens")
    suspend fun markUserWallerWasCreated(@Body body: MarkUserWalletWasCreatedBody): ApiResponse<Unit>

    /** Returns referral status by [walletId] */
    @GET("v1/referral/{walletId}")
    suspend fun getReferralStatus(@Path("walletId") walletId: String): ApiResponse<ReferralResponse>

    /** Make user referral, requires [StartReferralBody] */
    @POST("v1/referral")
    suspend fun startReferral(@Body startReferralBody: StartReferralBody): ApiResponse<ReferralResponse>

    @GET("v1/quotes")
    suspend fun getQuotes(
        @Query("currencyId") currencyId: String,
        @Query("coinIds") coinIds: String,
        @Query("fields") fields: String,
    ): ApiResponse<QuotesResponse>

    @GET("v1/promotion")
    suspend fun getPromotionInfo(
        @Query("programName") name: String,
        @Header("Cache-Control") cacheControl: String = "max-age=600",
    ): ApiResponse<PromotionInfoResponse>

    @GET("v1/settings/{wallet_id}")
    suspend fun getUserTokensSettings(@Path("wallet_id") walletId: String): ApiResponse<UserTokensSettingsResponse>

    @PUT("v1/settings/{wallet_id}")
    suspend fun saveUserTokensSettings(
        @Path("wallet_id") walletId: String,
        @Body userTokensSettings: UserTokensSettingsResponse,
    ): ApiResponse<Unit>

    @POST("v1/user-network-account")
    suspend fun createUserNetworkAccount(
        @Body body: CreateUserNetworkAccountBody,
    ): ApiResponse<CreateUserNetworkAccountResponse>

    @POST("v1/account")
    suspend fun createUserTokensAccount(
        @Body body: CreateUserTokensAccountBody,
    ): ApiResponse<UserTokensAccountResponse>

    @PUT("v1/account/{account_id}")
    suspend fun updateUserTokensAccount(
        @Path("account_id") accountId: Int,
        @Body body: UpdateUserTokensAccountBody,
    ): ApiResponse<UserTokensAccountResponse>

    @PUT("v1/account/{account_id}/archive")
    suspend fun archiveUserTokensAccount(@Path("account_id") accountId: Int): ApiResponse<UserTokensAccountResponse>

    @PUT("v1/account/{account_id}/unarchive")
    suspend fun restoreUserTokensAccount(@Path("account_id") accountId: Int): ApiResponse<UserTokensAccountResponse>

    @GET("v1/features")
    suspend fun getFeatures(): ApiResponse<FeaturesResponse>

    @ReadTimeout(duration = 5, unit = TimeUnit.SECONDS)
    @GET("v1/networks/providers")
    suspend fun getBlockchainProviders(): Map<String, List<ProviderModel>>

    @GET("v1/seedphrase-notification/{wallet_id}")
    suspend fun getSeedPhraseNotificationStatus(
        @Path("wallet_id") walletId: String,
    ): ApiResponse<SeedPhraseNotificationDTO>

    @PUT("v1/seedphrase-notification/{wallet_id}")
    suspend fun updateSeedPhraseNotificationStatus(
        @Path("wallet_id") walletId: String,
        @Body body: SeedPhraseNotificationDTO,
    ): ApiResponse<Unit>

    @GET("v1/seedphrase-notification/{wallet_id}/confirmed")
    suspend fun getSeedPhraseSecondNotificationStatus(
        @Path("wallet_id") walletId: String,
    ): ApiResponse<SeedPhraseNotificationDTO>

    @PUT("v1/seedphrase-notification/{wallet_id}/confirmed")
    suspend fun updateSeedPhraseSecondNotificationStatus(
        @Path("wallet_id") walletId: String,
        @Body body: SeedPhraseNotificationDTO,
    ): ApiResponse<Unit>

    @GET("v1/hot_crypto")
    suspend fun getHotCrypto(@Query("currency") currencyId: String): ApiResponse<HotCryptoResponse>

    @GET("v1/stories/{story_id}")
    suspend fun getStoryById(@Path("story_id") storyId: String): ApiResponse<StoryContentResponse>

    // region push notifications
    @GET("v1/notification/push_notifications_eligible_networks")
    suspend fun getEligibleNetworksForPushNotifications(): ApiResponse<List<CryptoNetworkResponse>>

    @POST("v1/user-wallets/applications/")
    suspend fun createApplicationId(
        @Body
        body: NotificationApplicationCreateBody,
    ): ApiResponse<NotificationApplicationIdResponse>

    @PATCH("v1/user-wallets/applications/{application_id}")
    suspend fun updatePushTokenForApplicationId(
        @Path("application_id") applicationId: String,
        @Body body: NotificationApplicationCreateBody,
    ): ApiResponse<Unit>

    @PATCH("v1/user-wallets/wallets/{wallet_id}/notify")
    suspend fun setNotificationsEnabled(@Path("wallet_id") walletId: String, @Body body: WalletBody): ApiResponse<Unit>
    // endregion

    // region wallets
    @PATCH("v1/user-wallets/wallets/{wallet_id}")
    suspend fun updateWallet(@Path("wallet_id") walletId: String, @Body body: WalletBody): ApiResponse<Unit>

    @POST("v1/user-wallets/wallets/create-and-connect-by-appuid/{application_id}")
    suspend fun associateApplicationIdWithWallets(
        @Path("application_id") applicationId: String,
        @Body body: List<WalletIdBody>,
    ): ApiResponse<Unit>

    @GET("v1/user-wallets/wallets/{wallet_id}")
    suspend fun getWalletById(@Path("wallet_id") walletId: String): ApiResponse<WalletResponse>

    @GET("v1/user-wallets/wallets/by-app/{app_id}")
    suspend fun getWallets(@Path("app_id") appId: String): ApiResponse<List<WalletResponse>>
    // endregion

    // promo
    @POST("promo/v1/promo-codes/activate")
    suspend fun activatePromoCode(@Body body: PromocodeActivationBody): ApiResponse<PromocodeActivationResponse>
    // endregion
}