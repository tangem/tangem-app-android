package com.tangem.domain.common.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach

/**
 * Created by Anton Zhilenkov on 06/04/2022.
 */
@Suppress("MagicNumber")
class ValueDebouncer<T>(
    private val initialValue: T,
    private val debounceDuration: Long = 400,
    private val onValueChanged: (T) -> Unit,
    private val onEmitValueReceived: (T) -> Unit = {},
) {

    var emittedValue: T = initialValue
        private set
    var emitsCountBeforeDebounce: Int = 0
        private set
    var debounced: T = initialValue
        private set

    private val debounceScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.Main)
    private val flow = MutableStateFlow(debounced)

    init {
        debounceScope.launch {
            flow.debounce(debounceDuration)
                .onEach {
                    debounced = it
                    onValueChanged(it)
                    debounceScope.launch {
                        delay(500)
                        emitsCountBeforeDebounce = 0
                    }
                }
                .collect()
        }
    }

    fun isDebounced(value: T): Boolean = this.debounced == value

    fun emmit(emmitValue: T) {
        emitsCountBeforeDebounce++
        emittedValue = emmitValue
        onEmitValueReceived.invoke(emmitValue)
        debounceScope.launch { flow.emit(emmitValue) }
    }
}
