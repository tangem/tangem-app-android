package com.tangem.tap.common.analytics

import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.BlockchainSdkErrorEventHandler
import com.tangem.tap.common.analytics.api.CardSdkErrorEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventLogger
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import com.tangem.tap.common.extensions.filterNotNull

/**
* [REDACTED_AUTHOR]
 */
object Analytics : GlobalAnalyticsEventHandler {

    private val handlers = mutableMapOf<String, AnalyticsEventHandler>()

    private val analyticsHandlers: List<AnalyticsEventHandler>
        get() = handlers.values.toList()

    private val attachToAllEventsParams: MutableMap<String, String> = mutableMapOf()

    override fun id(): String = analyticsHandlers.joinToString(", ") { it.id() }

    override fun addHandler(name: String, handler: AnalyticsEventHandler) {
        handlers[name] = handler
    }

    override fun removeHandler(name: String): AnalyticsEventHandler? {
        return handlers.remove(name)
    }

    override fun attachToAllEvents(key: String, value: String) {
        attachToAllEventsParams[key] = value
    }

    override fun send(event: String, params: Map<String, String>) {
        analyticsHandlers.forEach { it.send(event, params) }
    }

    override fun send(event: AnalyticsEvent, card: Card?, blockchain: String?) {
        analyticsHandlers.forEach { it.send(event, card, blockchain) }
    }

    override fun handleAnalyticsEvent(
        event: AnalyticsEventAnOld,
        params: Map<String, String>,
        card: Card?,
        blockchain: String?,
    ) {
        analyticsHandlers.forEach {
            it.handleAnalyticsEvent(event, params, card, blockchain)
        }
    }

    override fun send(error: Throwable, params: Map<String, String>) {
        analyticsHandlers.filterIsInstance<ErrorEventLogger>().forEach {
            it.logErrorEvent(error, params)
        }
    }

    override fun send(
        error: TangemSdkError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String>,
        card: Card?,
    ) {
        analyticsHandlers.filterIsInstance<CardSdkErrorEventHandler>().forEach {
            it.send(error, action, params, card)
        }
    }

    override fun send(
        error: BlockchainError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String>,
        card: Card?,
    ) {
        analyticsHandlers.filterIsInstance<BlockchainSdkErrorEventHandler>().forEach {
            it.send(error, action, params, card)
        }
    }

    override fun send(order: Storefront.Order) {
        analyticsHandlers.filterIsInstance<ShopifyOrderEventHandler>().forEach {
            it.send(order)
        }
    }

    override fun prepareParams(card: Card?, blockchain: String?, params: Map<String, String>): Map<String, String> {
        return super.prepareParams(card, blockchain, params).toMutableMap().apply {
            putAll(attachToAllEventsParams)
        }
    }
}
// [REDACTED_TODO_COMMENT]
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
