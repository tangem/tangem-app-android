package com.tangem.tap.common.analytics

import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.extensions.filterNotNull

interface EventHandler {
    fun handleEvent(
        event: String,
        params: Map<String, String> = emptyMap(),
    )
}

interface AnalyticsEventHandler : EventHandler {
    fun handleAnalyticsEvent(
        event: AnalyticsEvent,
        params: Map<String, String> = emptyMap(),
        card: Card? = null,
        blockchain: String? = null,
    )

    fun prepareParams(
        card: Card?,
        blockchain: String? = null,
        params: Map<String, String> = emptyMap(),
    ): Map<String, String> {
// [REDACTED_TODO_COMMENT]
        return mapOf(
            AnalyticsParam.FIRMWARE.param to card?.firmwareVersion?.stringValue,
            AnalyticsParam.BATCH_ID.param to card?.batchId,
            AnalyticsParam.BLOCKCHAIN.param to blockchain,
        ).filterNotNull() + params
    }
}

interface ShopifyOrderEventHandler {
    fun handleShopifyOrderEvent(order: Storefront.Order)
}

interface ErrorEventHandler {
    fun handleErrorEvent(
        error: Throwable,
        params: Map<String, String> = emptyMap(),
    )
}

interface SdkErrorEventHandler : CardSdkErrorEventHandler, BlockchainSdkErrorEventHandler

interface CardSdkErrorEventHandler {
    fun handleCardSdkErrorEvent(
        error: TangemSdkError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String> = emptyMap(),
        card: Card? = null,
    )
}

interface BlockchainSdkErrorEventHandler {
    fun handleBlockchainSdkErrorEvent(
        error: BlockchainError,
        action: Analytics.ActionToLog,
        params: Map<AnalyticsParam, String> = mapOf(),
        card: Card? = null,
    )
}
