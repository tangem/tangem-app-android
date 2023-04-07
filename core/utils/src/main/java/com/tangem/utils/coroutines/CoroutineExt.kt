package com.tangem.utils.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun <R> runCatching(dispatcher: CoroutineDispatcher, block: suspend () -> R): Result<R> {
    return runCatching {
        withContext(dispatcher) { block() }
    }
}

suspend inline fun ifActive(crossinline block: suspend () -> Unit) = coroutineScope {
    if (isActive) {
        block()
    }
}

class Debouncer {

    private var debounceJob: Job? = null

    fun debounce(waitMs: Long = 300L, coroutineScope: CoroutineScope, destinationFunction: () -> Unit) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(waitMs)
            destinationFunction.invoke()
        }
    }
}