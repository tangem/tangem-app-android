package com.tangem.tap.common.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import dagger.Lazy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AnalyticsHandlersInitializerTest {

    @Test
    fun `GIVEN handlers WHEN initialize THEN they are resolved once and await returns`() = runTest {
        // Arrange
        var resolutions = 0
        val initializer = createInitializer(
            handlers = Lazy {
                resolutions++
                emptySet()
            },
        )

        // Act
        initializer.initialize()
        val resolutionsBeforeRun = resolutions
        advanceUntilIdle()
        val awaited = runCatching { initializer.awaitInitialized() }

        // Assert
        assertThat(resolutionsBeforeRun).isEqualTo(0)
        assertThat(resolutions).isEqualTo(1)
        assertThat(awaited.isSuccess).isTrue()
    }

    @Test
    fun `GIVEN handler creation fails WHEN initialize THEN await returns without throwing`() = runTest {
        // Arrange
        val initializer = createInitializer(handlers = Lazy { error("missing api key") })

        // Act
        initializer.initialize()
        advanceUntilIdle()
        val awaited = runCatching { initializer.awaitInitialized() }

        // Assert
        assertThat(awaited.isSuccess).isTrue()
    }

    @Test
    fun `GIVEN initialize never called WHEN await THEN returns immediately`() = runTest {
        // Arrange
        val initializer = createInitializer(handlers = Lazy { emptySet() })

        // Act
        val startedAt = System.nanoTime()
        initializer.awaitInitialized()
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        // Assert
        assertThat(elapsedMs).isLessThan(500L)
    }

    private fun TestScope.createInitializer(handlers: Lazy<Set<AnalyticsHandler>>) = AnalyticsHandlersInitializer(
        handlers = handlers,
        appScope = TestAppCoroutineScope(testScope = this),
        dispatchers = TestingCoroutineDispatcherProvider(io = StandardTestDispatcher(testScheduler)),
    )
}