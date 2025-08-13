package com.tangem.features.send.v2.api.subcomponents.feeSelector

import com.tangem.features.send.v2.api.subcomponents.feeSelector.entity.FeeSelectorData
import kotlinx.coroutines.flow.Flow

interface FeeSelectorReloadTrigger {

    /** Trigger fee update */
    suspend fun triggerUpdate(feeData: FeeSelectorData = FeeSelectorData())

    /** Triggers fee loading status. Used in cases where fee loading state is extended due to business logic */
    suspend fun triggerLoadingState()
}

interface FeeSelectorReloadListener {
    /** Flow triggers fee reload */
    val reloadTriggerFlow: Flow<FeeSelectorData>

    /** Triggers fee loading status. Used in cases where fee loading state is extended due to business logic */
    val loadingStateTriggerFlow: Flow<Unit>
}