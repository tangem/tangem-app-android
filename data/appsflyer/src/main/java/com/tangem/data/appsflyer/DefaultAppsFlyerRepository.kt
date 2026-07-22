package com.tangem.data.appsflyer

import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import javax.inject.Inject

internal class DefaultAppsFlyerRepository @Inject constructor(
    private val appsFlyerStore: AppsFlyerStore,
) : AppsFlyerRepository {

    override suspend fun getDeeplink(): String? {
        return appsFlyerStore.getNavigationDeeplink()
    }

    override suspend fun clearDeeplink() {
        appsFlyerStore.clearNavigationDeeplink()
    }
}