package com.tangem.tap.common.analytics.api

import android.app.Application
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.AnalyticsHandlersLogConfig
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import com.tangem.tap.domain.configurable.config.Config

/**
 * Created by Anton Zhilenkov on 23/09/2022.
 */
interface AnalyticsEventHandler {
    fun send(event: AnalyticsEvent)
}

interface AnalyticsHandler : AnalyticsEventHandler {
    fun id(): String

    fun send(event: String, params: Map<String, String> = emptyMap())

    override fun send(event: AnalyticsEvent) {
        send(prepareEventString(event), event.params)
    }

    fun prepareEventString(event: AnalyticsEvent): String = "[${event.category}] ${event.event}"
}

interface ErrorEventHandler {
    fun send(
        error: Throwable,
        params: Map<String, String> = emptyMap(),
    )
}

interface AnalyticsHandlerHolder {
    fun addHandler(name: String, handler: AnalyticsHandler)
    fun removeHandler(name: String): AnalyticsHandler?
}

interface AnalyticsHandlerBuilder {
    fun build(data: Data): AnalyticsHandler?

    data class Data(
        val application: Application,
        val config: Config,
        val isDebug: Boolean,
        val logConfig: AnalyticsHandlersLogConfig,
        val jsonConverter: MoshiJsonConverter,
    )
}
