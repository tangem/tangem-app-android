package com.tangem.core.analytics

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.*
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
[REDACTED_AUTHOR]
 */
interface GlobalAnalyticsEventHandler :
    AnalyticsEventHandler,
    AnalyticsHandlerHolder,
    AnalyticsFilterHolder,
    ParamsInterceptorHolder,
    AnalyticsErrorHandler,
    AnalyticsExceptionHandler,
    AnalyticsUserIdHandler

object Analytics : GlobalAnalyticsEventHandler {

    private val analyticsScope: CoroutineScope by lazy { createScope() }

    private val handlers = mutableMapOf<String, AnalyticsHandler>()
    private val paramsInterceptors = ConcurrentHashMap<String, ParamsInterceptor>()
    private val analyticsFilters = mutableSetOf<AnalyticsEventFilter>()
    private val analyticsMutex = Mutex()

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

    override fun removeParamsInterceptor(interceptorId: String): ParamsInterceptor? {
        return paramsInterceptors.remove(interceptorId)
    }

    override fun setUserId(userId: String) {
        analyticsScope.launch {
            val userIdHash = userId.calculateSha256().toHexString()

            analyticsMutex.withLock {
                analyticsHandlers.filterIsInstance<AnalyticsUserIdHandler>()
                    .forEach { handler -> handler.setUserId(userIdHash) }
            }
        }
    }

    override fun clearUserId() {
        analyticsScope.launch {
            analyticsMutex.withLock {
                analyticsHandlers.filterIsInstance<AnalyticsUserIdHandler>()
                    .forEach { handler -> handler.clearUserId() }
            }
        }
    }

    override fun send(event: AnalyticsEvent) {
        analyticsScope.launch {
            event.params = applyParamsInterceptors(event)
            val eventFilter = analyticsFilters.firstOrNull { it.canBeAppliedTo(event) }

            analyticsMutex.withLock {
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
    }

    override fun sendErrorEvent(event: AnalyticsEvent) {
        analyticsScope.launch {
            event.params = applyParamsInterceptors(event)
            analyticsMutex.withLock {
                analyticsHandlers.filterIsInstance<AnalyticsErrorHandler>()
                    .forEach { handler -> handler.sendErrorEvent(event) }
            }
        }
    }

    override fun sendException(event: ExceptionAnalyticsEvent) {
        analyticsScope.launch {
            analyticsMutex.withLock {
                analyticsHandlers.filterIsInstance<AnalyticsExceptionHandler>()
                    .forEach { it.sendException(event) }
            }
        }
    }

    private suspend fun applyParamsInterceptors(event: AnalyticsEvent): MutableMap<String, String> {
        val interceptedParams = event.params.toMutableMap()
        analyticsMutex.withLock {
            paramsInterceptors.values
                .filter { it.canBeAppliedTo(event) }
                .forEach { it.intercept(interceptedParams) }
        }
        return interceptedParams
    }

    private fun createScope(): CoroutineScope {
        val name = "Analytics"
        val dispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        val exHandler = FeatureCoroutineExceptionHandler.create(name)
        return CoroutineScope(SupervisorJob() + dispatcher + CoroutineName(name) + exHandler)
    }
}