package com.tangem.tap.common.analytics

import android.app.Application
import android.util.Log
import com.amplitude.api.Amplitude
import com.amplitude.api.AmplitudeClient
import com.shopify.buy3.Storefront
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import org.json.JSONObject

class AmplitudeAnalyticsHandler(application: Application, apiKey:String) : AnalyticsHandler() {

    private val client: AmplitudeClient = Amplitude.getInstance()
        .initialize(application.applicationContext, apiKey)
        .enableForegroundTracking(application)

    override fun triggerEvent(
        event: AnalyticsEvent,
        card: Card?,
        blockchain: String?,
        params: Map<String, String>
    ) {
        Log.e("TEST ","_"+event.event)
        client.logEvent(event.event, prepareParams(card, blockchain, params).toJson())
    }

    override fun triggerEvent(event: String, params: Map<String, String>) {
        client.logEvent(event, params.toJson())
    }

    override fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: Analytics.ActionToLog,
        parameters: Map<AnalyticsParam, String>?,
        card: Card?
    ) {
        //TODO
    }

    override fun logError(error: Throwable, params: Map<String, String>) {
        //TODO
    }

    override fun getOrderEvent(): String {
        //TODO
        return ""
    }

    override fun getOrderParams(order: Storefront.Order): Map<String, String> {
        //TODO
        return emptyMap()
    }

    private fun Map<String, String>.toJson(): JSONObject {
        val json = JSONObject()
        this.map { json.put(it.key, it.value) }
        return json
    }

}
