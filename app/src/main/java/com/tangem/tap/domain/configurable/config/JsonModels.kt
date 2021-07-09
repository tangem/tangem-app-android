package com.tangem.tap.domain.configurable.config

/**
* [REDACTED_AUTHOR]
 */

class FeatureModel(
        val isWalletPayIdEnabled: Boolean,
        val isTopUpEnabled: Boolean,
        val isSendingToPayIdEnabled: Boolean,
        val isCreatingTwinCardsAllowed: Boolean,
)

class ConfigValueModel(
        val coinMarketCapKey: String,
        val moonPayApiKey: String,
        val moonPayApiSecretKey: String,
        val blockchairAuthorizationToken: String?,
        val blockcypherTokens: Set<String>?,
        val infuraProjectId: String?,
)

class ConfigModel(val features: FeatureModel?, val configValues: ConfigValueModel?) {
    companion object {
        fun empty(): ConfigModel = ConfigModel(null, null)
    }
}