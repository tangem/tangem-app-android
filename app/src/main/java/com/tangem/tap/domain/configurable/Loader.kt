package com.tangem.tap.domain.configurable

import com.tangem.wallet.BuildConfig

/**
 * Created by Anton Zhilenkov on 12/11/2020.
 */
interface Loader<T> {
    fun load(onComplete: (T) -> Unit)

    companion object {
        const val featuresName = "features_${BuildConfig.ENVIRONMENT}"
        const val configValuesName = "tangem-app-config/config_${BuildConfig.ENVIRONMENT}"
        const val warnings = "warnings_${BuildConfig.ENVIRONMENT}"
    }
}
