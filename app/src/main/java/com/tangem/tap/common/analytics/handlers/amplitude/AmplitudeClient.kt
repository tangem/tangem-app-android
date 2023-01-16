package com.tangem.tap.common.analytics.handlers.amplitude

import android.app.Application
import com.amplitude.api.Amplitude
import com.amplitude.api.AmplitudeClient
import com.tangem.common.Converter
import com.tangem.core.analytics.api.EventLogger
import org.json.JSONObject

/**
 * Created by Anton Zhilenkov on 22/09/2022.
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

    override fun logEvent(event: String, params: Map<String, String>) {
        client.logEvent(event, ParamsToJSONObjectConverter().convert(params))
    }
}

private class ParamsToJSONObjectConverter : Converter<Map<String, String>, JSONObject> {
    override fun convert(value: Map<String, String>): JSONObject = JSONObject().apply {
        value.forEach { this.put(it.key, it.value) }
    }
}
