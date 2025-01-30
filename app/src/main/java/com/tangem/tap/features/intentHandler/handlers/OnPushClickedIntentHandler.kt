package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.events.Push
import com.tangem.tap.features.intentHandler.IntentHandler

internal class OnPushClickedIntentHandler(val analyticsEventHandler: AnalyticsEventHandler) : IntentHandler {

    override fun handleIntent(intent: Intent?, isFromForeground: Boolean): Boolean {
        val fromPush = intent?.extras?.containsKey(OPENED_FROM_GCM_PUSH) ?: false

        return if (fromPush) {
            analyticsEventHandler.send(Push.PushNotificationOpened)
            true
        } else {
            false
        }
    }

    companion object {
        const val OPENED_FROM_GCM_PUSH = "google.sent_time" // every bundle from FCM contains this key
    }
}