package com.tangem.feature.learn2earn.data

import com.tangem.data.source.preferences.storage.UsedCardsPrefStorage
import com.tangem.feature.learn2earn.data.api.UsedCardsPreferenceStorage

/**
 * @author Anton Zhilenkov on 07.06.2023.
 */
class DefaultUsedCardsPreferenceStorage(
    private val usedCardsPrefStorage: UsedCardsPrefStorage,
) : UsedCardsPreferenceStorage {

    override fun isHadActivatedCards(): Boolean = usedCardsPrefStorage.hadFinishedActivation()
}
