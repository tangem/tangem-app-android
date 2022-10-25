package com.tangem.tap.common.analytics

import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.api.BlockchainSdkErrorEventHandler
import com.tangem.tap.common.analytics.api.CardSdkErrorEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventHandler
import com.tangem.tap.common.analytics.api.ErrorEventLogger
import com.tangem.tap.common.analytics.api.SdkErrorEventHandler
import com.tangem.tap.common.analytics.api.ShopifyOrderEventHandler
import com.tangem.tap.common.extensions.filterNotNull

interface GlobalAnalyticsEventHandler : AnalyticsEventHandler,
    ErrorEventHandler,
    SdkErrorEventHandler,
    ShopifyOrderEventHandler

class GlobalAnalyticsHandler(
    private val analyticsHandlers: List<AnalyticsEventHandler>,
) : GlobalAnalyticsEventHandler {

    override fun handleEvent(event: String, params: Map<String, String>) {
        analyticsHandlers.forEach { it.handleEvent(event, params) }
    }

    override fun handleAnalyticsEvent(
        event: AnalyticsEvent,
        params: Map<String, String>,
        card: Card?,
        blockchain: String?,
    ) {
        analyticsHandlers.forEach { it.handleAnalyticsEvent(event, params, card, blockchain) }
    }

    override fun handleErrorEvent(error: Throwable, params: Map<String, String>) {
        analyticsHandlers.filterIsInstance<ErrorEventLogger>().forEach {
            it.logErrorEvent(error, params)
        }
    }

    override fun handleCardSdkErrorEvent(
        error: TangemSdkError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String>,
        card: Card?,
    ) {
        analyticsHandlers.filterIsInstance<CardSdkErrorEventHandler>().forEach {
            it.handleCardSdkErrorEvent(error, action, params, card)
        }
    }

    override fun handleBlockchainSdkErrorEvent(
        error: BlockchainError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String>,
        card: Card?,
    ) {
        analyticsHandlers.filterIsInstance<BlockchainSdkErrorEventHandler>().forEach {
            it.handleBlockchainSdkErrorEvent(error, action, params, card)
        }
    }

    override fun handleShopifyOrderEvent(order: Storefront.Order) {
        analyticsHandlers.filterIsInstance<ShopifyOrderEventHandler>().forEach {
            it.handleShopifyOrderEvent(order)
        }
    }
}
// [REDACTED_TODO_COMMENT]
fun GlobalAnalyticsEventHandler.logWcEvent(event: Analytics.WcAnalyticsEvent) {
    when (event) {
        is Analytics.WcAnalyticsEvent.Action -> {
            handleAnalyticsEvent(
                event = AnalyticsEvent.WC_SUCCESS_RESPONSE,
                params = mapOf(
                    AnalyticsParam.WALLET_CONNECT_ACTION.param to event.action.name,
                ),
            )
        }
        is Analytics.WcAnalyticsEvent.Error -> {
            val params = mapOf(
                AnalyticsParam.WALLET_CONNECT_ACTION.param to event.action?.name,
                AnalyticsParam.ERROR_DESCRIPTION.param to event.error.message,
            ).filterNotNull()
            handleErrorEvent(event.error, params)
        }
        is Analytics.WcAnalyticsEvent.InvalidRequest ->
            handleAnalyticsEvent(
                event = AnalyticsEvent.WC_INVALID_REQUEST,
                params = mapOf(
                    AnalyticsParam.WALLET_CONNECT_REQUEST.param to event.json,
                ).filterNotNull(),
            )
        is Analytics.WcAnalyticsEvent.Session -> {
            val analyticsEvent = when (event.event) {
                Analytics.WcSessionEvent.Disconnect -> AnalyticsEvent.WC_SESSION_DISCONNECTED
                Analytics.WcSessionEvent.Connect -> AnalyticsEvent.WC_NEW_SESSION
            }
            handleAnalyticsEvent(
                event = analyticsEvent,
                params = mapOf(
                    AnalyticsParam.WALLET_CONNECT_DAPP_URL.param to event.url,
                ).filterNotNull(),
            )
        }
    }
}
