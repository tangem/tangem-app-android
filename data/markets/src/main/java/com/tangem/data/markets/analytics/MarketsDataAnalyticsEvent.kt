package com.tangem.data.markets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue

sealed interface MarketsDataAnalyticsEvent {

    sealed class List(
        event: String,
        params: Map<String, EventValue> = mapOf(),
    ) : AnalyticsEvent(category = "Markets", event = event, params = params), MarketsDataAnalyticsEvent {

        data class Error(
            val errorType: Type,
            val errorCode: Int? = null,
        ) : List(
            event = "Data Error",
            params = buildMap {
                put("Error Type", errorType.value.asStringValue())
                errorCode?.let { put("Error Code", it.asStringValue()) }
            },
        )
    }

    sealed class Details(
        event: String,
        params: Map<String, EventValue> = mapOf(),
    ) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params), MarketsDataAnalyticsEvent {

        data class Error(
            val request: Request,
            val tokenSymbol: String,
            val errorType: Type,
            val errorCode: Int? = null,
        ) : Details(
            event = "Data Error",
            params = buildMap {
                put("Source", request.source.asStringValue())
                put("Token", tokenSymbol.asStringValue())
                errorCode?.let { put("Error Code", it.asStringValue()) }
                put("Error Type", errorType.value.asStringValue())
            },
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
        val errorType: Type,
        val errorCode: Int? = null,
    ) : AnalyticsEvent(
        category = "Markets / Chart",
        event = "Data Error",
        params = buildMap {
            put("Request path", requestPath.asStringValue())
            errorCode?.let { put("Error Code", it.asStringValue()) }
            put("Error Type", errorType.value.asStringValue())
            put("Error Description", "Chart data contains null values from the API".asStringValue())
        },
    ),
        MarketsDataAnalyticsEvent

    enum class Type(val value: String) {
        Http("Http"),
        Timeout("Timeout"),
        Network("Network"),
        Custom("Custom"),
        Unknown("Unknown"),
    }
}