package com.tangem.core.analytics.api

interface UserIdHolder {

    fun setUserId(userId: String)

    fun clearUserId()
}