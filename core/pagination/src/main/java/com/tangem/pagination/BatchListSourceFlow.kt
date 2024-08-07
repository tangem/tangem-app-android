package com.tangem.pagination

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

fun <TKey, TData, TUpdate> BatchListSource<TKey, TData, TUpdate>.toBatchFlow() =
    object : BatchFlow<TKey, TData, TUpdate> {
        override val state: StateFlow<BatchListState<TKey, TData>>
            get() = this@toBatchFlow.state
        override val updateResults: SharedFlow<Pair<TUpdate, BatchUpdateResult<TKey, TData>>>
            get() = this@toBatchFlow.updateResults
    }

interface BatchFlow<TKey, TData, TUpdate> {
    val state: StateFlow<BatchListState<TKey, TData>>
    val updateResults: SharedFlow<Pair<TUpdate, BatchUpdateResult<TKey, TData>>>
}
