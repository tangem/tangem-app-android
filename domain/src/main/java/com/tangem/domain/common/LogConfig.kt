package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

object LogConfig {
    const val imageLoader: Boolean = false
    val storeAction: Boolean = BuildConfig.DEBUG
    const val zendesk: Boolean = false
    val network: NetworkLogConfig = NetworkLogConfig
    val analyticsHandlers: AnalyticsHandlersLogConfig = AnalyticsHandlersLogConfig
}

object NetworkLogConfig {
    const val mercuryoService: Boolean = false
    const val moonPayService: Boolean = false
    val blockchainSdkNetwork: Boolean = BuildConfig.DEBUG
}

object AnalyticsHandlersLogConfig {
    const val firebase: Boolean = false
    const val appsFlyer: Boolean = false
    const val amplitude: Boolean = false
}
