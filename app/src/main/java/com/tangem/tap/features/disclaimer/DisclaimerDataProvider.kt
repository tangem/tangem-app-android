package com.tangem.tap.features.disclaimer

/**
[REDACTED_AUTHOR]
 */
interface DisclaimerDataProvider {
    fun getLanguage(): String
    fun getCardId(): String
    suspend fun accept()
    suspend fun isAccepted(): Boolean
}