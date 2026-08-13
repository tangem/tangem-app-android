package com.tangem.store.datasource.config

/**
 * Store-specific slice of the application environment config.
 *
 * Holds only the properties owned by the Store stream (analytics / attribution / marketing), so the
 * module does not depend on the full environment config.
 */
data class StoreEnvironmentConfig(
    val amplitudeApiKey: String,
    val amplitudeApiKeyDev: String?,
    val appsFlyerApiKey: String,
    val appsAppId: String,
    val customerIoCdpApiKey: String?,
    val surveySparrowToken: String?,
)