package com.tangem.spend.datasource.config

/**
 * TangemPay-specific slice of the application environment config.
 *
 * Holds only the fields the spend datasource actually needs, so the module does not depend on the full
 * environment config (which pulls the blockchain SDK and grows on almost every new integration).
 */
data class TangemPayEnvironmentConfig(
    val bffStaticToken: String?,
    val bffStaticTokenDev: String?,
)