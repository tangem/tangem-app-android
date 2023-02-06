package com.tangem.tap.features.disclaimer

import com.tangem.tap.persistence.DisclaimerPrefStorage

/**
[REDACTED_AUTHOR]
 */
interface DisclaimerDataProvider {
    fun getLanguage(): String
    fun getCardId(): String
    fun storage(): DisclaimerPrefStorage
}