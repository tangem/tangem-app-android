package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.ErrorEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.converters.AnalyticsErrorConverter

class FirebaseAnalyticsHandler(
    private val client: FirebaseAnalyticsClient,
) : AnalyticsHandler, ErrorEventHandler {

    override fun id(): String = ID

    override fun send(event: String, params: Map<String, String>) {
        client.logEvent(event, params)
    }

    override fun send(event: AnalyticsEvent) {
        val error = event.error
        if (error == null) {
            super.send(event)
        } else {
            val errorConverter = AnalyticsErrorConverter()
            if (!errorConverter.canBeHandled(error)) return

            val errorParams = errorConverter.convert(error).toMutableMap()
            errorParams["Category"] = event.category
            errorParams["Event"] = event.event
            errorParams.putAll(event.params)
            send(error, errorParams)
        }
    }

    override fun send(error: Throwable, params: Map<String, String>) {
        client.logErrorEvent(error, params)
    }

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
