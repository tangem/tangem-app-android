package com.tangem.features.send.api.subcomponents.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Trigger for reducing amount from another component
 */
interface SendAmountReduceTrigger {
    suspend fun triggerReduceBy(reduceBy: AmountReduceByTransformer.ReduceByData)
    suspend fun triggerReduceTo(reduceTo: BigDecimal)
    suspend fun triggerIgnoreReduce()
}

/**
 * Trigger for reducing amount from another component
 */
interface SendAmountReduceListener {
    val reduceToTriggerFlow: Flow<BigDecimal>
    val reduceByTriggerFlow: Flow<AmountReduceByTransformer.ReduceByData>
    val ignoreReduceTriggerFlow: Flow<Unit>
}

/**
 * Trigger amount change from another component.
 * Different from another triggers because it takes raw string instead of BigDecimal
 */
interface SendAmountUpdateTrigger {
    suspend fun triggerUpdateAmount(amountValue: String, isEnterInFiatSelected: Boolean?)
}

/**
 * Trigger amount change from another component.
 * Different from another triggers because it takes raw string instead of BigDecimal
 */
interface SendAmountUpdateListener {
    val updateAmountTriggerFlow: Flow<Pair<String, Boolean?>>
}