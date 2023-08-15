package com.tangem.datasource.local.datastore.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Represents a trigger mechanism to emit values on-demand.
 *
 * This class provides a mechanism to trigger emissions via the [trigger] method.
 *
 * @property triggerFlow The internal flow that gets toggled to trigger emissions.
 */
internal class Trigger(
    private val triggerFlow: MutableStateFlow<Boolean> = MutableStateFlow(value = false),
) : Flow<Unit> {

    /**
     * Collects values emitted by this flow.
     *
     * Overrides the default collection mechanism to emit a [Unit] value whenever [triggerFlow] changes.
     *
     * @param collector The collector responsible for handling emitted values.
     */
    override suspend fun collect(collector: FlowCollector<Unit>): Nothing {
        triggerFlow.collect { collector.emit(Unit) }
    }

    /**
     * Triggers an emission.
     */
    fun trigger() {
        triggerFlow.value = !triggerFlow.value
    }
}
