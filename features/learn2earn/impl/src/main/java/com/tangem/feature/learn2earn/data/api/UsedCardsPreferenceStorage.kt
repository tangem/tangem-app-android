package com.tangem.feature.learn2earn.data.api

/**
 * @author Anton Zhilenkov on 07.06.2023.
 * An interface that encapsulates access to deprecated PreferencesDataSource.usedCardsPrefStorage.
 */
interface UsedCardsPreferenceStorage {
    fun isHadActivatedCards(): Boolean
}
