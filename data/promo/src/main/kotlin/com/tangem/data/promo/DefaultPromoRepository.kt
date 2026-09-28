package com.tangem.data.promo

import com.squareup.moshi.Moshi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.promo.converter.PromoCampaignConverter
import com.tangem.data.promo.converter.PromoCampaignV3Converter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.promotion.models.CreatePromotionRegistrationBody
import com.tangem.datasource.api.promotion.models.PromotionRegistrationResponse
import com.tangem.datasource.api.promotion.models.PromotionWalletType
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.*
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

internal class DefaultPromoRepository(
    private val promotionsSupplier: PromotionsSupplier,
    private val tangemApi: TangemTechApi,
    private val moshi: Moshi,
    private val campaignsStore: RuntimeSharedStore<Map<UserWalletId, List<PromoCampaign>>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoRepository {

    override suspend fun getCampaignState(
        campaign: PromoCampaignId,
        userWalletId: UserWalletId,
        forceRefresh: Boolean,
    ): PromoCampaignState = withContext(dispatchers.io) {
        val all = promotionsSupplier.getPromotions(userWalletId, forceRefresh)
            .promotions.firstOrNull { it.name == campaign.slug }?.all
        when {
            all == null -> PromoCampaignState.NotActive(campaign)
            all.status == ACTIVE_STATUS -> PromoCampaignConverter.toAvailable(campaign, all)
            else -> PromoCampaignState.NotActive(campaign)
        }
    }

    override suspend fun enroll(
        campaign: PromoCampaignId,
        tokenReward: TokenReward,
        walletIds: List<UserWalletId>,
    ): EnrollResult = withContext(dispatchers.io) {
        val body = CreatePromotionRegistrationBody(
            campaignId = campaign.slug,
            walletIds = walletIds.map { it.stringValue },
            tokenReward = tokenReward.toDto(),
        )
        when (val response = tangemApi.createPromotionRegistration(body)) {
            is ApiResponse.Success -> {
                val saved = response.data.data.tokenReward.toDomain()
                EnrollResult.Success(saved)
            }
            is ApiResponse.Error -> {
                val cause = response.cause
                val conflict = (cause as? ApiResponseError.HttpException)
                    ?.takeIf { it.code == ApiResponseError.HttpException.Code.CONFLICT }
                if (conflict != null) {
                    val existing = parseConflict(conflict.errorBody)?.data
                        ?.tokenReward
                        ?.toDomain()
                        ?: tokenReward.toEnrolledTokenReward()
                    EnrollResult.AlreadyEnrolled(existing)
                } else {
                    throw cause
                }
            }
        }
    }

    override suspend fun getCampaigns(userWallet: UserWallet, forceRefresh: Boolean): List<PromoCampaign> {
        val userWalletId = userWallet.walletId
        if (!forceRefresh) {
            campaignsStore.getSyncOrNull()?.get(userWalletId)?.let { return it }
        }

        val campaigns = withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    tangemApi.getPromotionsV3(
                        walletId = userWalletId.stringValue,
                        walletType = PromotionWalletType.from(userWallet),
                        batchId = (userWallet as? UserWallet.Cold)?.scanResponse?.card?.batchId,
                        languageISOCode = SupportedLanguages.getCurrentSupportedLanguageCode(),
                    ).getOrThrow().promotions.mapNotNull(PromoCampaignV3Converter::convert)
                },
                onError = { error ->
                    TangemLogger.w("Unable to fetch promotion", error)
                    throw error
                },
            )
        }
        campaignsStore.update(emptyMap()) { it + (userWalletId to campaigns) }
        return campaigns
    }

    private fun parseConflict(body: String?): PromotionRegistrationResponse? {
        if (body.isNullOrBlank()) return null
        return runCatching {
            moshi.adapter(PromotionRegistrationResponse::class.java).fromJson(body)
        }.getOrNull()
    }

    private fun TokenReward.toDto() = CreatePromotionRegistrationBody.TokenRewardDto(
        tokenAddress = tokenAddress,
        networkId = networkId,
        userAddress = userAddress,
        tokenId = tokenId,
    )

    private fun TokenReward.toEnrolledTokenReward() = EnrolledTokenReward(
        tokenAddress = tokenAddress,
        networkId = networkId,
        tokenId = tokenId,
    )

    private fun PromotionRegistrationResponse.RegisteredTokenRewardDto.toDomain() = EnrolledTokenReward(
        tokenAddress = tokenAddress,
        networkId = networkId,
        tokenId = tokenId,
    )

    private companion object {
        const val ACTIVE_STATUS = "active"
    }
}