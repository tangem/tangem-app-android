package com.tangem.tap.common.analytics

import com.tangem.domain.common.FeatureCoroutineExceptionHandler
import com.tangem.tap.common.analytics.api.AnalyticsEventFilter
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsFilterHolder
import com.tangem.tap.common.analytics.api.AnalyticsHandler
import com.tangem.tap.common.analytics.api.AnalyticsHandlerHolder
import com.tangem.tap.common.analytics.api.ParamsInterceptor
import com.tangem.tap.common.analytics.api.ParamsInterceptorHolder
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Created by Anton Zhilenkov on 27.10.2022.
 */
interface GlobalAnalyticsEventHandler : AnalyticsEventHandler,
    AnalyticsHandlerHolder,
    AnalyticsFilterHolder,
    ParamsInterceptorHolder

object Analytics : GlobalAnalyticsEventHandler {

    private val analyticsScope: CoroutineScope by lazy { createScope() }

    private val handlers = mutableMapOf<String, AnalyticsHandler>()
    private val paramsInterceptors = mutableMapOf<String, ParamsInterceptor>()
    private val analyticsFilters = mutableSetOf<AnalyticsEventFilter>()

    private val analyticsHandlers: List<AnalyticsHandler>
        get() = handlers.values.toList()

    override fun addHandler(name: String, handler: AnalyticsHandler) {
        handlers[name] = handler
    }

    override fun removeHandler(name: String): AnalyticsHandler? {
        return handlers.remove(name)
    }

    override fun addFilter(filter: AnalyticsEventFilter) {
        analyticsFilters.add(filter)
    }

    override fun removeFilter(filter: AnalyticsEventFilter): Boolean {
        return analyticsFilters.remove(filter)
    }

    override fun addParamsInterceptor(interceptor: ParamsInterceptor) {
        paramsInterceptors[interceptor.id()] = interceptor
    }

    override fun removeParamsInterceptor(interceptor: ParamsInterceptor): ParamsInterceptor? {
        return paramsInterceptors.remove(interceptor.id())
    }

    override fun send(event: AnalyticsEvent) {
        analyticsScope.launch {
            event.params = applyParamsInterceptors(event)
            val eventFilter = analyticsFilters.firstOrNull { it.canBeAppliedTo(event) }

            when {
                eventFilter == null -> analyticsHandlers.forEach { handler -> handler.send(event) }
                eventFilter.canBeSent(event) -> {
                    analyticsHandlers
                        .filter { handler -> eventFilter.canBeConsumedByHandler(handler, event) }
                        .forEach { handler -> handler.send(event) }
                }
            }
        }
    }

    private fun applyParamsInterceptors(event: AnalyticsEvent): MutableMap<String, String> {
        val interceptedParams = event.params.toMutableMap()
        paramsInterceptors.values
            .filter { it.canBeAppliedTo(event) }
            .forEach { it.intercept(interceptedParams) }

        return interceptedParams
    }

    private fun createScope(): CoroutineScope {
        val name = "Analytics"
        val dispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        val exHandler = FeatureCoroutineExceptionHandler.create(name)
        return CoroutineScope(SupervisorJob() + dispatcher + CoroutineName(name) + exHandler)
    }
}
