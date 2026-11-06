package com.tangem.core.analytics.config

/**
 * Analytics-specific slice of the application environment config, so the analytics implementation does not
 * depend on the full environment config.
 */
data class AnalyticsEnvironmentConfig(
    val amplitudeApiKey: String,
    val amplitudeApiKeyDev: String?,
    val appsFlyerApiKey: String,
    val customerIoCdpApiKey: String?,
)