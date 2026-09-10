package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

object LogConfig {
    val shouldStoreAction: Boolean = BuildConfig.LOG_ENABLED
    val analyticsHandlers: AnalyticsHandlersLogConfig = AnalyticsHandlersLogConfig
}

object AnalyticsHandlersLogConfig {
    val isFirebaseLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isAppsflyerLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isCustomerIoLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isOpenTelemetryLogEnabled: Boolean = BuildConfig.LOG_ENABLED
}