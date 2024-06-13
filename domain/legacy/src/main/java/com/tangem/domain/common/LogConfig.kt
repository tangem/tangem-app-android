package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

object LogConfig {
    const val imageLoader: Boolean = false
    val storeAction: Boolean = BuildConfig.LOG_ENABLED
    val network: NetworkLogConfig = NetworkLogConfig
    val analyticsHandlers: AnalyticsHandlersLogConfig = AnalyticsHandlersLogConfig
}

object NetworkLogConfig {
    const val mercuryoService: Boolean = false
    const val moonPayService: Boolean = false
    const val utorgService: Boolean = false
    val tangemTechService: Boolean = BuildConfig.LOG_ENABLED
    val paymentologyApiService: Boolean = BuildConfig.LOG_ENABLED
    val blockchainSdkNetwork: Boolean = BuildConfig.LOG_ENABLED
}

object AnalyticsHandlersLogConfig {
    val firebase: Boolean = BuildConfig.LOG_ENABLED
    val amplitude: Boolean = BuildConfig.LOG_ENABLED
}
