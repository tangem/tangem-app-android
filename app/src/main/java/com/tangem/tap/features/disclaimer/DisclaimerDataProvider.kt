package com.tangem.tap.features.disclaimer

import com.tangem.data.source.preferences.storage.DisclaimerPrefStorage

/**
[REDACTED_AUTHOR]
 */
interface DisclaimerDataProvider {
    fun getLanguage(): String
    fun getCardId(): String
    fun storage(): DisclaimerPrefStorage
}