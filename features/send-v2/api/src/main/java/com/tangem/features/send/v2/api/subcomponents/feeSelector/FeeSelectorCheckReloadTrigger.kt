package com.tangem.features.send.v2.api.subcomponents.feeSelector

import kotlinx.coroutines.flow.Flow

interface FeeSelectorCheckReloadTrigger {
    /** Trigger return callback with check result */
    suspend fun callbackCheckResult(isSuccess: Boolean)

    /** Trigger fee check reload */
    suspend fun triggerCheckUpdate()
}

interface FeeSelectorCheckReloadListener {
    /**
     * Flow triggers fee check reload before transaction.
     * Usually after significant time after fee was updated last time
     */
    val checkReloadTriggerFlow: Flow<Unit>

    /** Flow return result of fee check update */
    val checkReloadResultFlow: Flow<Boolean>
}