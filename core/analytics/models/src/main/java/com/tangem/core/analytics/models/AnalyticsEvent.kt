package com.tangem.core.analytics.models

/**
[REDACTED_AUTHOR]
 *
 * Base analytics event
 * @param category event category (e.g. "Wallet", "Settings")
 * @param event event name (e.g. "Create", "Backup")
 * @param params event parameters
 * @param isSpecific if true, event will be sent only to ***specific*** analytics handlers
 * @see [com.tangem.core.analytics.api.AnalyticsHandler.specific]
 */
open class AnalyticsEvent(
    val category: String,
    val event: String,
    var params: Map<String, String> = mapOf(),
    val isSpecific: Boolean = false,
) {

    val id: String = "[$category] $event"
}