package com.tangem.feature.swap.viewmodels

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class PeriodicTask(
    private val delay: Long,
    private val task: suspend () -> Unit,
) {

    private var isActive: AtomicBoolean = AtomicBoolean(false)

    suspend fun runTaskWithDelay() {
        isActive.set(true)
        while (isActive.get()) {
            task.invoke()
            delay(delay)
        }
    }

    fun cancel() {
        isActive.set(false)
    }
}

class SingleTaskScheduler {

    private var lastTask: PeriodicTask? = null

    fun scheduleTask(scope: CoroutineScope, task: PeriodicTask) {
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
