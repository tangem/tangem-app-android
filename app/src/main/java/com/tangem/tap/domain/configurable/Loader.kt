package com.tangem.tap.domain.configurable

import com.tangem.wallet.BuildConfig

/**
[REDACTED_AUTHOR]
 */
interface Loader<T> {
    fun load(onComplete: (T) -> Unit)

    companion object {
        const val featuresName = "features_${BuildConfig.ENVIRONMENT}"
        const val configValuesName = "tangem-app-config/config_${BuildConfig.ENVIRONMENT}"
        const val warnings = "warnings_${BuildConfig.ENVIRONMENT}"
    }
}