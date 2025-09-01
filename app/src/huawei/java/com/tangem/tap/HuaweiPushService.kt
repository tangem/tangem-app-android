package com.tangem.tap

import android.os.Bundle
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.tangem.google.GoogleServicesHelper
import com.tangem.tap.common.pushes.PushNotificationDelegate
import timber.log.Timber

class HuaweiPushService : HmsMessageService() {

    private val pushNotificationDelegate: PushNotificationDelegate by lazy {
        PushNotificationDelegate(applicationContext)
    }

    override fun onNewToken(token: String?, bundle: Bundle?) {
        super.onNewToken(token, bundle)
        Timber.i("HuaweiPushService: On new token from HuaweiService: $token")
    }

    override fun onTokenError(e: Exception?, bundle: Bundle?) {
        super.onTokenError(e, bundle)
        Timber.i("HuaweiPushService: Fetching token from HuaweiService failed cause: ${e?.message}")
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)
        val isGoogleServicesAvailable = GoogleServicesHelper.checkGoogleServicesAvailability(this)
        if (isGoogleServicesAvailable) return
        val notification = message?.notification ?: return
        val channelId = notification.channelId ?: TANGEM_CHANNEL_ID

        pushNotificationDelegate.showNotification(
            dataMap = message.dataOfMap,
            title = notification.title,
            body = notification.body,
            channelId = channelId,
            priority = message.urgency,
            imageUrl = notification.imageUrl,
            vibratePattern = notification.vibrateConfig,
        )
    }

    private companion object {
        const val TANGEM_CHANNEL_ID = "Tangem General" // General channel for notifications
    }
}