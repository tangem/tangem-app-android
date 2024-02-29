package com.tangem.tap.features.disclaimer

/**
 * Created by Anton Zhilenkov on 22.12.2022.
 */
interface DisclaimerDataProvider {
    fun getLanguage(): String
    fun getCardId(): String
    suspend fun accept()
    suspend fun isAccepted(): Boolean
}
