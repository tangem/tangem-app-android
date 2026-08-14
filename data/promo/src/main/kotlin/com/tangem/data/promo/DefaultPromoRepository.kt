package com.tangem.data.promo

import com.squareup.moshi.Moshi
import com.tangem.data.promo.converter.PromoCampaignConverter
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.promotion.models.CreatePromotionRegistrationBody
import com.tangem.datasource.api.promotion.models.PromotionRegistrationResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.EnrolledTokenReward
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.models.TokenReward
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultPromoRepository(
    private val promotionsSupplier: PromotionsSupplier,
    private val tangemApi: TangemTechApi,
    private val moshi: Moshi,
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