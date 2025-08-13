package com.tangem.features.send.v2.subcomponents.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trigger for reducing amount from another component
 */
interface SendAmountReduceTrigger {
    suspend fun triggerReduceBy(reduceBy: ReduceByData)
    suspend fun triggerReduceTo(reduceTo: BigDecimal)
    suspend fun triggerIgnoreReduce()
}

/**
 * Trigger for reducing amount from another component
 */
interface SendAmountReduceListener {
    val reduceToTriggerFlow: Flow<BigDecimal>
    val reduceByTriggerFlow: Flow<ReduceByData>
    val ignoreReduceTriggerFlow: Flow<Unit>
}

/**
 * Trigger amount change from another component.
 * Different from another triggers because it takes raw string instead of BigDecimal
 */
interface SendAmountUpdateTrigger {
    suspend fun triggerUpdateAmount(amountValue: String)
}

/**
 * Trigger amount change from another component.
 * Different from another triggers because it takes raw string instead of BigDecimal
 */
interface SendAmountUpdateListener {
    val updateAmountTriggerFlow: Flow<String>
}

@Singleton
internal class DefaultSendAmountReduceTrigger @Inject constructor() :
    SendAmountReduceTrigger,
    SendAmountReduceListener,
    SendAmountUpdateTrigger,
    SendAmountUpdateListener {

    override val reduceToTriggerFlow = MutableSharedFlow<BigDecimal>()
    override val reduceByTriggerFlow = MutableSharedFlow<ReduceByData>()
    override val ignoreReduceTriggerFlow = MutableSharedFlow<Unit>()
    override val updateAmountTriggerFlow = MutableSharedFlow<String>()

    override suspend fun triggerReduceBy(reduceBy: ReduceByData) {
        reduceByTriggerFlow.emit(reduceBy)
    }

    override suspend fun triggerReduceTo(reduceTo: BigDecimal) {
        reduceToTriggerFlow.emit(reduceTo)
    }

    override suspend fun triggerIgnoreReduce() {
        ignoreReduceTriggerFlow.emit(Unit)
    }

    override suspend fun triggerUpdateAmount(amountValue: String) {
        updateAmountTriggerFlow.emit(amountValue)
    }
}