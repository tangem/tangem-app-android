package com.tangem.grow.datasource.config

/**
 * Grow-specific slice of the application environment config.
 *
 * Holds only the properties owned by the Grow stream (onramp / staking / yield), so the module does not
 * depend on the full environment config. Complex-typed properties (express, p2p, survey-sparrow rating)
 * are added once their models are relocated out of core:datasource.
 */
data class GrowEnvironmentConfig(
    val moonPayApiKey: String,
    val moonPayApiSecretKey: String,
    val mercuryoWidgetId: String,
    val mercuryoSecret: String,
    val stakeKitApiKey: String?,
    val yieldModuleApiKey: String?,
    val yieldModuleApiKeyDev: String?,
    val gaslessTxApiKey: String?,
    val gaslessTxApiKeyDev: String?,
)