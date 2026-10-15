package com.tangem.core.analytics

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.*
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.analytics.models.OneTimePerSessionEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.FeatureCoroutineExceptionHandler
import dagger.Lazy
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches analytics events to every [AnalyticsHandler] one at a time, in send order, off the main thread.
 *
 * [filters] are ordered: the first filter applicable to an event decides whether and to which handlers it is sent.
 * Handlers and filters are resolved lazily: their dependencies (e.g. preferences, app scope) themselves depend on
 * analytics.
 */
@Singleton
internal class DefaultAnalyticsEventHandler @Inject constructor(
    private val handlers: Lazy<Set<@JvmSuppressWildcards AnalyticsHandler>>,
    private val filters: Lazy<List<@JvmSuppressWildcards AnalyticsEventFilter>>,
    staticInterceptors: Set<@JvmSuppressWildcards ParamsInterceptor>,
    dispatchers: CoroutineDispatcherProvider,
) : AnalyticsEventHandler,
    AnalyticsErrorHandler,
    AnalyticsExceptionHandler,
    AnalyticsUserIdHandler,
    ParamsInterceptorHolder {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val analyticsDispatcher = dispatchers.io.limitedParallelism(1)

    private val analyticsScope = CoroutineScope(
        SupervisorJob() + analyticsDispatcher + CoroutineName(NAME) + FeatureCoroutineExceptionHandler.create(NAME),
    )

    private val paramsInterceptors = ConcurrentHashMap(staticInterceptors.associateBy { it.id() })
    private val throttledEventsState = ConcurrentHashMap<String, Long>()
    private val analyticsMutex = Mutex()

    override fun addParamsInterceptor(interceptor: ParamsInterceptor) {
        paramsInterceptors[interceptor.id()] = interceptor
    }

    override fun removeParamsInterceptor(interceptorId: String): ParamsInterceptor? {
        return paramsInterceptors.remove(interceptorId)
    }

    override fun setUserId(userId: String) {
        analyticsScope.launch {
            val userIdHash = userId.hexToBytes()
                .calculateSha256()
                .toHexString()

            analyticsMutex.withLock {
                handlers.get().filterIsInstance<AnalyticsUserIdHandler>()
                    .forEach { handler -> handler.setUserId(userIdHash) }
            }
        }
    }

    override fun clearUserId() {
        analyticsScope.launch {
            analyticsMutex.withLock {
                handlers.get().filterIsInstance<AnalyticsUserIdHandler>()
                    .forEach { handler -> handler.clearUserId() }
            }
        }
    }

    override fun send(event: AnalyticsEvent) {
        analyticsScope.launch {
            if (event is OneTimePerSessionEvent && !shouldSendThrottledEvent(event)) {
                return@launch
            }
            val eventWithParams = event.withParams(applyParamsInterceptors(event))
            val eventFilter = filters.get().firstOrNull { it.canBeAppliedTo(eventWithParams) }

            analyticsMutex.withLock {
                when {
                    eventFilter == null -> handlers.get().forEach { handler -> handler.send(eventWithParams) }
                    eventFilter.canBeSent(eventWithParams) -> {
                        handlers.get()
                            .filter { handler -> eventFilter.canBeConsumedByHandler(handler, eventWithParams) }
                            .forEach { handler -> handler.send(eventWithParams) }
                    }
                }
            }
        }
    }

    override fun sendErrorEvent(event: AnalyticsEvent) {
        analyticsScope.launch {
            val eventWithParams = event.withParams(applyParamsInterceptors(event))
            analyticsMutex.withLock {
                handlers.get().filterIsInstance<AnalyticsErrorHandler>()
                    .forEach { handler -> handler.sendErrorEvent(eventWithParams) }
            }
        }
    }

    override fun sendException(event: ExceptionAnalyticsEvent) {
        analyticsScope.launch {
            analyticsMutex.withLock {
                handlers.get().filterIsInstance<AnalyticsExceptionHandler>()
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

    private fun shouldSendThrottledEvent(event: OneTimePerSessionEvent): Boolean {
        val now = System.currentTimeMillis()
        val id = event.oneTimeEventId
        return when (val throttleMs = event.throttleSeconds?.let(TimeUnit.SECONDS::toMillis)) {
            null -> throttledEventsState.putIfAbsent(id, now) == null
            else -> throttledEventsState.compute(id) { _, lastSendTime ->
                when {
                    lastSendTime == null -> now
                    now - lastSendTime >= throttleMs -> now
                    else -> lastSendTime
                }
            } == now
        }
    }

    private companion object {
        const val NAME = "Analytics"
    }
}