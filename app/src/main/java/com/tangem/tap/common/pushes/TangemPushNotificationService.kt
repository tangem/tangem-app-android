package com.tangem.tap.common.pushes

import android.annotation.SuppressLint
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
    lateinit var pushMessageDispatcher: PushMessageDispatcher

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        TangemLogger.d("New FCM token received: $token")

        CustomerIOFirebaseMessagingService.onNewToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        pushMessageDispatcher.dispatch(message)
    }
}