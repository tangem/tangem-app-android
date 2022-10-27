package com.tangem.tap.common.analytics.api

import android.app.Application
import com.shopify.buy3.Storefront
import com.tangem.blockchain.common.BlockchainError
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.AnalyticsHandlersLogConfig
import com.tangem.tap.common.analytics.AnalyticsAnOld
import com.tangem.tap.common.analytics.AnalyticsEventAnOld
import com.tangem.tap.common.analytics.AnalyticsParamAnOld
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.domain.configurable.config.Config

/**
[REDACTED_AUTHOR]
 */
interface AnalyticsEventHandler {
    fun id(): String

    fun send(event: String, params: Map<String, String> = emptyMap())

    fun send(event: AnalyticsEvent, card: Card? = null, blockchain: String? = null) {
        send(
            event = prepareEventString(event.category, event.event),
            params = prepareParams(card, blockchain, event.params),
        )
    }

    @Deprecated("Migrate to AnalyticsEvent")
    fun handleAnalyticsEvent(
        event: AnalyticsEventAnOld,
        params: Map<String, String> = emptyMap(),
        card: Card? = null,
        blockchain: String? = null,
    ) {
        send(event.event, prepareParams(card, blockchain, params))
    }

    fun prepareParams(
        card: Card? = null,
        blockchain: String? = null,
        params: Map<String, String> = emptyMap(),
    ): Map<String, String> = mapOf(
        AnalyticsParam.Firmware to card?.firmwareVersion?.stringValue,
        AnalyticsParam.BatchId to card?.batchId,
        AnalyticsParam.Blockchain to blockchain,
    ).filterNotNull() + params

    fun prepareEventString(category: String, event: String): String {
        return "[$category] $event"
    }
}

interface ErrorEventHandler {
    fun send(
        error: Throwable,
        params: Map<String, String> = emptyMap(),
    )
}

interface SdkErrorEventHandler : CardSdkErrorEventHandler, BlockchainSdkErrorEventHandler

interface CardSdkErrorEventHandler {
    fun send(
        error: TangemSdkError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String> = emptyMap(),
        card: Card? = null,
    )
}

interface BlockchainSdkErrorEventHandler {
    fun send(
        error: BlockchainError,
        action: AnalyticsAnOld.ActionToLog,
        params: Map<AnalyticsParamAnOld, String> = mapOf(),
        card: Card? = null,
    )
}

interface ShopifyOrderEventHandler {
    fun send(order: Storefront.Order)
}

interface AnalyticsHandlerBuilder {
    fun build(data: Data): AnalyticsEventHandler?

    data class Data(
        val application: Application,
        val config: Config,
        val isDebug: Boolean,
        val logConfig: AnalyticsHandlersLogConfig,
        val jsonConverter: MoshiJsonConverter,
    )
}