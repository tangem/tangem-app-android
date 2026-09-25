package com.tangem.datasource.api.common.response.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.datasource.local.logs.LogsSanitizer

internal data class ApiErrorEvent(
    val endpoint: String,
    val code: Int,
    val message: String,
) : AnalyticsEvent(
    category = "Tangem API Service",
    event = "Exception",
    params = mapOf(
        "Endpoint" to endpoint,
        "Code" to code.toString(),
        "Message" to message,
    ),
) {

    companion object {

        private const val MAX_MESSAGE_LENGTH = 200
        private const val TRUNCATION_MARKER = "…"

        /**
         * Builds the event from a raw HTTP error body. The body goes to a third-party analytics backend, so it is
         * reduced to what is needed to recognise the failure: hex runs (addresses, hashes, ids, tokens) are masked
         * with the same rule as the support logs and the text is capped at [MAX_MESSAGE_LENGTH] characters.
         */
        fun fromErrorBody(endpoint: String, code: Int, errorBody: String): ApiErrorEvent {
            return ApiErrorEvent(endpoint = endpoint, code = code, message = errorBody.toAnalyticsMessage())
        }

        internal fun String.toAnalyticsMessage(): String {
            val sanitized = LogsSanitizer.sanitize(this)
            return if (sanitized.length <= MAX_MESSAGE_LENGTH) {
                sanitized
            } else {
                sanitized.take(MAX_MESSAGE_LENGTH) + TRUNCATION_MARKER
            }
        }
    }
}
