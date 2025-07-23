package com.tangem.features.swap.v2.impl.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trigger amount change from another component.
 * Different from another triggers because it takes raw string instead of BigDecimal
 */
interface SwapAmountUpdateListener {
    val updateAmountTriggerFlow: Flow<String>
}

/**
 * Trigger for reducing swap amount from another component
 */
interface SwapAmountReduceTrigger {
    suspend fun triggerReduceBy(reduceBy: ReduceByData)
    suspend fun triggerReduceTo(reduceTo: BigDecimal)
    suspend fun triggerIgnoreReduce()
}

/**
 * Trigger for reducing swap amount from another component
 */
interface SwapAmountReduceListener {
    val reduceToTriggerFlow: Flow<BigDecimal>
    val reduceByTriggerFlow: Flow<ReduceByData>
    val ignoreReduceTriggerFlow: Flow<Unit>
}

@Singleton
internal class DefaultSwapAmountUpdateTrigger @Inject constructor() :
    SwapAmountUpdateTrigger,
    SwapAmountUpdateListener,
    SwapAmountReduceTrigger,
    SwapAmountReduceListener {

    override val updateAmountTriggerFlow: SharedFlow<String>
    field = MutableSharedFlow<String>()

    override val reduceToTriggerFlow: SharedFlow<BigDecimal>
    field = MutableSharedFlow<BigDecimal>()

    override val reduceByTriggerFlow: SharedFlow<ReduceByData>
    field = MutableSharedFlow<ReduceByData>()

    override val ignoreReduceTriggerFlow: SharedFlow<Unit>
    field = MutableSharedFlow<Unit>()

    override suspend fun triggerUpdateAmount(amountValue: String) {
        updateAmountTriggerFlow.emit(amountValue)
    }

    override suspend fun triggerReduceBy(reduceBy: ReduceByData) {
        reduceByTriggerFlow.emit(reduceBy)
    }

    override suspend fun triggerReduceTo(reduceTo: BigDecimal) {
        reduceToTriggerFlow.emit(reduceTo)
    }

    override suspend fun triggerIgnoreReduce() {
        ignoreReduceTriggerFlow.emit(Unit)
    }
}