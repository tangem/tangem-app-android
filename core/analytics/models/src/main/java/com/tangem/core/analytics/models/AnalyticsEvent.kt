package com.tangem.core.analytics.models

/**
[REDACTED_AUTHOR]
 */
open class AnalyticsEvent(
    val category: String,
    val event: String,
    val params: Map<String, String> = mapOf(),
) {
    val id: String = "[$category] $event"

    fun withParams(newParams: Map<String, String>) = AnalyticsEvent(category, event, newParams)

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