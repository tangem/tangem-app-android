package com.tangem.data.appsflyer

import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.AppsFlyerDeeplinkSource
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import javax.inject.Inject
import com.tangem.datasource.local.appsflyer.AppsFlyerDeeplinkSource as StoreDeeplinkSource

internal class DefaultAppsFlyerRepository @Inject constructor(
    private val appsFlyerStore: AppsFlyerStore,
) : AppsFlyerRepository {

    override suspend fun getDeeplink(source: AppsFlyerDeeplinkSource): String? {
        return appsFlyerStore.getDeeplink(source.toStoreSource())
    }

    override suspend fun clearDeeplink(source: AppsFlyerDeeplinkSource) {
        appsFlyerStore.clearDeeplink(source.toStoreSource())
    }

    private fun AppsFlyerDeeplinkSource.toStoreSource() = when (this) {
        AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding -> StoreDeeplinkSource.TangemPayHotWalletOnboarding
        AppsFlyerDeeplinkSource.Referral -> StoreDeeplinkSource.Referral
    }
}