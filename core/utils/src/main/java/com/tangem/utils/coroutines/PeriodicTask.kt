package com.tangem.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class PeriodicTask<T>(
    private val delay: Long,
    private val task: suspend () -> Result<T>,
    private val onSuccess: (T) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val initialDelay: Long = 0L,
) {

    private var isActive: AtomicBoolean = AtomicBoolean(false)

    suspend fun runTaskWithDelay() {
        isActive.set(true)
        if (initialDelay > 0L) {
            delay(initialDelay)
        }
        while (isActive.get()) {
            val result = task.invoke()
            // A cancelled run must not report its result. The flag alone is not enough: task bodies that
            // wrap their work in stdlib `runCatching` instead of `runSuspendCatching` swallow the
            // CancellationException and hand back a plain failure, so the coroutine's own state is the
            // authority here.
            currentCoroutineContext().ensureActive()
            result
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

/**
 * Runs at most one [PeriodicTask] at a time.
 *
 * [scheduleTask] and [resumeLastTask] cancel the run they supersede, including the request it is suspended
 * on. Letting it finish would send that request twice, since the superseded result is discarded anyway and
 * the incoming run repeats it ([REDACTED_TASK_KEY]).
 *
 * [cancelTask] only stops the periodic loop and leaves a request already in flight to complete: callers use
 * it to pause (`onStop`, opening a bottom sheet) and rely on that work still landing in their stores. Use
 * [destroyTask] to tear the task down for good.
 *
 * [resumeLastTask] undoes exactly one [cancelTask]. A resume that nothing paused is ignored, so a caller
 * echoing its "not paused" state cannot restart a task that is already polling ([REDACTED_TASK_KEY]).
 */
class SingleTaskScheduler<T> {

    private val taskJobHolder = JobHolder()
    private var lastTask: PeriodicTask<T>? = null
    private var isPaused = false

    fun scheduleTask(scope: CoroutineScope, task: PeriodicTask<T>) {
        lastTask?.cancel()
        lastTask = task
        isPaused = false
        scope.launch {
            task.runTaskWithDelay()
        }.saveIn(taskJobHolder)
    }

    fun cancelTask() {
        lastTask?.cancel()
        isPaused = true
    }

    fun destroyTask() {
        lastTask?.cancel()
        lastTask = null
        isPaused = false
        taskJobHolder.cancel()
    }

    fun resumeLastTask(scope: CoroutineScope) {
        if (!isPaused) return
        val task = lastTask ?: return
        isPaused = false
        scope.launch {
            task.runTaskWithDelay()
        }.saveIn(taskJobHolder)
    }
}