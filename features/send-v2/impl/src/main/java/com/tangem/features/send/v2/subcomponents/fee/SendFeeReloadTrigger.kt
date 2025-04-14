package com.tangem.features.send.v2.subcomponents.fee

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SendFeeReloadTrigger {

    /** Trigger fee update */
    suspend fun triggerUpdate(feeData: SendFeeData)
}

interface SendFeeReloadListener {
    /** Flow triggers fee reload */
    val reloadTriggerFlow: Flow<SendFeeData>
}

interface SendFeeCheckReloadTrigger {
    /** Trigger return callback with check result */
    suspend fun callbackCheckResult(isSuccess: Boolean)

    /** Trigger fee check reload */
    suspend fun triggerCheckUpdate()
}

interface SendFeeCheckReloadListener {
    /**
     * Flow triggers fee check reload before transaction.
     * Usually after significant time after fee was updated last time
     */
    val checkReloadTriggerFlow: Flow<Unit>

    /** Flow return result of fee check update */
    val checkReloadResultFlow: Flow<Boolean>
}

@Singleton
internal class DefaultSendFeeReloadTrigger @Inject constructor() :
    SendFeeReloadTrigger,
    SendFeeReloadListener,
    SendFeeCheckReloadTrigger,
    SendFeeCheckReloadListener {

    private val _reloadTriggerFlow = MutableSharedFlow<SendFeeData>()
    override val reloadTriggerFlow = _reloadTriggerFlow.asSharedFlow()

    private val _checkReloadTriggerFlow = MutableSharedFlow<Unit>()
    override val checkReloadTriggerFlow = _checkReloadTriggerFlow.asSharedFlow()

    private val _checkReloadResultFlow = MutableSharedFlow<Boolean>()
    override val checkReloadResultFlow = _checkReloadResultFlow.asSharedFlow()

    override suspend fun triggerUpdate(feeData: SendFeeData) {
        _reloadTriggerFlow.emit(feeData)
    }

    override suspend fun triggerCheckUpdate() {
        _checkReloadTriggerFlow.emit(Unit)
    }

    override suspend fun callbackCheckResult(isSuccess: Boolean) {
        _checkReloadResultFlow.emit(isSuccess)
    }
}