package com.tangem.core.analytics.models

/**
[REDACTED_AUTHOR]
 *
 * Base analytics event
 * @param category event category (e.g. "Wallet", "Settings")
 * @param event event name (e.g. "Create", "Backup")
 * @param params event parameters
 */
open class AnalyticsEvent(
    val category: String,
    val event: String,
    var params: Map<String, String> = mapOf(),
) {

    val id: String = "[$category] $event"
}