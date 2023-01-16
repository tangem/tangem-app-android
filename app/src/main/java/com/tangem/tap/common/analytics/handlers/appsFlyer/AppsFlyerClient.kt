package com.tangem.tap.common.analytics.handlers.appsFlyer

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.tangem.core.analytics.api.EventLogger

/**
 * Created by Anton Zhilenkov on 22/09/2022.
 */
interface AppsFlyerAnalyticsClient : EventLogger

internal class AppsFlyerClient(
    private val context: Context,
    key: String,
) : AppsFlyerAnalyticsClient {

    private val appsFlyerLib: AppsFlyerLib = AppsFlyerLib.getInstance()

    init {
        appsFlyerLib.init(key, null, context)
        appsFlyerLib.start(context)
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        appsFlyerLib.logEvent(context, event, params)
    }
}
