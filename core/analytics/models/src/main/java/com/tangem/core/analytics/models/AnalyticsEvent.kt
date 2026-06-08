package com.tangem.core.analytics.models

/**
[REDACTED_AUTHOR]
 */
open class AnalyticsEvent(
    val category: String,
    val event: String,
    params: Map<String, String> = mapOf(),
) {
    var params: Map<String, String> = params
        private set

    val id: String = "[$category] $event"

    /**
     * Enriches this event with [newParams] and returns the same instance, preserving the concrete
     * runtime type so that marker interfaces survive enrichment and the event can still be routed
     * by the analytics filters and handlers.
     */
    fun withParams(newParams: Map<String, String>): AnalyticsEvent {
        params = newParams
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnalyticsEvent

        if (category != other.category) return false
        if (event != other.event) return false
        if (params != other.params) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + params.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}