package com.tangem.features.swap.v2.impl.amount

import com.tangem.features.swap.v2.api.subcomponents.SwapAmountUpdateTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trigger amount change from another component.
 * Different from another triggers because it takes raw string instead of BigDecimal
 */
interface SwapAmountUpdateListener {
    val updateAmountTriggerFlow: Flow<String>
}

@Singleton
internal class DefaultSwapAmountUpdateTrigger @Inject constructor() :
    SwapAmountUpdateTrigger,
    SwapAmountUpdateListener {

    override val updateAmountTriggerFlow: Flow<String>
    field = MutableSharedFlow<String>()

    override suspend fun triggerUpdateAmount(amountValue: String) {
        updateAmountTriggerFlow.emit(amountValue)
    }
}