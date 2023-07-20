package com.tangem.feature.learn2earn.data

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.promotion.PromotionApi
import com.tangem.datasource.api.promotion.models.*
import com.tangem.feature.learn2earn.data.api.Learn2earnPreferenceStorage
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.feature.learn2earn.data.models.PromoUserData
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
internal class DefaultLearn2earnRepository(
    private val preferencesStorage: Learn2earnPreferenceStorage,
    private val api: PromotionApi,
    private val dispatchers: AppCoroutineDispatcherProvider,
    moshi: Moshi,
) : Learn2earnRepository {

    private val promotionInfoAdapter = moshi.adapter(PromotionInfoResponse::class.java)
    private val userDataAdapter = moshi.adapter(PromoUserData::class.java)

    private var userData: PromoUserData = restoreUserData()

    override fun getUserData(): PromoUserData {
        return userData
    }

    override fun updateUserData(userData: PromoUserData) {
        this.userData = userData
        saveUserData(userData)
    }

    override fun getProgramName(): String {
        return PROGRAM_NAME
    }

    override suspend fun getPromotionInfo(): Result<PromotionInfoResponse> {
        return withContext(dispatchers.io) {
            val promotionInfo = restorePromotionInfo()
            val data = promotionInfo?.getData(userData.promoCode)
            if (data?.status == PromotionInfoResponse.Status.FINISHED) {
                Result.success(promotionInfo)
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

    override suspend fun requestAward(walletId: String, address: String): AwardResponse {
        return withContext(dispatchers.io) {
            api.requestAward(AwardRequestBody(walletId, address, getProgramName()))
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

    private fun restoreUserData(): PromoUserData {
        return try {
            preferencesStorage.userData?.let { userDataAdapter.fromJson(it) }
                ?: createEmptyUserData().apply { saveUserData(this) }
        } catch (ex: Exception) {
            Timber.e(ex)
            createEmptyUserData().apply { saveUserData(this) }
        }
    }

    private fun saveUserData(userData: PromoUserData) {
        try {
            preferencesStorage.userData = userDataAdapter.toJson(userData)
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun createEmptyUserData(): PromoUserData = PromoUserData(
        promoCode = null,
        isRegisteredInPromotion = false,
        isAlreadyReceivedAward = false,
        isLearningStageFinished = false,
    )

    private companion object {
        const val PROGRAM_NAME: String = "1inch"
    }
}

private fun PromotionInfoResponse.getData(promoCode: String?): PromotionInfoResponse.Data? {
    return if (promoCode == null) {
        newCard
    } else {
        oldCard
    }
}
