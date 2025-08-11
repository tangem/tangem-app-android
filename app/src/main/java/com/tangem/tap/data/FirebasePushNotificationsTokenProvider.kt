package com.tangem.tap.data

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirebasePushNotificationsTokenProvider @Inject constructor() {
    suspend fun getToken(): String {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (ex: Exception) {
            Timber.e(ex)
            ""
        }
    }
}