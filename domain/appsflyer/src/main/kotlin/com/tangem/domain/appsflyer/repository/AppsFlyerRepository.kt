package com.tangem.domain.appsflyer.repository

import com.tangem.domain.wallets.models.AppsFlyerConversionData
import kotlinx.coroutines.flow.Flow

interface AppsFlyerRepository {

    suspend fun getConversionData(): AppsFlyerConversionData?

    suspend fun getUID(): String?

    suspend fun storeIfAbsent(value: AppsFlyerConversionData)

    suspend fun storeUIDIfAbsent(value: String)

    fun observeNavigationDeeplink(): Flow<String?>

    suspend fun getNavigationDeeplink(): String?

    suspend fun storeNavigationDeeplink(deepLinkValue: String)

    suspend fun clearNavigationDeeplink()
}