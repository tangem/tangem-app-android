package com.tangem.datasource.local.datastore.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

internal class Trigger(
    private val triggerFlow: MutableStateFlow<Boolean> = MutableStateFlow(value = false),
) : Flow<Unit> {

    override suspend fun collect(collector: FlowCollector<Unit>): Nothing {
        triggerFlow.collect { collector.emit(Unit) }
    }

    fun trigger() {
        triggerFlow.value = !triggerFlow.value
    }
}
