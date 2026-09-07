package com.tangem.tap.common.pushes

import android.content.Context
import androidx.core.net.toUri
import com.google.firebase.messaging.RemoteMessage
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.tap.common.pushes.PushMessageDispatcher.Companion.DATA_BODY_KEY
import com.tangem.tap.common.pushes.PushMessageDispatcher.Companion.DATA_IMAGE_KEY
import com.tangem.tap.common.pushes.PushMessageDispatcher.Companion.DATA_TITLE_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import io.customer.messagingpush.CustomerIOFirebaseMessagingService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PushMessageDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val pushMessageHandler: PushMessageHandler,
) {
    private val pushNotificationDelegate: PushNotificationDelegate by lazy {
        PushNotificationDelegate(context, analyticsExceptionHandler)
    }

    fun dispatch(message: RemoteMessage) {
        CustomerIOFirebaseMessagingService.onMessageReceived(
            context = context,
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
     * [dispatch] in every app state (foreground and background), so it must be rendered here or it is never
     * shown. Display fields follow the Customer.io data-payload convention ([DATA_TITLE_KEY], [DATA_BODY_KEY],
     * [DATA_IMAGE_KEY]); backend data-only pushes use the same keys. Silent pushes (no title and no body —
     * e.g. delivered-metric or data refresh only) show nothing.
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