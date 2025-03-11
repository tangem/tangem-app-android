package com.tangem.tap.common.analytics.api

import android.app.Application
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.common.AnalyticsHandlersLogConfig

interface AnalyticsHandlerBuilder {
    fun build(data: Data): AnalyticsHandler?

    data class Data(
        val application: Application,
        val config: EnvironmentConfig,
        val isDebug: Boolean,
        val logConfig: AnalyticsHandlersLogConfig,
        val jsonConverter: MoshiJsonConverter,
    )
}