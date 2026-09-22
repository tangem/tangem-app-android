package com.tangem.data.markets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR
import com.tangem.core.analytics.models.OtelIncludedEvent
import com.tangem.core.analytics.models.OtelMetric

sealed interface MarketsDataAnalyticsEvent {

    sealed class List(
        event: String,
        params: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent(category = "Markets", event = event, params = params), MarketsDataAnalyticsEvent {

        data class Error(
            val errorType: Type,
            val errorCode: Int? = null,
            val errorMessage: String,
        ) : List(
            event = "Data Error",
            params = buildMap {
                put(AnalyticsParam.ERROR_TYPE, errorType.value)
                put(AnalyticsParam.ERROR_CODE, errorCode?.toString() ?: IS_NOT_HTTP_ERROR)
                put(AnalyticsParam.ERROR_MESSAGE, errorMessage)
            },
        ), OtelIncludedEvent {

            override val otelMetric = OtelMetric.Counter(allowedParams = setOf(AnalyticsParam.ERROR_CODE))
        }
    }

    sealed class Details(
        event: String,
        params: Map<String, String> = emptyMap(),
    ) : AnalyticsEvent(category = "Markets / Chart", event = event, params = params), MarketsDataAnalyticsEvent {

        data class Error(
            val request: Request,
            val tokenSymbol: String,
            val errorType: Type,
            val errorCode: Int? = null,
            val errorMessage: String,
        ) : Details(
            event = "Data Error",
            params = buildMap {
                put("Source", request.source)
                put("Token", tokenSymbol)
                put(AnalyticsParam.ERROR_TYPE, errorType.value)
                put(AnalyticsParam.ERROR_CODE, errorCode?.toString() ?: IS_NOT_HTTP_ERROR)
                put(AnalyticsParam.ERROR_MESSAGE, errorMessage)
            },
        ), OtelIncludedEvent {

            override val otelMetric = OtelMetric.Counter(allowedParams = setOf(AnalyticsParam.ERROR_CODE))

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
            put("Request path", requestPath)
            put(AnalyticsParam.ERROR_TYPE, errorType.value)
            put(AnalyticsParam.ERROR_CODE, errorCode?.toString() ?: IS_NOT_HTTP_ERROR)
            put(AnalyticsParam.ERROR_MESSAGE, "Chart data contains null values from the API")
        },
    ),
        MarketsDataAnalyticsEvent,
        OtelIncludedEvent {

        override val otelMetric = OtelMetric.Counter(allowedParams = setOf(AnalyticsParam.ERROR_CODE))
    }

    enum class Type(val value: String) {
        Http("Http"),
        Timeout("Timeout"),
        Network("Network"),
        Custom("Custom"),
        Unknown("Unknown"),
    }
}