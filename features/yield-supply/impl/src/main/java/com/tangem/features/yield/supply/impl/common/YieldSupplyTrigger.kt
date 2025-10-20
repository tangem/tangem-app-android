package com.tangem.features.yield.supply.impl.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trigger for entering/exiting protocol from other components
 */
interface YieldSupplyProtocolTrigger {
    suspend fun onEnterProtocol()
    suspend fun onExitProtocol()
}

/**
 * Listener to observe entering/exiting protocol events
 */
interface YieldSupplyProtocolListener {
    val enterProtocolTriggerFlow: Flow<Unit>
    val exitProtocolTriggerFlow: Flow<Unit>
}

@Singleton
internal class DefaultYieldSupplyProtocolTrigger @Inject constructor() :
    YieldSupplyProtocolTrigger,
    YieldSupplyProtocolListener {

    override val enterProtocolTriggerFlow = MutableSharedFlow<Unit>()
    override val exitProtocolTriggerFlow = MutableSharedFlow<Unit>()

    override suspend fun onEnterProtocol() {
        enterProtocolTriggerFlow.emit(Unit)
    }

    override suspend fun onExitProtocol() {
        exitProtocolTriggerFlow.emit(Unit)
    }
}