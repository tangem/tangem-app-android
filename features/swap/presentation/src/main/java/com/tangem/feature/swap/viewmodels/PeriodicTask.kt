package com.tangem.feature.swap.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class PeriodicTask<T>(
    private val delay: Long,
    private val task: suspend () -> Result<T>,
    private val onSuccess: (T) -> Unit,
    private val onError: (Throwable) -> Unit,
) {

    private var isActive: AtomicBoolean = AtomicBoolean(false)

    suspend fun runTaskWithDelay() {
        isActive.set(true)
        while (isActive.get()) {
            task.invoke()
                .onSuccess {
                    if (!isActive.get()) {
                        return@onSuccess
                    }
                    onSuccess.invoke(it)
                }
                .onFailure {
                    if (!isActive.get()) {
                        return@onFailure
                    }
                    onError.invoke(it)
                }
            delay(delay)
        }
    }

    fun cancel() {
        isActive.set(false)
    }
}

class SingleTaskScheduler<T> {

    private var lastTask: PeriodicTask<T>? = null

    fun scheduleTask(scope: CoroutineScope, task: PeriodicTask<T>) {
        lastTask?.cancel()
        lastTask = task
        scope.launch {
            task.runTaskWithDelay()
        }
    }

    fun cancelTask() {
        lastTask?.cancel()
    }
}
