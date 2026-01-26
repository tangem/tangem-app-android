package com.tangem.datasource.local.appsflyer

import com.tangem.domain.wallets.models.AppsFlyerConversionData

interface AppsFlyerConversionStore {

    suspend fun get(): AppsFlyerConversionData?

    suspend fun store(value: AppsFlyerConversionData)

    suspend fun storeIfAbsent(value: AppsFlyerConversionData)
}