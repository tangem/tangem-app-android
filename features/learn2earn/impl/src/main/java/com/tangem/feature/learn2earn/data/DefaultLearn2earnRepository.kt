package com.tangem.feature.learn2earn.data

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.promotion.PromotionApi
import com.tangem.datasource.api.promotion.models.*
import com.tangem.feature.learn2earn.data.api.Learn2earnPreferenceStorage
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.feature.learn2earn.data.api.UsedCardsPreferenceStorage
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
internal class DefaultLearn2earnRepository(
    private val preferencesStorage: Learn2earnPreferenceStorage,
    private val usedCardsPreferenceStorage: UsedCardsPreferenceStorage,
    private val moshi: Moshi,
    private val api: PromotionApi,
    private val dispatchers: AppCoroutineDispatcherProvider,
) : Learn2earnRepository {

    private val promotionInfoAdapter = moshi.adapter(PromotionInfoResponse::class.java)

    override fun isHadActivatedCards(): Boolean {
        return usedCardsPreferenceStorage.isHadActivatedCards()
    }

    override fun getPromoCode(): String? {
        return preferencesStorage.promoCode
    }

    override fun savePromoCode(code: String) {
        preferencesStorage.promoCode = code
    }

    override fun getProgramName(): String {
        return PROGRAM_NAME
    }

    override fun isAlreadyReceivedAward(): Boolean {
        return preferencesStorage.alreadyReceivedAward
    }

    override suspend fun getPromotionInfo(): Result<PromotionInfoResponse> {
        return withContext(dispatchers.io) {
            val info = restorePromotionInfo()

            if (info?.status == PromotionInfoResponse.Status.FINISHED) {
                Result.success(info)
            } else {
                runCatching { api.getPromotionInfo(getProgramName()) }
                    .fold(
                        onSuccess = { infoResponse ->
                            savePromotionInfo(infoResponse)
                            Result.success(infoResponse)
                        },
                        onFailure = { exception -> Result.failure(exception) },
                    )
            }
        }
    }

    override suspend fun validate(walletId: String): ValidateResponse {
        return withContext(dispatchers.io) {
            api.validate(ValidateRequestBody(walletId, getProgramName()))
        }
    }

    override suspend fun requestAward(walletId: String): AwardResponse {
        return withContext(dispatchers.io) {
            api.requestAward(AwardRequestBody(walletId, getProgramName()))
        }
    }

    override suspend fun validateCode(walletId: String, code: String?): CodeValidateResponse {
        return withContext(dispatchers.io) {
            api.validateCode(CodeValidateRequestBody(walletId, code))
        }
    }

    override suspend fun requestAwardByCode(walletId: String, address: String, code: String?): CodeAwardResponse {
        return withContext(dispatchers.io) {
            api.requestAwardByCode(CodeAwardRequestBody(walletId, address, code))
        }
    }

    private fun restorePromotionInfo(): PromotionInfoResponse? {
        return try {
            preferencesStorage.promotionInfo?.let { promotionInfoAdapter.fromJson(it) }
        } catch (ex: Exception) {
            Timber.e(ex)
            preferencesStorage.promotionInfo = null
            null
        }
    }

    private fun savePromotionInfo(infoResponse: PromotionInfoResponse) {
        try {
            preferencesStorage.promotionInfo = promotionInfoAdapter.toJson(infoResponse)
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private companion object {
        const val PROGRAM_NAME: String = "1inch"
    }
}