package com.tangem.tap.common.analytics.handlers.amplitude

import android.app.Application
import com.amplitude.api.Amplitude
import com.amplitude.api.AmplitudeClient
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.UserIdHolder
import com.tangem.tap.common.analytics.AnalyticsEventsLogger
import com.tangem.utils.converter.Converter
import com.tangem.wallet.BuildConfig
import org.json.JSONObject

/**
[REDACTED_AUTHOR]
 */
interface AmplitudeAnalyticsClient : EventLogger, UserIdHolder

internal class AmplitudeClient(
    application: Application,
    key: String,
    private val logger: AnalyticsEventsLogger?,
) : AmplitudeAnalyticsClient {

    private val client: AmplitudeClient = Amplitude.getInstance()

    init {
        client.initialize(application, key)
        client.enableForegroundTracking(application)
        client.enableLogging(BuildConfig.TESTER_MENU_ENABLED)
    }

    override fun setUserId(userId: String) {
        client.setUserId(userId)
    }

    override fun clearUserId() {
        client.setUserId(null)
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        logger?.logEvent(event, params)
        client.logEvent(event, ParamsToJSONObjectConverter().convert(params))
    }
}

private class ParamsToJSONObjectConverter : Converter<Map<String, String>, JSONObject> {
    override fun convert(value: Map<String, String>): JSONObject = JSONObject().apply {
        value.forEach { this.put(it.key, it.value) }
    }
}