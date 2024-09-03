package com.tangem.core.analytics

interface AppInstanceIdProvider {

    suspend fun getAppInstanceId(): String?

    fun getAppInstanceIdSync(): String?
}