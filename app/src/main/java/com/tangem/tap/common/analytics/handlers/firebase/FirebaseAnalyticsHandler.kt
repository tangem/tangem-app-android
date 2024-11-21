package com.tangem.tap.common.analytics.handlers.firebase

import com.google.firebase.analytics.FirebaseAnalytics
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.ErrorEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.converters.AnalyticsErrorConverter
import com.tangem.tap.common.analytics.events.Shop

class FirebaseAnalyticsHandler(
    private val client: FirebaseAnalyticsClient,
) : AnalyticsHandler, ErrorEventHandler {

    override fun id(): String = ID

    override fun send(eventId: String, params: Map<String, EventValue>) {
        client.logEvent(eventId, params)
    }

    override fun send(event: AnalyticsEvent) {
        val error = event.error
        when {
            error != null -> {
                val errorConverter = AnalyticsErrorConverter()
                if (!errorConverter.canBeHandled(error)) return

                val errorParams = errorConverter.convert(error)
                    .mapValues { it.value.asStringValue() as EventValue }
                    .toMutableMap()
                errorParams["Category"] = event.category.asStringValue()
                errorParams["Event"] = event.event.asStringValue()
                errorParams.putAll(event.params)
                send(error, errorParams)
            }
            event is Shop.Purchased -> {
                send(FirebaseAnalytics.Event.PURCHASE, event.params)
            }
            else -> {
                super.send(event)
            }
        }
    }

    override fun send(error: Throwable, params: Map<String, EventValue>) {
        client.logErrorEvent(error, params)
    }

    private fun String.asStringValue() = EventValue.StringValue(this)

    companion object {
        const val ID = "Firebase"
    }

    class Builder : AnalyticsHandlerBuilder {
        override fun build(data: AnalyticsHandlerBuilder.Data): AnalyticsHandler? = when {
            !data.isDebug -> FirebaseClient()
            data.isDebug && data.logConfig.firebase -> FirebaseLogClient(data.jsonConverter)
            else -> null
        }?.let { FirebaseAnalyticsHandler(it) }
    }
}