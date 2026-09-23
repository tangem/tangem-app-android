package com.tangem.tap.common.analytics

import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.ExceptionLogger
import com.tangem.utils.logging.TangemLogger
import org.json.JSONObject

class AnalyticsEventsLogger(
    private val name: String,
) : EventLogger, ExceptionLogger {

    override fun logEvent(event: String, params: Map<String, String>) {
        TangemLogger.d(prettyPrint(event = event, params = params))
    }

    override fun logException(error: Throwable, params: Map<String, String>) {
        TangemLogger.e(prettyPrint(event = "error", params = params), error)
    }

    private fun prettyPrint(event: String, params: Map<String, String>): String {
        val paramsJson = JSONObject()
        params.forEach { (key, value) -> paramsJson.put(key, value) }

        return JSONObject()
            .put("client", name)
            .put("event", event)
            .put("params", paramsJson)
            .toString(JSON_INDENT_SPACES)
    }

    private companion object {
        const val JSON_INDENT_SPACES = 3
    }
}