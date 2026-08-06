package com.tangem.utils.coroutines

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodicTaskTest {

    @Test
    fun `GIVEN initialDelay 0 and delay 1000 WHEN runTaskWithDelay THEN task is invoked immediately`() = runTest {
        val callCount = AtomicInteger(0)
        val onSuccess = mockk<(Int) -> Unit>(relaxed = true)
        val onError = mockk<(Throwable) -> Unit>(relaxed = true)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = onSuccess,
            onError = onError,
            initialDelay = 0L,
        )

        launch { periodicTask.runTaskWithDelay() }
        runCurrent()

        assertThat(callCount.get()).isEqualTo(1)
        periodicTask.cancel()
    }

    @Test
    fun `GIVEN initialDelay 1000 WHEN runTaskWithDelay THEN task is not invoked before initialDelay elapses`() =
        runTest {
            val callCount = AtomicInteger(0)
            val periodicTask = PeriodicTask(
                delay = PERIOD,
                task = { callCount.incrementAndGet(); Result.success(VALUE) },
                onSuccess = mockk(relaxed = true),
                onError = mockk(relaxed = true),
                initialDelay = INITIAL_DELAY,
            )

            launch { periodicTask.runTaskWithDelay() }
            advanceTimeBy(INITIAL_DELAY - 1)

            assertThat(callCount.get()).isEqualTo(0)
            periodicTask.cancel()
        }

    @Test
    fun `GIVEN initialDelay 1000 WHEN runTaskWithDelay THEN task is invoked once initialDelay elapses`() = runTest {
        val callCount = AtomicInteger(0)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = INITIAL_DELAY,
        )

        launch { periodicTask.runTaskWithDelay() }
        advanceTimeBy(INITIAL_DELAY)
        runCurrent()

        assertThat(callCount.get()).isEqualTo(1)
        periodicTask.cancel()
    }

    @Test
    fun `GIVEN periodic task WHEN runTaskWithDelay THEN task is invoked repeatedly every delay ms`() = runTest {
        val callCount = AtomicInteger(0)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )

        launch { periodicTask.runTaskWithDelay() }
        runCurrent()
        advanceTimeBy(PERIOD * 3)
        runCurrent()

        assertThat(callCount.get()).isEqualTo(4)
        periodicTask.cancel()
    }

    @Test
    fun `GIVEN successful task WHEN runTaskWithDelay THEN onSuccess is called with the result value`() = runTest {
        val received = AtomicInteger(-1)
        val errors = AtomicInteger(0)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { Result.success(VALUE) },
            onSuccess = { received.set(it) },
            onError = { errors.incrementAndGet() },
            initialDelay = 0L,
        )

        launch { periodicTask.runTaskWithDelay() }
        runCurrent()

        assertThat(received.get()).isEqualTo(VALUE)
        assertThat(errors.get()).isEqualTo(0)
        periodicTask.cancel()
    }

    @Test
    fun `GIVEN failing task WHEN runTaskWithDelay THEN onError is called with the thrown exception`() = runTest {
        val boom = IllegalStateException("boom")
        val captured = arrayOfNulls<Throwable>(1)
        val successes = AtomicInteger(0)
        val periodicTask = PeriodicTask<Int>(
            delay = PERIOD,
            task = { Result.failure(boom) },
            onSuccess = { successes.incrementAndGet() },
            onError = { captured[0] = it },
            initialDelay = 0L,
        )

        launch { periodicTask.runTaskWithDelay() }
        runCurrent()

        assertThat(captured[0]).isSameInstanceAs(boom)
        assertThat(successes.get()).isEqualTo(0)
        periodicTask.cancel()
    }

    @Test
    fun `GIVEN task running WHEN cancel THEN no further invocations happen`() = runTest {
        val callCount = AtomicInteger(0)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )

        launch { periodicTask.runTaskWithDelay() }
        runCurrent()
        assertThat(callCount.get()).isEqualTo(1)

        periodicTask.cancel()
        advanceUntilIdle()

        assertThat(callCount.get()).isEqualTo(1)
    }

    @Test
    fun `GIVEN initialDelay 1000 and cancel before it elapses WHEN runTaskWithDelay THEN task is never invoked`() =
        runTest {
            val callCount = AtomicInteger(0)
            val periodicTask = PeriodicTask(
                delay = PERIOD,
                task = { callCount.incrementAndGet(); Result.success(VALUE) },
                onSuccess = mockk(relaxed = true),
                onError = mockk(relaxed = true),
                initialDelay = INITIAL_DELAY,
            )

            launch { periodicTask.runTaskWithDelay() }
            advanceTimeBy(INITIAL_DELAY / 2)
            periodicTask.cancel()
            advanceUntilIdle()

            assertThat(callCount.get()).isEqualTo(0)
        }

    @Test
    fun `GIVEN task cancelled during invocation WHEN runTaskWithDelay THEN onSuccess is not called for the pending result`() =
        runTest {
            val onSuccess = mockk<(Int) -> Unit>(relaxed = true)
            val periodicTask = PeriodicTask(
                delay = PERIOD,
                // Simulates a slow task that completes after the scheduler was cancelled.
                task = {
                    delay(PERIOD)
                    Result.success(VALUE)
                },
                onSuccess = onSuccess,
                onError = mockk(relaxed = true),
                initialDelay = 0L,
            )

            launch { periodicTask.runTaskWithDelay() }
            runCurrent()
            periodicTask.cancel()
            advanceUntilIdle()

            verify(exactly = 0) { onSuccess.invoke(any()) }
        }

    @Test
    fun `GIVEN request in flight WHEN scheduleTask supersedes it THEN the superseded request is aborted`() = runTest {
        // Arrange
        val started = AtomicInteger(0)
        val completed = AtomicInteger(0)
        val scheduler = SingleTaskScheduler<Int>()
        fun requestTask() = PeriodicTask(
            delay = PERIOD,
            task = {
                started.incrementAndGet()
                delay(REQUEST_DURATION) // emulates an HTTP round trip
                completed.incrementAndGet()
                Result.success(VALUE)
            },
            onSuccess = mockk<(Int) -> Unit>(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )

        // Act — a re-trigger arrives while the first request is still in flight
        scheduler.scheduleTask(backgroundScope, requestTask())
        runCurrent()
        scheduler.scheduleTask(backgroundScope, requestTask())
        advanceTimeBy(REQUEST_DURATION + 1)
        runCurrent()

        // Assert — both runs started, but only the surviving one reached the network
        assertThat(started.get()).isEqualTo(2)
        assertThat(completed.get()).isEqualTo(1)
        scheduler.destroyTask()
    }

    @Test
    fun `GIVEN request in flight WHEN cancelTask THEN the request is left to complete`() = runTest {
        // Arrange — pausing must not abort work callers rely on landing in their stores; only the periodic
        // loop stops. Cancelling it here would strand features that pause without ever resuming.
        val completed = AtomicInteger(0)
        val onSuccess = mockk<(Int) -> Unit>(relaxed = true)
        val scheduler = SingleTaskScheduler<Int>()
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = {
                delay(REQUEST_DURATION)
                completed.incrementAndGet()
                Result.success(VALUE)
            },
            onSuccess = onSuccess,
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )

        // Act
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        scheduler.cancelTask()
        advanceTimeBy(REQUEST_DURATION + 1)
        runCurrent()

        // Assert — the request completed, but its discarded result was not reported
        assertThat(completed.get()).isEqualTo(1)
        verify(exactly = 0) { onSuccess.invoke(any()) }
        scheduler.destroyTask()
    }

    @Test
    fun `GIVEN request in flight WHEN destroyTask THEN the request is aborted`() = runTest {
        // Arrange
        val completed = AtomicInteger(0)
        val scheduler = SingleTaskScheduler<Int>()
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = {
                delay(REQUEST_DURATION)
                completed.incrementAndGet()
                Result.success(VALUE)
            },
            onSuccess = mockk<(Int) -> Unit>(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )

        // Act
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        scheduler.destroyTask()
        advanceTimeBy(REQUEST_DURATION + 1)
        runCurrent()

        // Assert
        assertThat(completed.get()).isEqualTo(0)
    }

    @Test
    fun `GIVEN request in flight WHEN resumeLastTask THEN the superseded request is aborted`() = runTest {
        // Arrange
        val started = AtomicInteger(0)
        val completed = AtomicInteger(0)
        val scheduler = SingleTaskScheduler<Int>()
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = {
                started.incrementAndGet()
                delay(REQUEST_DURATION)
                completed.incrementAndGet()
                Result.success(VALUE)
            },
            onSuccess = mockk<(Int) -> Unit>(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )

        // Act — pause and resume while the request is still in flight
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        scheduler.cancelTask()
        scheduler.resumeLastTask(backgroundScope)
        advanceTimeBy(REQUEST_DURATION + 1)
        runCurrent()

        // Assert — the resumed run replaces the paused one instead of polling alongside it
        assertThat(started.get()).isEqualTo(2)
        assertThat(completed.get()).isEqualTo(1)
        scheduler.destroyTask()
    }

    @Test
    fun `GIVEN task never paused WHEN resumeLastTask THEN the running task is not restarted`() = runTest {
        // Arrange — the fee-selector block reports "bottom sheet hidden" the moment it subscribes, so a
        // resume can arrive while the first request is still in flight. Restarting there re-sends the whole
        // batch a few seconds after it was sent ([REDACTED_TASK_KEY]).
        val started = AtomicInteger(0)
        val scheduler = SingleTaskScheduler<Int>()
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = {
                started.incrementAndGet()
                delay(REQUEST_DURATION) // emulates an HTTP round trip
                Result.success(VALUE)
            },
            onSuccess = mockk<(Int) -> Unit>(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )

        // Act — resume without a preceding pause
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        scheduler.resumeLastTask(backgroundScope)
        advanceTimeBy(REQUEST_DURATION + 1)
        runCurrent()

        // Assert — the already running task keeps its own cadence, the spurious resume adds no run
        assertThat(started.get()).isEqualTo(1)
        scheduler.destroyTask()
    }

    @Test
    fun `GIVEN task already resumed WHEN resumeLastTask again THEN the task is not restarted`() = runTest {
        // Arrange
        val callCount = AtomicInteger(0)
        val scheduler = SingleTaskScheduler<Int>()
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        scheduler.cancelTask()
        advanceUntilIdle()

        // Act — one pause must be undone by one resume, however many resumes arrive
        scheduler.resumeLastTask(backgroundScope)
        runCurrent()
        val countAfterResume = callCount.get()
        scheduler.resumeLastTask(backgroundScope)
        runCurrent()

        // Assert
        assertThat(callCount.get()).isEqualTo(countAfterResume)
        scheduler.destroyTask()
    }

    @Test
    fun `GIVEN paused task rescheduled WHEN resumeLastTask THEN the scheduled task is not restarted`() = runTest {
        // Arrange — a fresh scheduleTask supersedes the pause, so a later resume has nothing to undo.
        val callCount = AtomicInteger(0)
        val scheduler = SingleTaskScheduler<Int>()
        fun countingTask() = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk<(Int) -> Unit>(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )
        scheduler.scheduleTask(backgroundScope, countingTask())
        runCurrent()
        scheduler.cancelTask()
        scheduler.scheduleTask(backgroundScope, countingTask())
        runCurrent()
        val countAfterReschedule = callCount.get()

        // Act
        scheduler.resumeLastTask(backgroundScope)
        runCurrent()

        // Assert
        assertThat(callCount.get()).isEqualTo(countAfterReschedule)
        scheduler.destroyTask()
    }

    @Test
    fun `GIVEN task body swallowing cancellation WHEN its coroutine is cancelled THEN callbacks are not invoked`() =
        runTest {
            // Arrange — the owning scope dies (model destroyed) while a request is in flight, so the task
            // flag stays set and only the coroutine state tells the run is over.
            val onSuccess = mockk<(Int) -> Unit>(relaxed = true)
            val onError = mockk<(Throwable) -> Unit>(relaxed = true)
            val periodicTask = PeriodicTask(
                delay = PERIOD,
                // `runCatching` turns the CancellationException into a plain failure, as several models do.
                task = {
                    runCatching {
                        delay(REQUEST_DURATION)
                        VALUE
                    }
                },
                onSuccess = onSuccess,
                onError = onError,
                initialDelay = 0L,
            )

            // Act
            val job = backgroundScope.launch { periodicTask.runTaskWithDelay() }
            runCurrent()
            job.cancel()
            runCurrent()

            // Assert
            verify(exactly = 0) { onSuccess.invoke(any()) }
            verify(exactly = 0) { onError.invoke(any()) }
        }

    @Test
    fun `GIVEN no task scheduled WHEN resumeLastTask THEN no crash and no invocations`() = runTest {
        val scheduler = SingleTaskScheduler<Int>()

        scheduler.resumeLastTask(backgroundScope)
        advanceUntilIdle()
        // No assertion needed beyond not crashing — lastTask is null, the safe-call is a no-op.
    }

    @Test
    fun `GIVEN scheduled task cancelled WHEN resumeLastTask THEN task resumes and is invoked again`() = runTest {
        val callCount = AtomicInteger(0)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )
        val scheduler = SingleTaskScheduler<Int>()
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        assertThat(callCount.get()).isEqualTo(1)
        scheduler.cancelTask()
        advanceUntilIdle()
        val countAtPause = callCount.get()

        scheduler.resumeLastTask(backgroundScope)
        runCurrent()

        assertThat(callCount.get()).isEqualTo(countAtPause + 1)
        scheduler.cancelTask()
    }

    @Test
    fun `GIVEN resumed task WHEN delay elapses THEN task continues ticking periodically`() = runTest {
        val callCount = AtomicInteger(0)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )
        val scheduler = SingleTaskScheduler<Int>()
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        scheduler.cancelTask()
        advanceUntilIdle()
        val countAtPause = callCount.get()

        scheduler.resumeLastTask(backgroundScope)
        runCurrent()
        val countAfterResume = callCount.get()
        advanceTimeBy(PERIOD)
        runCurrent()

        // Immediate invocation on resume.
        assertThat(countAfterResume).isEqualTo(countAtPause + 1)
        // After one more PERIOD elapses, at least one additional periodic tick has fired.
        assertThat(callCount.get()).isGreaterThan(countAfterResume)
        scheduler.cancelTask()
    }

    @Test
    fun `GIVEN scheduled task WHEN destroyTask THEN task stops and resumeLastTask is a no-op`() = runTest {
        val callCount = AtomicInteger(0)
        val periodicTask = PeriodicTask(
            delay = PERIOD,
            task = { callCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )
        val scheduler = SingleTaskScheduler<Int>()
        scheduler.scheduleTask(backgroundScope, periodicTask)
        runCurrent()
        assertThat(callCount.get()).isEqualTo(1)

        scheduler.destroyTask()
        advanceUntilIdle()
        val countAfterDestroy = callCount.get()

        scheduler.resumeLastTask(backgroundScope)
        advanceUntilIdle()

        assertThat(countAfterDestroy).isEqualTo(1)
        assertThat(callCount.get()).isEqualTo(countAfterDestroy)
    }

    @Test
    fun `GIVEN multiple scheduleTask calls WHEN resumeLastTask THEN only the latest task is resumed`() = runTest {
        val firstCount = AtomicInteger(0)
        val secondCount = AtomicInteger(0)
        val firstTask = PeriodicTask(
            delay = PERIOD,
            task = { firstCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )
        val secondTask = PeriodicTask(
            delay = PERIOD,
            task = { secondCount.incrementAndGet(); Result.success(VALUE) },
            onSuccess = mockk(relaxed = true),
            onError = mockk(relaxed = true),
            initialDelay = 0L,
        )
        val scheduler = SingleTaskScheduler<Int>()
        scheduler.scheduleTask(backgroundScope, firstTask)
        runCurrent()
        // scheduleTask cancels the previous task and overwrites lastTask.
        scheduler.scheduleTask(backgroundScope, secondTask)
        runCurrent()
        scheduler.cancelTask()
        advanceUntilIdle()
        val firstAtPause = firstCount.get()
        val secondAtPause = secondCount.get()

        scheduler.resumeLastTask(backgroundScope)
        runCurrent()

        assertThat(firstCount.get()).isEqualTo(firstAtPause)
        assertThat(secondCount.get()).isEqualTo(secondAtPause + 1)
        scheduler.cancelTask()
    }

    private companion object {
        const val PERIOD = 10_000L
        const val INITIAL_DELAY = 1_000L
        const val REQUEST_DURATION = 1_000L
        const val VALUE = 42
    }
}