package com.tangem.datasource.local.appsflyer

import com.tangem.domain.wallets.models.AppsFlyerConversionData

interface AppsFlyerStore {

    suspend fun get(): AppsFlyerConversionData?

    suspend fun getUID(): String?

    suspend fun store(value: AppsFlyerConversionData)

    suspend fun storeIfAbsent(value: AppsFlyerConversionData)

    suspend fun storeUIDIfAbsent(value: String)
}