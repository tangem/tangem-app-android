package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

object LogConfig {
    const val imageLoader: Boolean = false
    val storeAction: Boolean = BuildConfig.LOG_ENABLED
    val network: NetworkLogConfig = NetworkLogConfig
    val analyticsHandlers: AnalyticsHandlersLogConfig = AnalyticsHandlersLogConfig
}

object NetworkLogConfig {
    val blockchainSdkNetwork: Boolean = BuildConfig.LOG_ENABLED
}

object AnalyticsHandlersLogConfig {
    val firebase: Boolean = BuildConfig.LOG_ENABLED
    val amplitude: Boolean = false
    val appsflyer: Boolean = BuildConfig.LOG_ENABLED
}