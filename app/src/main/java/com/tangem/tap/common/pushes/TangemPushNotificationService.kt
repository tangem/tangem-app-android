package com.tangem.tap.common.pushes

import android.annotation.SuppressLint
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.AndroidEntryPoint
import io.customer.messagingpush.CustomerIOFirebaseMessagingService
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
internal class TangemPushNotificationService : FirebaseMessagingService() {

    @Inject
    lateinit var pushMessageHandler: PushMessageHandler

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
        if (notification != null) {
            pushNotificationDelegate.showNotification(
                dataMap = message.data,
                title = notification.title,
                body = notification.body,
                channelId = notification.channelId ?: TANGEM_CHANNEL_ID,
                priority = message.priority,
                imageUrl = notification.imageUrl,
                vibratePattern = notification.vibrateTimings,
            )
        } else {
            showNotificationFromDataPush(message)
        }
    }

    /**
     * New flow for data-only pushes. Unlike a `notification`-block message, a data-only message reaches
     * [onMessageReceived] in every app state (foreground and background), so it must be rendered here or
     * it is never shown. Display fields follow the Customer.io data-payload convention ([DATA_TITLE_KEY],
     * [DATA_BODY_KEY], [DATA_IMAGE_KEY]); backend data-only pushes use the same keys. Silent pushes
     * (no title and no body — e.g. delivered-metric or data refresh only) show nothing.
     */
    private fun showNotificationFromDataPush(message: RemoteMessage) {
        val data = message.data

        val title = data[DATA_TITLE_KEY]
        val body = data[DATA_BODY_KEY]
        if (title.isNullOrBlank() && body.isNullOrBlank()) return

        pushNotificationDelegate.showNotification(
            dataMap = data,
            title = title,
            body = body,
            channelId = TANGEM_CHANNEL_ID,
            priority = message.priority,
            imageUrl = data[DATA_IMAGE_KEY]?.takeIf { it.isNotBlank() }?.toUri(),
            vibratePattern = null,
        )
    }

    private companion object {
        const val TANGEM_CHANNEL_ID = "Tangem General" // General channel for notifications

        // Same keys as Customer.io uses in its data payload.
        // Keys: `title` / `body` / `image`.
        const val DATA_TITLE_KEY = "title"
        const val DATA_BODY_KEY = "body"
        const val DATA_IMAGE_KEY = "image"
    }
}