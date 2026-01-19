package com.tangem.data.wallets

import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Option
import arrow.core.toOption
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.BindWalletsByReferralCodeBody
import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.repository.WalletsPromoRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultWalletsPromoRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsStore: UserWalletsStore,
    private val appsFlyerConversionStore: AppsFlyerConversionStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletsPromoRepository {

    override suspend fun getConversionData(): Option<AppsFlyerConversionData> {
        return runSuspendCatching { appsFlyerConversionStore.get() }.getOrNull().toOption()
    }

    override suspend fun saveConversionData(refcode: String, campaign: String?) {
        val data = AppsFlyerConversionData(refcode = refcode, campaign = campaign)

        appsFlyerConversionStore.store(data)
    }

    override suspend fun bindRefcodeWithWallets(refcode: String, campaign: String?) = withContext(dispatchers.io) {
        val walletIds = userWalletsStore.userWalletsSync.map { it.walletId.stringValue }

        val result = tangemTechApi.bindWalletsByReferralCode(
            body = BindWalletsByReferralCodeBody(
                walletIds = walletIds,
                refcode = refcode,
                campaign = campaign,
            ),
        )

        val bindingData = ReferralWalletsBindingData(
            refcode = refcode,
            campaign = campaign,
            isDone = result is ApiResponse.Success,
        )

        appPreferencesStore.storeObject(key = REFERRAL_WALLETS_BINDING_DATA_KEY, value = bindingData)

        result.getOrThrow()
    }

    override suspend fun bindSavedRefcodeWithWallets(): AppsFlyerConversionData {
        val bindingData = appPreferencesStore.getObjectSyncOrNull<ReferralWalletsBindingData>(
            key = REFERRAL_WALLETS_BINDING_DATA_KEY,
        )

        if (bindingData == null) {
            val message = "No saved referral wallets binding data found"
            Timber.e(message)
            error(message)
        }

        val conversionData = AppsFlyerConversionData(
            refcode = bindingData.refcode,
            campaign = bindingData.campaign,
        )

        if (bindingData.isDone) {
            Timber.i("Referral code ${bindingData.refcode} already bound with wallets")
            return conversionData
        }

        bindRefcodeWithWallets(
            refcode = bindingData.refcode,
            campaign = bindingData.campaign,
        )

        return conversionData
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