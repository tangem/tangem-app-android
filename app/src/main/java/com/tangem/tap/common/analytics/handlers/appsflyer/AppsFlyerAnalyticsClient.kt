package com.tangem.tap.common.analytics.handlers.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.UserIdHolder
import com.tangem.tap.common.analytics.handlers.firebase.UnderscoreAnalyticsEventConverter

interface AppsFlyerAnalyticsClient : EventLogger, UserIdHolder

internal class AppsFlyerClient(
    private val context: Context,
    key: String,
    appId: String,
) : AppsFlyerAnalyticsClient {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()
    private val eventConverter = UnderscoreAnalyticsEventConverter()

    init {
        appsFlyerLib.init(key, null, context)
        appsFlyerLib.setAppId(appId)
        appsFlyerLib.start(context)
    }

    override fun setUserId(userId: String) {
        appsFlyerLib.setCustomerUserId(userId)
    }

    override fun clearUserId() {
        appsFlyerLib.setCustomerUserId(null)
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        appsFlyerLib.logEvent(
            context,
            event,
            eventConverter.convertEventParams(params),
        )
    }
}