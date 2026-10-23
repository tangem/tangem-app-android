package com.tangem.data.wallets

import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.BindWalletsByReferralCodeBody
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.repository.WalletsPromoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

internal class DefaultWalletsPromoRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val appsFlyerRepository: AppsFlyerRepository,
) : WalletsPromoRepository {

    override suspend fun bindRefcodeWithWallets(conversionData: AppsFlyerConversionData) = withContext(dispatchers.io) {
        val savedBindingData = getSavedBindingData()

        when {
            savedBindingData == null -> saveAndBind(conversionData)
            !savedBindingData.isDone -> {
                val isRejected = bind(refcode = savedBindingData.refcode, campaign = savedBindingData.campaign)

                if (isRejected && savedBindingData.refcode != conversionData.refcode) {
                    saveAndBind(conversionData)
                }
            }
        }
    }

    private suspend fun saveAndBind(conversionData: AppsFlyerConversionData) {
        saveBindingData(refcode = conversionData.refcode, campaign = conversionData.campaign)
        bind(refcode = conversionData.refcode, campaign = conversionData.campaign)
    }

    override suspend fun retryBindRefcodeWithWallets() {
        val savedBindingData = getSavedBindingData()

        if (savedBindingData != null) {
            bind(refcode = savedBindingData.refcode, campaign = savedBindingData.campaign)
        } else {
            TangemLogger.i("retryBindRefcodeWithWallets: Binding data isn't required")
        }
    }

    override suspend fun getReferralCodeIfExists(): String? {
        return appsFlyerRepository.getConversionData()?.refcode
    }

    private suspend fun getSavedBindingData(): ReferralWalletsBindingData? {
        return appPreferencesStore.getObjectSyncOrNull(key = REFERRAL_WALLETS_BINDING_DATA_KEY)
    }

    private suspend fun saveBindingData(refcode: String, campaign: String?) {
        val bindingData = ReferralWalletsBindingData(refcode = refcode, campaign = campaign, isDone = false)

        appPreferencesStore.storeObject(key = REFERRAL_WALLETS_BINDING_DATA_KEY, value = bindingData)
    }

    /** @return true if the backend rejected the code and its binding data was cleared */
    private suspend fun bind(refcode: String, campaign: String?): Boolean {
        val walletIds = userWalletsListRepository.userWallets.value.orEmpty().map { it.walletId.stringValue }

        val result = tangemTechApi.bindWalletsByReferralCode(
            body = BindWalletsByReferralCodeBody(walletIds = walletIds, refcode = refcode, campaign = campaign),
        )

        return when (result) {
            is ApiResponse.Success -> {
                val successData = ReferralWalletsBindingData(refcode = refcode, campaign = campaign, isDone = true)

                appPreferencesStore.storeObject(key = REFERRAL_WALLETS_BINDING_DATA_KEY, value = successData)
                false
            }
            is ApiResponse.Error -> if (result.cause.isRejection()) {
                TangemLogger.e("Referral code is rejected, binding data is cleared: ${result.cause}")
                appPreferencesStore.editData { it.remove(REFERRAL_WALLETS_BINDING_DATA_KEY) }
                true
            } else {
                TangemLogger.e("Failed to bind referral code, will retry: ${result.cause}")
                false
            }
        }
    }

    /**
     * A rejected code would otherwise be retried on every launch and block a later valid code from being bound.
     * Auth, timeout and rate-limit 4xx are transient, so they keep the binding data for a retry.
     */
    private fun ApiResponseError.isRejection(): Boolean {
        return this is ApiResponseError.HttpException &&
            code.numericCode in CLIENT_ERROR_CODES &&
            code !in TRANSIENT_CLIENT_ERROR_CODES
    }

    @JsonClass(generateAdapter = true)
    internal data class ReferralWalletsBindingData(
        @Json(name = "refcode") val refcode: String,
        @Json(name = "campaign") val campaign: String?,
        @Json(name = "done") val isDone: Boolean,
    )

    private companion object {

        val REFERRAL_WALLETS_BINDING_DATA_KEY by lazy { stringPreferencesKey(name = "referralWalletsBindingData") }

        val CLIENT_ERROR_CODES = 400..499

        val TRANSIENT_CLIENT_ERROR_CODES = setOf(
            ApiResponseError.HttpException.Code.UNAUTHORIZED,
            ApiResponseError.HttpException.Code.FORBIDDEN,
            ApiResponseError.HttpException.Code.PROXY_AUTHENTICATION_REQUIRED,
            ApiResponseError.HttpException.Code.REQUEST_TIMEOUT,
            ApiResponseError.HttpException.Code.TOO_EARLY,
            ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
        )
    }
}