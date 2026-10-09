package com.tangem.data.promo

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.squareup.moshi.Moshi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.promo.converter.PromoCampaignConverter
import com.tangem.data.promo.converter.PromoCampaignV3Converter
import com.tangem.data.promo.converter.PromoRegistrationConverter
import com.tangem.data.promo.converter.PromoRegistrationErrorConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.promotion.models.*
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.*
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class DefaultPromoRepository(
    private val promotionsSupplier: PromotionsSupplier,
    private val tangemApi: TangemTechApi,
    private val moshi: Moshi,
    private val campaignsStore: RuntimeSharedStore<Map<UserWalletId, List<PromoCampaign>>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoRepository {

    /** Every promo banner block asks for its wallet's campaigns on start; one request per wallet serves them all. */
    private val campaignsMutexes = ConcurrentHashMap<UserWalletId, Mutex>()

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

        return campaignsMutexes.getOrPut(userWalletId, ::Mutex).withLock {
            val cached = if (forceRefresh) null else campaignsStore.getSyncOrNull()?.get(userWalletId)
            cached ?: fetchCampaigns(userWallet).also { campaigns ->
                campaignsStore.update(emptyMap()) { it + (userWalletId to campaigns) }
            }
        }
    }

    override suspend fun register(
        promotionId: Int,
        enrollments: List<PromoEnrollment>,
    ): Either<PromoRegistrationError, List<PromoRegistrationResult>> = withContext(dispatchers.io) {
        either {
            ensure(enrollments.isNotEmpty()) {
                invalidEnrollments("No wallets to register for promotion $promotionId")
            }
            ensure(enrollments.distinctBy { it.userWalletId }.size == enrollments.size) {
                invalidEnrollments("Duplicate wallets in registration for promotion $promotionId")
            }

            val body = PromotionRegistrationsBody(enrollments = enrollments.map(PromoRegistrationConverter::toDto))
            when (val response = tangemApi.createPromotionRegistrations(promotionId = promotionId, body = body)) {
                is ApiResponse.Success -> response.data.results.mapNotNull { result ->
                    PromoRegistrationConverter.toDomain(result).also { resultModel ->
                        if (resultModel == null) {
                            TangemLogger.e("Unreadable registration result dropped for promotion $promotionId")
                        }
                    }
                }
                is ApiResponse.Error -> {
                    val cause = response.cause
                    val error = parseError((cause as? ApiResponseError.HttpException)?.errorBody)
                    TangemLogger.e("Registration for promotion $promotionId failed: ${error?.detail}", cause)
                    raise(PromoRegistrationErrorConverter.convert(cause))
                }
            }
        }
    }

    private suspend fun fetchCampaigns(userWallet: UserWallet): List<PromoCampaign> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                tangemApi.getPromotionsV3(
                    walletId = userWallet.walletId.stringValue,
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

    private fun parseConflict(body: String?): PromotionRegistrationResponse? {
        if (body.isNullOrBlank()) return null
        return runCatching {
            moshi.adapter(PromotionRegistrationResponse::class.java).fromJson(body)
        }.getOrNull()
    }

    private fun parseError(body: String?): PromotionErrorResponse? {
        if (body.isNullOrBlank()) return null
        return runCatching { moshi.adapter(PromotionErrorResponse::class.java).fromJson(body) }.getOrNull()
    }

    private fun invalidEnrollments(message: String) = PromoRegistrationError.Generic(IllegalArgumentException(message))

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