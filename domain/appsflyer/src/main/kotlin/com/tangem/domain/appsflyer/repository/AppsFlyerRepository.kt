package com.tangem.domain.appsflyer.repository

import com.tangem.domain.appsflyer.AppsFlyerDeeplinkSource

interface AppsFlyerRepository {

    suspend fun clearDeeplink(source: AppsFlyerDeeplinkSource)
}