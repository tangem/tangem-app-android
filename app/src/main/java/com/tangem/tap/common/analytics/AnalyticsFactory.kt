package com.tangem.tap.common.analytics

import com.tangem.tap.common.analytics.api.AnalyticsEventFilter
import com.tangem.tap.common.analytics.api.AnalyticsHandlerBuilder
import com.tangem.tap.common.analytics.api.ParamsInterceptor

/**
[REDACTED_AUTHOR]
 */
class AnalyticsFactory {

    private val builders = mutableListOf<AnalyticsHandlerBuilder>()
    private val filters = mutableListOf<AnalyticsEventFilter>()
    private val interceptors = mutableListOf<ParamsInterceptor>()

    fun addHandlerBuilder(builder: AnalyticsHandlerBuilder) {
        builders.add(builder)
    }

    fun addFilter(filter: AnalyticsEventFilter) {
        filters.add(filter)
    }

    fun addParamsInterceptor(interceptor: ParamsInterceptor) {
        interceptors.add(interceptor)
    }

    fun build(analytics: Analytics, data: AnalyticsHandlerBuilder.Data) {
        builders.mapNotNull { it.build(data) }.forEach { analytics.addHandler(it.id(), it) }
        filters.forEach { analytics.addFilter(it) }
        interceptors.forEach { analytics.addParamsInterceptor(it) }

        builders.clear()
        filters.clear()
        interceptors.clear()
    }
}