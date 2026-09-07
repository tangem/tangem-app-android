package com.tangem.domain.common

import com.tangem.domain.features.BuildConfig

object LogConfig {

    /**
     * Verbose Coil tracing: every image request plus its response body in Logcat. Off by default
     * even in debuggable builds — image bytes at `Level.BODY` flood Logcat and the logging client
     * replaces Coil's own, dropping its disk cache. Flip it by hand while debugging image loading.
     *
     * Failed image requests are reported regardless of this flag, see `CoilErrorEventListener`.
     */
    const val imageLoader: Boolean = false
    val shouldStoreAction: Boolean = BuildConfig.LOG_ENABLED
    val analyticsHandlers: AnalyticsHandlersLogConfig = AnalyticsHandlersLogConfig
}

object AnalyticsHandlersLogConfig {
    val isFirebaseLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isAppsflyerLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isCustomerIoLogEnabled: Boolean = BuildConfig.LOG_ENABLED
    val isOpenTelemetryLogEnabled: Boolean = BuildConfig.LOG_ENABLED
}