package com.tangem.data.appsflyer

import com.tangem.data.appsflyer.store.AppsFlyerStore
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultAppsFlyerRepository @Inject constructor(
    private val appsFlyerStore: AppsFlyerStore,
) : AppsFlyerRepository {

    override suspend fun getConversionData(): AppsFlyerConversionData? = appsFlyerStore.get()

    override suspend fun getUID(): String? = appsFlyerStore.getUID()

    override suspend fun storeIfAbsent(value: AppsFlyerConversionData) {
        appsFlyerStore.storeIfAbsent(value)
    }

    override suspend fun storeUIDIfAbsent(value: String) {
        appsFlyerStore.storeUIDIfAbsent(value)
    }

    override fun observeNavigationDeeplink(): Flow<String?> = appsFlyerStore.observeNavigationDeeplink()

    override suspend fun getNavigationDeeplink(): String? = appsFlyerStore.getNavigationDeeplink()

    override suspend fun storeNavigationDeeplink(deepLinkValue: String) {
        appsFlyerStore.storeNavigationDeeplink(deepLinkValue)
    }

    override suspend fun clearNavigationDeeplink() {
        appsFlyerStore.clearNavigationDeeplink()
    }
}