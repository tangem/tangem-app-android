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
}

/**
 * Trigger for reducing amount from another component
 */
interface SendAmountReduceListener {
    val reduceToTriggerFlow: Flow<BigDecimal>
    val reduceByTriggerFlow: Flow<ReduceByData>
}

@Singleton
internal class DefaultSendAmountReduceTrigger @Inject constructor() :
    SendAmountReduceTrigger,
    SendAmountReduceListener {
    override val reduceToTriggerFlow = MutableSharedFlow<BigDecimal>()
    override val reduceByTriggerFlow = MutableSharedFlow<ReduceByData>()

    override suspend fun triggerReduceBy(reduceBy: ReduceByData) {
        reduceByTriggerFlow.emit(reduceBy)
    }

    override suspend fun triggerReduceTo(reduceTo: BigDecimal) {
        reduceToTriggerFlow.emit(reduceTo)
    }
}