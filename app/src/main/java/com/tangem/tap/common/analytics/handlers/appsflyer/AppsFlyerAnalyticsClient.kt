package com.tangem.tap.common.analytics.handlers.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.tangem.core.analytics.api.EventLogger

interface AppsFlyerAnalyticsClient : EventLogger

internal class AppsFlyerClient(
    private val context: Context,
    key: String,
    appId: String,
) : AppsFlyerAnalyticsClient {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()

    init {
        appsFlyerLib.init(key, null, context)
        appsFlyerLib.setAppId(appId)
        appsFlyerLib.start(context)
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        appsFlyerLib.logEvent(context, event, params)
    }
}