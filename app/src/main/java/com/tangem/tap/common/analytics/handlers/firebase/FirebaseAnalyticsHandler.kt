package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.converters.AnalyticsErrorConverter

class FirebaseAnalyticsHandler(
    private val client: FirebaseAnalyticsClient,
) : AnalyticsHandler, AnalyticsErrorHandler, AnalyticsExceptionHandler, AnalyticsUserIdHandler {

    private val errorConverter = AnalyticsErrorConverter()

    override fun id(): String = ID

    override fun setUserId(userId: String) {
        client.setUserId(userId)
    }

    override fun clearUserId() {
        client.clearUserId()
    }

    override fun send(eventId: String, params: Map<String, String>) {
        client.logEvent(eventId, params)
    }

    override fun sendException(event: ExceptionAnalyticsEvent) {
        if (!errorConverter.canBeHandled(event.exception)) return

        val errorParams = errorConverter.convert(event.exception).toMutableMap()
        errorParams.putAll(event.params)

        client.logException(event.exception, event.params)
    }

    override fun sendErrorEvent(event: AnalyticsEvent) {
        send(event)
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