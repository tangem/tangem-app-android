package com.tangem.tap.data

import com.google.firebase.messaging.FirebaseMessaging
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

internal class FirebasePushNotificationsTokenProvider @Inject constructor() : PushNotificationsTokenProvider {
    override suspend fun getToken(): String {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (ex: Exception) {
            Timber.e(ex)
            ""
        }
    }
}