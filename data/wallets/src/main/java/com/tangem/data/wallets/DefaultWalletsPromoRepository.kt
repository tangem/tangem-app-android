package com.tangem.data.wallets

import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.BindWalletsByReferralCodeBody
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
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
    private val appsFlyerStore: AppsFlyerStore,
) : WalletsPromoRepository {

    override suspend fun bindRefcodeWithWallets(conversionData: AppsFlyerConversionData) = withContext(dispatchers.io) {
        val savedBindingData = getSavedBindingData()

        when {
            savedBindingData == null -> {
                // first time binding
                saveBindingData(refcode = conversionData.refcode, campaign = conversionData.campaign)
                bind(refcode = conversionData.refcode, campaign = conversionData.campaign)
            }
            !savedBindingData.isDone -> {
                // try again
                bind(refcode = savedBindingData.refcode, campaign = savedBindingData.campaign)
            }
        }
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
        return appsFlyerStore.get()?.refcode
    }

    private suspend fun getSavedBindingData(): ReferralWalletsBindingData? {
        return appPreferencesStore.getObjectSyncOrNull(key = REFERRAL_WALLETS_BINDING_DATA_KEY)
    }

    private suspend fun saveBindingData(refcode: String, campaign: String?) {
        val bindingData = ReferralWalletsBindingData(refcode = refcode, campaign = campaign, isDone = false)

        appPreferencesStore.storeObject(key = REFERRAL_WALLETS_BINDING_DATA_KEY, value = bindingData)
    }

    private suspend fun bind(refcode: String, campaign: String?) {
        val walletIds = userWalletsListRepository.userWallets.value.orEmpty().map { it.walletId.stringValue }

        val result = tangemTechApi.bindWalletsByReferralCode(
            body = BindWalletsByReferralCodeBody(walletIds = walletIds, refcode = refcode, campaign = campaign),
        )

        if (result is ApiResponse.Success) {
            val successData = ReferralWalletsBindingData(refcode = refcode, campaign = campaign, isDone = true)

            appPreferencesStore.storeObject(key = REFERRAL_WALLETS_BINDING_DATA_KEY, value = successData)
        }
    }

    @JsonClass(generateAdapter = true)
    private data class ReferralWalletsBindingData(
        @Json(name = "refcode") val refcode: String,
        @Json(name = "campaign") val campaign: String?,
        @Json(name = "done") val isDone: Boolean,
    )

    private companion object {

        val REFERRAL_WALLETS_BINDING_DATA_KEY by lazy { stringPreferencesKey(name = "referralWalletsBindingData") }
    }
}