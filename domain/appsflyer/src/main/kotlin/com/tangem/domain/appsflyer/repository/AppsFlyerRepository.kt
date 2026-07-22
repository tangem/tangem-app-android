package com.tangem.domain.appsflyer.repository

interface AppsFlyerRepository {

    suspend fun getDeeplink(): String?

    suspend fun clearDeeplink()
}