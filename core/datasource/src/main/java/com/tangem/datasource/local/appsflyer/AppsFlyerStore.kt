package com.tangem.datasource.local.appsflyer

import com.tangem.domain.wallets.models.AppsFlyerConversionData
import kotlinx.coroutines.flow.Flow

interface AppsFlyerStore {

    suspend fun get(): AppsFlyerConversionData?

    suspend fun getUID(): String?

    suspend fun store(value: AppsFlyerConversionData)

    suspend fun storeIfAbsent(value: AppsFlyerConversionData)

    suspend fun storeUIDIfAbsent(value: String)

    fun observeNavigationDeeplink(): Flow<String?>

    suspend fun getNavigationDeeplink(): String?

    suspend fun storeNavigationDeeplink(deepLinkValue: String)

    suspend fun clearNavigationDeeplink()
}