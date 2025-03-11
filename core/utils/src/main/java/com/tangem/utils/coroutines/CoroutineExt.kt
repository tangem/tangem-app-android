package com.tangem.utils.coroutines

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

suspend fun <R> runCatching(dispatcher: CoroutineDispatcher, block: suspend () -> R): Result<R> {
    return runCatching {
        withContext(dispatcher) { block() }
    }
}

suspend fun <R> waitForDelay(delay: Long, block: suspend CoroutineScope.() -> R): R = coroutineScope {
    val minWaitingTime = async { delay(delay) }
    val actionInvoke = async { block() }
    minWaitingTime.await()
    actionInvoke.await()
}

class Debouncer {

    private var debounceJob: Job? = null

    fun debounce(
        coroutineScope: CoroutineScope,
        waitMs: Long = 300L,
        context: CoroutineContext = EmptyCoroutineContext,
        forceUpdate: Boolean = false,
        destinationFunction: suspend () -> Unit,
    ) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch(context) {
            if (!forceUpdate) delay(waitMs)
            destinationFunction.invoke()
        }
    }

    fun release() {
        debounceJob?.cancel()
    }

    companion object {
        const val DEFAULT_WAIT_TIME_MS = 500L
    }
}