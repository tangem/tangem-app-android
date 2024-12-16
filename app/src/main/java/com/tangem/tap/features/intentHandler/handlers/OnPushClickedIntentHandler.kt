package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.events.Push
import com.tangem.tap.features.intentHandler.IntentHandler

internal class OnPushClickedIntentHandler(val analyticsEventHandler: AnalyticsEventHandler) : IntentHandler {

    override fun handleIntent(intent: Intent?, isFromForeground: Boolean): Boolean {
        val fromPush = intent?.extras?.getBoolean(IS_OPENED_FROM_PUSH) ?: false

        return if (fromPush) {
            analyticsEventHandler.send(Push.PushNotificationOpened)
            true
        } else {
            false
        }
    }

    companion object {
        const val IS_OPENED_FROM_PUSH = "IS_OPENED_FROM_PUSH"
    }
}