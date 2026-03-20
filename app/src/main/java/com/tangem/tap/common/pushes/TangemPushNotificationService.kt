package com.tangem.tap.common.pushes

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tangem.tap.common.analytics.CustomerIoFeatureToggles
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.AndroidEntryPoint
import io.customer.messagingpush.CustomerIOFirebaseMessagingService
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
internal class TangemPushNotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var customerIoFeatureToggles: CustomerIoFeatureToggles

    private val pushNotificationDelegate: PushNotificationDelegate by lazy {
        PushNotificationDelegate(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        TangemLogger.d("New FCM token received: $token")

        if (customerIoFeatureToggles.isFeatureEnabled) {
            CustomerIOFirebaseMessagingService.onNewToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (customerIoFeatureToggles.isFeatureEnabled) {
            CustomerIOFirebaseMessagingService.onMessageReceived(
                context = applicationContext,
                remoteMessage = message,
                handleNotificationTrigger = false,
            )
        }

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