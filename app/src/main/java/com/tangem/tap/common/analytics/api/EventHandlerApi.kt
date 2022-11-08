package com.tangem.tap.common.analytics.api

import android.app.Application
import com.tangem.common.card.Card
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.AnalyticsHandlersLogConfig
import com.tangem.tap.common.analytics.AnalyticsEventAnOld
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.extensions.filterNotNull
import com.tangem.tap.domain.configurable.config.Config

/**
[REDACTED_AUTHOR]
 */
interface AnalyticsEventHandler {
    fun id(): String

    fun send(event: String, params: Map<String, String> = emptyMap())

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
        AnalyticsParam.Blockchain to blockchain,
    ).filterNotNull() + params

    fun prepareEventString(category: String, event: String): String {
        return "[$category] $event"
    }
}

interface AnalyticsHandlerHolder {
    fun addHandler(name: String, handler: AnalyticsEventHandler)
    fun removeHandler(name: String): AnalyticsEventHandler?
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