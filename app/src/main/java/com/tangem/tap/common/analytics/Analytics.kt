package com.tangem.tap.common.analytics

import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.common.FeatureCoroutineExceptionHandler
import com.tangem.tap.common.analytics.api.AnalyticsEventFilter
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.AnalyticsFilterHolder
import com.tangem.tap.common.analytics.api.AnalyticsHandlerHolder
import com.tangem.tap.common.analytics.api.BlockchainSdkErrorEventHandler
import com.tangem.tap.common.analytics.api.CardSdkErrorEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventLogger
import com.tangem.tap.common.analytics.api.ParamsInterceptor
import com.tangem.tap.common.analytics.api.ParamsInterceptorHolder
import com.tangem.tap.common.analytics.api.SdkErrorEventHandler
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import com.tangem.tap.common.extensions.filterNotNull
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
[REDACTED_AUTHOR]
 */
interface GlobalAnalyticsEventHandler : AnalyticsEventHandler,
    ErrorEventHandler,
    SdkErrorEventHandler,
    AnalyticsHandlerHolder,
    AnalyticsFilterHolder,
    ParamsInterceptorHolder {

    fun send(event: AnalyticsEvent, card: Card? = null, blockchain: String? = null)
}

object Analytics : GlobalAnalyticsEventHandler {

    private val analyticsScope: CoroutineScope by lazy { createScope() }

    private val handlers = mutableMapOf<String, AnalyticsEventHandler>()
    private val analyticsFilters = mutableSetOf<AnalyticsEventFilter>()
    private val paramsInterceptors = mutableMapOf<String, ParamsInterceptor>()

    private val analyticsHandlers: List<AnalyticsEventHandler>
        get() = handlers.values.toList()

    override fun id(): String = analyticsHandlers.joinToString(", ") { it.id() }

    override fun addHandler(name: String, handler: AnalyticsEventHandler) {
        handlers[name] = handler
    }

    override fun removeHandler(name: String): AnalyticsEventHandler? {
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

    override fun send(event: String, params: Map<String, String>) {
        analyticsScope.launch {
            analyticsHandlers.forEach { it.send(event, params.interceptParams()) }
        }
    }

    override fun send(event: AnalyticsEvent, card: Card?, blockchain: String?) {
        analyticsScope.launch {
            val eventString = prepareEventString(event.category, event.event)
            val eventParams = prepareParams(card, blockchain, event.interceptParams())

            val eventFilter = analyticsFilters.firstOrNull { it.canBeAppliedTo(event) }
            when {
                eventFilter == null -> analyticsHandlers.forEach { it.send(eventString, eventParams) }
                eventFilter.canBeSent(event) -> {
                    analyticsHandlers
                        .filter { handler -> eventFilter.canBeConsumedByHandler(handler, event) }
                        .forEach { it.send(eventString, eventParams) }
                }
            }
        }
    }

    override fun handleAnalyticsEvent(
        event: AnalyticsEventAnOld,
        params: Map<String, String>,
        card: Card?,
        blockchain: String?,
    ) {
        analyticsScope.launch {
            analyticsHandlers.forEach {
                it.handleAnalyticsEvent(event, params.interceptParams(), card, blockchain)
            }
        }
    }

    override fun send(error: Throwable, params: Map<String, String>) {
        analyticsScope.launch {
            analyticsHandlers.filterIsInstance<ErrorEventLogger>().forEach {
                it.logErrorEvent(error, params)
            }
        }
    }

    override fun send(
        error: TangemSdkError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String>,
        card: Card?,
    ) {
        analyticsScope.launch {
            analyticsHandlers.filterIsInstance<CardSdkErrorEventHandler>().forEach {
                it.send(error, action, params, card)
            }
        }
    }

    override fun send(
        error: BlockchainError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String>,
        card: Card?,
    ) {
        analyticsScope.launch {
            analyticsHandlers.filterIsInstance<BlockchainSdkErrorEventHandler>().forEach {
                it.send(error, action, params, card)
            }
        }
    }

    private fun AnalyticsEvent.interceptParams(): Map<String, String> = params.interceptParams()

    private fun Map<String, String>.interceptParams(): Map<String, String> {
        return this.toMutableMap().apply { interceptParams() }
    }

    private fun MutableMap<String, String>.interceptParams() {
        paramsInterceptors.values.forEach { it.intercept(this) }
    }

    private fun createScope(): CoroutineScope {
        val name = "Analytics"
        val dispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        val exHandler = FeatureCoroutineExceptionHandler.create(name)
        return CoroutineScope(Job() + dispatcher + CoroutineName(name) + exHandler)
    }
}

fun GlobalAnalyticsEventHandler.logWcEvent(event: AnalyticsAnOld.WcAnalyticsEvent) {
    when (event) {
        is AnalyticsAnOld.WcAnalyticsEvent.Action -> {
            handleAnalyticsEvent(
                event = AnalyticsEventAnOld.WC_SUCCESS_RESPONSE,
                params = mapOf(
                    AnalyticsParamAnOld.WALLET_CONNECT_ACTION.param to event.action.name,
                ),
            )
        }
        is AnalyticsAnOld.WcAnalyticsEvent.Error -> {
            val params = mapOf(
                AnalyticsParamAnOld.WALLET_CONNECT_ACTION.param to event.action?.name,
                AnalyticsParamAnOld.ERROR_DESCRIPTION.param to event.error.message,
            ).filterNotNull()
            send(event.error, params)
        }
        is AnalyticsAnOld.WcAnalyticsEvent.InvalidRequest ->
            handleAnalyticsEvent(
                event = AnalyticsEventAnOld.WC_INVALID_REQUEST,
                params = mapOf(
                    AnalyticsParamAnOld.WALLET_CONNECT_REQUEST.param to event.json,
                ).filterNotNull(),
            )
        is AnalyticsAnOld.WcAnalyticsEvent.Session -> {
            val analyticsEvent = when (event.event) {
                AnalyticsAnOld.WcSessionEvent.Disconnect -> AnalyticsEventAnOld.WC_SESSION_DISCONNECTED
                AnalyticsAnOld.WcSessionEvent.Connect -> AnalyticsEventAnOld.WC_NEW_SESSION
            }
            handleAnalyticsEvent(
                event = analyticsEvent,
                params = mapOf(
                    AnalyticsParamAnOld.WALLET_CONNECT_DAPP_URL.param to event.url,
                ).filterNotNull(),
            )
        }
    }
}