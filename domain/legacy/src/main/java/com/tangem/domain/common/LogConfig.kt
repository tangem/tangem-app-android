package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

object LogConfig {
    const val imageLoader: Boolean = false
    val shouldStoreAction: Boolean = BuildConfig.LOG_ENABLED
    val network: NetworkLogConfig = NetworkLogConfig
    val analyticsHandlers: AnalyticsHandlersLogConfig = AnalyticsHandlersLogConfig
}

object NetworkLogConfig {
    val isBlockchainSdkNetworkLogEnabled: Boolean = BuildConfig.LOG_ENABLED
}

object AnalyticsHandlersLogConfig {
    val isFirebaseLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isAmplitudeLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isAppsflyerLogEnabled: Boolean = BuildConfig.LOG_ENABLED
}