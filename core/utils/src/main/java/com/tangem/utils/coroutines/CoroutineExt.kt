package com.tangem.utils.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

suspend inline fun <T, R> T.runSuspendCatching(block: suspend T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
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

fun CoroutineScope.launchOnCancellation(block: suspend () -> Unit) {
    launch {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                block()
            }
        }
    }
}

@Suppress("LongParameterList", "MagicNumber")
inline fun <T1, T2, T3, T4, T5, T6, T7, R> combine7(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = combine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { arr ->
    @Suppress("UNCHECKED_CAST")
    transform(
        arr[0] as T1,
        arr[1] as T2,
        arr[2] as T3,
        arr[3] as T4,
        arr[4] as T5,
        arr[5] as T6,
        arr[6] as T7,
    )
}