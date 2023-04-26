package com.tangem.tap.features.disclaimer

import com.tangem.store.preferences.storage.DisclaimerPrefStorage

/**
 * Created by Anton Zhilenkov on 22.12.2022.
 */
interface DisclaimerDataProvider {
    fun getLanguage(): String
    fun getCardId(): String
    fun storage(): DisclaimerPrefStorage
}
