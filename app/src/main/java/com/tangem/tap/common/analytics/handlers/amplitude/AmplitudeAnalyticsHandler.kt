package com.tangem.tap.common.analytics.handlers.amplitude

import android.app.Application
import com.amplitude.api.Amplitude
import com.amplitude.api.AmplitudeClient
import com.tangem.common.Converter
import com.tangem.common.card.Card
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.AnalyticsEventHandler
import org.json.JSONObject

class AmplitudeAnalyticsHandler(
    application: Application,
    key: String,
) : AnalyticsEventHandler {

    private val client: AmplitudeClient = Amplitude.getInstance()

    init {
        client.initialize(application, key)
        client.enableForegroundTracking(application)
    }

    override fun handleEvent(event: String, params: Map<String, String>) {
        client.logEvent(event, ParamsJSONObjectConverter().convert(params))
    }

    override fun handleAnalyticsEvent(
        event: AnalyticsEvent,
        params: Map<String, String>,
        card: Card?,
        blockchain: String?,
    ) {
        handleEvent(event.event, prepareParams(card, blockchain, params))
    }

    class ParamsJSONObjectConverter : Converter<Map<String, String>, JSONObject> {
        override fun convert(value: Map<String, String>): JSONObject = JSONObject().apply {
            value.forEach { this.put(it.key, it.value) }
        }
    }
}

