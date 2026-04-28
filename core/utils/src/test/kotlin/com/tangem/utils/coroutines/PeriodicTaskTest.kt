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

    private companion object {
        const val PERIOD = 10_000L
        const val INITIAL_DELAY = 1_000L
        const val VALUE = 42
    }
}