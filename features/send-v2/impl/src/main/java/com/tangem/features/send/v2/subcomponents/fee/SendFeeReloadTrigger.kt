package com.tangem.features.send.v2.subcomponents.fee

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SendFeeReloadTrigger {

    /** Flow triggers fee reload */
    val reloadTriggerFlow: Flow<Unit>

    /** Trigger fee update */
    suspend fun triggerUpdate()
}

interface SendFeeCheckReloadTrigger {

    /**
     * Flow triggers fee check reload before transaction.
     * Usually after significant time after fee was updated last time
     */
    val checkReloadTriggerFlow: Flow<Unit>

    /** Flow return result of fee check update */
    val checkReloadResultFlow: Flow<Boolean>

    /** Trigger return callback with check result */
    suspend fun callbackCheckResult(isSuccess: Boolean)

    /** Trigger fee check reload */
    suspend fun triggerCheckUpdate()
}

@Singleton
internal class DefaultSendFeeReloadTrigger @Inject constructor() : SendFeeReloadTrigger, SendFeeCheckReloadTrigger {

    private val _reloadTriggerFlow = MutableSharedFlow<Unit>()
    override val reloadTriggerFlow = _reloadTriggerFlow.asSharedFlow()

    private val _checkReloadTriggerFlow = MutableSharedFlow<Unit>()
    override val checkReloadTriggerFlow = _checkReloadTriggerFlow.asSharedFlow()

    private val _checkReloadResultFlow = MutableSharedFlow<Boolean>()
    override val checkReloadResultFlow = _checkReloadResultFlow.asSharedFlow()

    override suspend fun triggerUpdate() {
        _reloadTriggerFlow.emit(Unit)
    }

    override suspend fun triggerCheckUpdate() {
        _checkReloadTriggerFlow.emit(Unit)
    }

    override suspend fun callbackCheckResult(isSuccess: Boolean) {
        _checkReloadResultFlow.emit(isSuccess)
    }
}