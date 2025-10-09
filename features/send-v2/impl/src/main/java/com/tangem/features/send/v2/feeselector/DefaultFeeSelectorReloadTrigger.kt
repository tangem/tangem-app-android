package com.tangem.features.send.v2.feeselector

import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.feeSelector.entity.FeeSelectorData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultFeeSelectorReloadTrigger @Inject constructor() :
    FeeSelectorReloadTrigger,
    FeeSelectorReloadListener,
    FeeSelectorCheckReloadTrigger,
    FeeSelectorCheckReloadListener {

    override val reloadTriggerFlow: SharedFlow<FeeSelectorData>
        field = MutableSharedFlow()

    override val loadingStateTriggerFlow: SharedFlow<Unit>
        field = MutableSharedFlow()

    override val checkReloadTriggerFlow: SharedFlow<Unit>
        field = MutableSharedFlow()

    override val checkReloadResultFlow: SharedFlow<Boolean>
        field = MutableSharedFlow()

    override suspend fun triggerUpdate(feeData: FeeSelectorData) {
        reloadTriggerFlow.emit(feeData)
    }

    override suspend fun triggerLoadingState() {
        loadingStateTriggerFlow.emit(Unit)
    }

    override suspend fun triggerCheckUpdate() {
        checkReloadTriggerFlow.emit(Unit)
    }

    override suspend fun callbackCheckResult(isSuccess: Boolean) {
        checkReloadResultFlow.emit(isSuccess)
    }
}