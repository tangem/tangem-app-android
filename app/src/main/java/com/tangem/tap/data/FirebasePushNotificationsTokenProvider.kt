package com.tangem.tap.data

import com.google.firebase.messaging.FirebaseMessaging
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebasePushNotificationsTokenProvider @Inject constructor() : PushNotificationsTokenProvider {
    override suspend fun getToken(): String {
        return FirebaseMessaging.getInstance().token.await()
    }
}