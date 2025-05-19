package com.tangem.core.analytics

class DummyAppInstanceIdProvider : AppInstanceIdProvider {

    override suspend fun getAppInstanceId(): String? {
        return null
    }

    override fun getAppInstanceIdSync(): String? {
        return null
    }
}