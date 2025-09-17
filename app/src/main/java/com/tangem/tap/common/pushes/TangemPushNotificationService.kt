package com.tangem.tap.common.pushes

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
internal class TangemPushNotificationService : FirebaseMessagingService() {

    private val pushNotificationDelegate: PushNotificationDelegate by lazy {
        PushNotificationDelegate(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token received: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification ?: return
        val channelId = notification.channelId ?: TANGEM_CHANNEL_ID

        pushNotificationDelegate.showNotification(
            dataMap = message.data,
            title = notification.title,
            body = notification.body,
            channelId = channelId,
            priority = message.priority,
            imageUrl = notification.imageUrl,
            vibratePattern = notification.vibrateTimings,
        )
    }

    private companion object {
        const val TANGEM_CHANNEL_ID = "Tangem General" // General channel for notifications
    }
}