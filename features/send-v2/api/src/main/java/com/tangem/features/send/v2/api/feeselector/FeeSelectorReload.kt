package com.tangem.features.send.v2.api.feeselector

import kotlinx.coroutines.flow.Flow

data class FeeSelectorReloadData(val removeSuggestedFee: Boolean = false)

interface FeeSelectorReloadTrigger {
    suspend fun triggerUpdate(data: FeeSelectorReloadData = FeeSelectorReloadData())
}

interface FeeSelectorReloadListener {
    val reloadTriggerFlow: Flow<FeeSelectorReloadData>
}