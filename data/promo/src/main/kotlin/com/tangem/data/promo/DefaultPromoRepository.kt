package com.tangem.data.promo

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.squareup.moshi.Moshi
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.promo.converter.PromoCampaignV3Converter
import com.tangem.data.promo.converter.PromoRegistrationConverter
import com.tangem.data.promo.converter.PromoRegistrationErrorConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.promotion.models.*
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.*
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class DefaultPromoRepository(
    private val tangemApi: TangemTechApi,
    private val moshi: Moshi,
    private val campaignsStore: RuntimeSharedStore<Map<UserWalletId, List<PromoCampaign>>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : PromoRepository {

    /** Every promo banner block asks for its wallet's campaigns on start; one request per wallet serves them all. */
    private val campaignsMutexes = ConcurrentHashMap<UserWalletId, Mutex>()

    override suspend fun getCampaignsSync(userWallet: UserWallet, forceRefresh: Boolean): List<PromoCampaign> {
        val userWalletId = userWallet.walletId

        return campaignsMutexes.getOrPut(userWalletId, ::Mutex).withLock {
            val cached = if (forceRefresh) null else campaignsStore.getSyncOrNull()?.get(userWalletId)
            cached ?: fetchCampaigns(userWallet).also { campaigns ->
                campaignsStore.update(emptyMap()) { it + (userWalletId to campaigns) }
            }
        }
    }

    override fun getCampaigns(userWalletId: UserWalletId): Flow<List<PromoCampaign>> {
        return campaignsStore.get().mapNotNull { it[userWalletId] }.distinctUntilChanged()
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

    private fun parseError(body: String?): PromotionErrorResponse? {
        if (body.isNullOrBlank()) return null
        return runCatching { moshi.adapter(PromotionErrorResponse::class.java).fromJson(body) }.getOrNull()
    }

    private fun invalidEnrollments(message: String) = PromoRegistrationError.Generic(IllegalArgumentException(message))
}