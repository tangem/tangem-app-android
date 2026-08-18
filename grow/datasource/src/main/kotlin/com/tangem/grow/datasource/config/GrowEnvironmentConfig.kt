package com.tangem.grow.datasource.config

/**
 * Grow-specific slice of the application environment config.
 *
 * Holds only the properties owned by the Grow stream (onramp / staking / yield), so the module does not
 * depend on the full environment config.
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
    val express: ExpressModel?,
    val devExpress: ExpressModel?,
) {

    /** Grow-local mirror of the express keys held by the app environment config. */
    data class ExpressModel(val apiKey: String, val signVerifierPublicKey: String)
}