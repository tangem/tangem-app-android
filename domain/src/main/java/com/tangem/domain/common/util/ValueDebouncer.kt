package com.tangem.domain.common.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
[REDACTED_AUTHOR]
 */
class ValueDebouncer<T>(
    private val initialValue: T,
    private val debounceDuration: Long = 400,
    private val onValueChanged: (T) -> Unit,
    private val onEmitValueReceived: (T) -> Unit = {},
) {

    var emittedValue: T = initialValue
        private set
    var debounced: T = initialValue
        private set

    private val debounceScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Main)
    private val flow = MutableStateFlow(debounced)

    init {
        debounceScope.launch {
            flow.filter { if (debounced == null) true else debounced != it }
                .debounce(debounceDuration)
                .onEach {
                    debounced = it
                    onValueChanged(it)
                }
                .collect()
        }
    }

    fun isDebounced(value: T): Boolean = this.debounced == value

    fun emmit(emmitValue: T) {
        emittedValue = emmitValue
        onEmitValueReceived.invoke(emmitValue)
        debounceScope.launch { flow.emit(emmitValue) }
    }
}