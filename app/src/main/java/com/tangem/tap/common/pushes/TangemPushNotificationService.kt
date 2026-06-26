package com.tangem.tap.common.pushes

import android.annotation.SuppressLint
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tangem.features.pushnotifications.PushNotificationsFeatureToggles
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.AndroidEntryPoint
import io.customer.messagingpush.CustomerIOFirebaseMessagingService
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
internal class TangemPushNotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var pushMessageHandler: PushMessageHandler

    @Inject
    lateinit var pushNotificationsFeatureToggles: PushNotificationsFeatureToggles

    private val pushNotificationDelegate: PushNotificationDelegate by lazy {
        PushNotificationDelegate(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        TangemLogger.d("New FCM token received: $token")

        CustomerIOFirebaseMessagingService.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        CustomerIOFirebaseMessagingService.onMessageReceived(
            context = applicationContext,
            remoteMessage = message,
            handleNotificationTrigger = false,
        )

        pushMessageHandler.onMessageReceived(message.data)

        val notification = message.notification
        when {
            notification != null -> pushNotificationDelegate.showNotification(
                dataMap = message.data,
                title = notification.title,
                body = notification.body,
                channelId = notification.channelId ?: TANGEM_CHANNEL_ID,
                priority = message.priority,
                imageUrl = notification.imageUrl,
                vibratePattern = notification.vibrateTimings,
            )
            pushNotificationsFeatureToggles.isDataPushAsNotificationEnabled -> showNotificationFromDataPush(message)
        }
    }

    private fun showNotificationFromDataPush(message: RemoteMessage) {
        val data = message.data
        val title = data[DATA_KEY_TITLE]
        val body = data[DATA_KEY_BODY]
        // Skip non-displayable data pushes
        if (title.isNullOrEmpty() && body.isNullOrEmpty()) return

        pushNotificationDelegate.showNotification(
            dataMap = data,
            title = title,
            body = body,
            channelId = TANGEM_CHANNEL_ID,
            priority = message.priority,
            imageUrl = data[DATA_KEY_IMAGE]?.takeIf { it.isNotBlank() }?.toUri(),
            vibratePattern = null,
        )
    }

    private companion object {
        const val TANGEM_CHANNEL_ID = "Tangem General" // General channel for notifications

        // Customer.io places display fields in the FCM data payload under these keys for data/rich pushes.
        const val DATA_KEY_TITLE = "title"
        const val DATA_KEY_BODY = "body"
        const val DATA_KEY_IMAGE = "image"
    }
}