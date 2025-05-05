package com.tangem.utils.notifications

interface PushNotificationsTokenProvider {

    suspend fun getToken(): String
}