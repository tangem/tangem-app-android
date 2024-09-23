package com.tangem.data.markets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed interface MarketsDataAnalyticsEvent {

    sealed class List(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Markets", event = event, params = params), MarketsDataAnalyticsEvent {

        data object Error : List(event = "Data Error")
    }

    sealed class Details(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params), MarketsDataAnalyticsEvent {

        data class Error(
            val request: Request,
            val tokenSymbol: String,
        ) : Details(
            event = "Data Error",
            params = mapOf(
                "Source" to request.source,
                "Token" to tokenSymbol,
            ),
        ) {

            enum class Request(val source: String) {
                Chart("Chart"),
                Info("Blocks"),
            }
        }
    }

    fun toEvent(): AnalyticsEvent = when (this) {
        is ChartNullValuesError -> this
        is List -> this
        is Details -> this
    }

    data class ChartNullValuesError(
        val requestPath: String,
    ) : AnalyticsEvent(
        category = "Markets / Chart",
        event = "Data Error",
        params = mapOf("Request path" to requestPath),
        error = IllegalStateException(
            "Chart data contains null values from the API",
        ),
    ),
        MarketsDataAnalyticsEvent
}