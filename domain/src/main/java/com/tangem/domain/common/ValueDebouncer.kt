package com.tangem.domain.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class ValueDebouncer<T>(
    var value: T?,
    private val debounce: Long = 400,
    private val onValueChanged: (T?) -> Unit
) {

    private val debounceScope = CoroutineScope(Job() + Dispatchers.Main)
    private val flow = MutableStateFlow(value)

    init {
        initFlow()
    }

    private fun initFlow() {
        debounceScope.launch {
            flow.filter { if (value == null) true else value != it }
                .debounce(debounce)
                .onEach {
                    Timber.d("onValueChanged: $it")
                    onValueChanged(it)
                }
                .collect()
        }
    }

    fun emmit(emmitValue: T) {
        debounceScope.launch { flow.emit(emmitValue) }
    }
}