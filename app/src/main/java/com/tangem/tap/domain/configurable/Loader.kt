package com.tangem.tap.domain.configurable

import com.tangem.wallet.BuildConfig

/**
[REDACTED_AUTHOR]
 */
interface Loader<T> {
    fun load(onComplete: (T) -> Unit)

    companion object {
        const val featuresName = "features_${BuildConfig.CONFIG_ENVIRONMENT}"
        const val configValuesName = "config_${BuildConfig.CONFIG_ENVIRONMENT}"
        const val warnings = "warnings_${BuildConfig.CONFIG_ENVIRONMENT}"
    }
}