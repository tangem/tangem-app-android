package com.tangem.tap.data

import com.google.firebase.messaging.FirebaseMessaging
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import kotlinx.coroutines.tasks.await

internal class FirebasePushNotificationsTokenProvider : PushNotificationsTokenProvider {
    override suspend fun getToken(): String {
        return FirebaseMessaging.getInstance().token.await()
    }
}