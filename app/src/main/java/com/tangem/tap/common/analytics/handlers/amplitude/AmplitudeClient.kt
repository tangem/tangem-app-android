package com.tangem.tap.common.analytics.handlers.amplitude

import android.app.Application
import com.amplitude.api.Amplitude
import com.amplitude.api.AmplitudeClient
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.models.EventValue
import com.tangem.utils.converter.Converter
import org.json.JSONObject
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
interface AmplitudeAnalyticsClient : EventLogger

internal class AmplitudeClient(
    application: Application,
    key: String,
) : AmplitudeAnalyticsClient {

    private val client: AmplitudeClient = Amplitude.getInstance()

    init {
        client.initialize(application, key)
        client.enableForegroundTracking(application)
    }

    override fun logEvent(event: String, params: Map<String, EventValue>) {
        client.logEvent(event, ParamsToJSONObjectConverter().convert(params))
    }
}

private class ParamsToJSONObjectConverter : Converter<Map<String, EventValue>, JSONObject> {
    override fun convert(value: Map<String, EventValue>): JSONObject = JSONObject().apply {
        value.forEach { this.put(it.key, it.value.getValue()) }
    }
}