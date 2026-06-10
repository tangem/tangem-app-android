package com.tangem.datasource.local.appsflyer

import com.tangem.domain.wallets.models.AppsFlyerConversionData

interface AppsFlyerStore {

    suspend fun get(): AppsFlyerConversionData?

    suspend fun getUID(): String?

    suspend fun store(value: AppsFlyerConversionData)

    suspend fun storeIfAbsent(value: AppsFlyerConversionData)

    suspend fun storeUIDIfAbsent(value: String)

    suspend fun getDeeplink(source: AppsFlyerDeeplinkSource): String?

    suspend fun storeDeeplink(source: AppsFlyerDeeplinkSource, deeplink: String)

    suspend fun clearDeeplink(source: AppsFlyerDeeplinkSource)
}

enum class AppsFlyerDeeplinkSource {
    TangemPayHotWalletOnboarding,
    ;

    fun toStoreKey() = when (this) {
        TangemPayHotWalletOnboarding -> "tangem_pay_hot_wallet_onboarding"
    }
}