package com.tangem.tap.common.analytics.api

import android.app.Application
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.domain.common.AnalyticsHandlersLogConfig
import com.tangem.tap.domain.configurable.config.Config

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
