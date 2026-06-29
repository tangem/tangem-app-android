package com.tangem.features.send.subcomponents.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer.ReduceByData
import com.tangem.features.send.api.subcomponents.amount.SendAmountReduceListener
import com.tangem.features.send.api.subcomponents.amount.SendAmountReduceTrigger
import com.tangem.features.send.api.subcomponents.amount.SendAmountUpdateListener
import com.tangem.features.send.api.subcomponents.amount.SendAmountUpdateTrigger
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSendAmountReduceTrigger @Inject constructor() :
    SendAmountReduceTrigger,
    SendAmountReduceListener,
    SendAmountUpdateTrigger,
    SendAmountUpdateListener {

    override val reduceToTriggerFlow = MutableSharedFlow<BigDecimal>()
    override val reduceByTriggerFlow = MutableSharedFlow<ReduceByData>()
    override val ignoreReduceTriggerFlow = MutableSharedFlow<Unit>()
    override val updateAmountTriggerFlow = MutableSharedFlow<Pair<String, Boolean?>>()

    override suspend fun triggerReduceBy(reduceBy: ReduceByData) {
        reduceByTriggerFlow.emit(reduceBy)
    }

    override suspend fun triggerReduceTo(reduceTo: BigDecimal) {
        reduceToTriggerFlow.emit(reduceTo)
    }

    override suspend fun triggerIgnoreReduce() {
        ignoreReduceTriggerFlow.emit(Unit)
    }

    override suspend fun triggerUpdateAmount(amountValue: String, isEnterInFiatSelected: Boolean?) {
        updateAmountTriggerFlow.emit(amountValue to isEnterInFiatSelected)
    }
}