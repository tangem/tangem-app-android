package com.tangem.google.auth

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class AwaitUnlessUiLostTest {

    private val lifecycleOwner: LifecycleOwner = mockk()

    @Test
    fun `GIVEN the UI over the host WHEN the result arrives before the host resumes THEN it is returned`() = runTest {
        // Arrange
        val lifecycle = createLifecycle()
        val response = CompletableDeferred<String>()

        // Act
        val actual = async { awaitUnlessUiLost(lifecycle, GRACE_PERIOD) { response.await() } }
        runCurrent()
        lifecycle.currentState = Lifecycle.State.STARTED
        response.complete(RESULT)
        lifecycle.currentState = Lifecycle.State.RESUMED

        // Assert
        assertThat(actual.await()).isEqualTo(RESULT)
    }

    @Test
    fun `GIVEN the app minimized and reopened WHEN no result comes THEN it gives up after the grace period`() =
        runTest {
            // Arrange
            val lifecycle = createLifecycle()
            val response = CompletableDeferred<String>()

            // Act
            val actual = async { awaitUnlessUiLost(lifecycle, GRACE_PERIOD) { response.await() } }
            runCurrent()
            lifecycle.currentState = Lifecycle.State.CREATED
            lifecycle.currentState = Lifecycle.State.RESUMED
            advanceUntilIdle()

            // Assert
            assertThat(actual.await()).isNull()
        }

    @Test
    fun `GIVEN the screen turned off and on WHEN the host restarts behind the UI THEN it keeps waiting`() =
        runTest {
            // Arrange
            val lifecycle = createLifecycle()
            val response = CompletableDeferred<String>()

            // Act
            val actual = async { awaitUnlessUiLost(lifecycle, GRACE_PERIOD) { response.await() } }
            runCurrent()
            lifecycle.currentState = Lifecycle.State.STARTED
            lifecycle.currentState = Lifecycle.State.CREATED
            lifecycle.currentState = Lifecycle.State.STARTED
            advanceTimeBy(GRACE_PERIOD * 2)
            response.complete(RESULT)

            // Assert
            assertThat(actual.await()).isEqualTo(RESULT)
        }

    @Test
    fun `GIVEN the host stopped at the call WHEN it resumes without a result THEN it gives up`() = runTest {
        // Arrange
        val lifecycle = createLifecycle(Lifecycle.State.CREATED)
        val response = CompletableDeferred<String>()

        // Act
        val actual = async { awaitUnlessUiLost(lifecycle, GRACE_PERIOD) { response.await() } }
        runCurrent()
        lifecycle.currentState = Lifecycle.State.RESUMED
        advanceUntilIdle()

        // Assert
        assertThat(actual.await()).isNull()
    }

    @Test
    fun `GIVEN the app reopened WHEN the result comes within the grace period THEN it is returned`() = runTest {
        // Arrange
        val lifecycle = createLifecycle()
        val response = CompletableDeferred<String>()

        // Act
        val actual = async { awaitUnlessUiLost(lifecycle, GRACE_PERIOD) { response.await() } }
        runCurrent()
        lifecycle.currentState = Lifecycle.State.CREATED
        lifecycle.currentState = Lifecycle.State.RESUMED
        advanceTimeBy(GRACE_PERIOD / 2)
        response.complete(RESULT)

        // Assert
        assertThat(actual.await()).isEqualTo(RESULT)
    }

    @Test
    fun `GIVEN the request fails WHEN awaited THEN the failure is rethrown`() = runTest {
        // Arrange
        val lifecycle = createLifecycle()
        val failure = IllegalStateException("boom")

        // Act
        val actual = runCatching {
            awaitUnlessUiLost<String>(lifecycle, GRACE_PERIOD) { throw failure }
        }.exceptionOrNull()

        // Assert — the coroutine machinery may hand back a copy of the exception, so its type is what counts
        assertThat(actual).isInstanceOf(IllegalStateException::class.java)
        assertThat(actual).hasMessageThat().isEqualTo(failure.message)
    }

    private fun createLifecycle(state: Lifecycle.State = Lifecycle.State.RESUMED): LifecycleRegistry =
        LifecycleRegistry.createUnsafe(lifecycleOwner).apply { currentState = state }

    private companion object {
        const val RESULT = "token"
        val GRACE_PERIOD = 2.seconds
    }
}