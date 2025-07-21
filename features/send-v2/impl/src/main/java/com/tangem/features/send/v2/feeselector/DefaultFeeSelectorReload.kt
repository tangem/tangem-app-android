package com.tangem.features.send.v2.feeselector

import com.tangem.features.send.v2.api.feeselector.FeeSelectorReloadData
import com.tangem.features.send.v2.api.feeselector.FeeSelectorReloadListener
import com.tangem.features.send.v2.api.feeselector.FeeSelectorReloadTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultFeeSelectorReload @Inject constructor() : FeeSelectorReloadListener, FeeSelectorReloadTrigger {
    private val _reloadTriggerFlow = MutableSharedFlow<FeeSelectorReloadData>()
    override val reloadTriggerFlow: Flow<FeeSelectorReloadData> = _reloadTriggerFlow.asSharedFlow()

    override suspend fun triggerUpdate(data: FeeSelectorReloadData) {
        _reloadTriggerFlow.emit(data)
    }
}