package com.tangem.features.send.v2.api.subcomponents.feeSelector

import com.tangem.features.send.v2.api.subcomponents.feeSelector.entity.FeeSelectorData
import kotlinx.coroutines.flow.Flow

interface FeeSelectorReloadTrigger {

    /** Trigger fee update */
    suspend fun triggerUpdate(feeData: FeeSelectorData = FeeSelectorData())
}

interface FeeSelectorReloadListener {
    /** Flow triggers fee reload */
    val reloadTriggerFlow: Flow<FeeSelectorData>
}