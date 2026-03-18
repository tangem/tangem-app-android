package com.tangem.data.networks.store

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class StoreAdaptiveThrottleTest {

    @Test
    fun `first value is emitted immediately`() = runTest {
        val flow = flowOf(setOf(1, 2, 3)).adaptiveThrottle()

        flow.test {
            val item = awaitItem()
            assertThat(item).isEqualTo(setOf(1, 2, 3))
            awaitComplete()
        }
    }

    @Test
    fun `size change bypasses throttling`() = runTest {
        val upstream = MutableSharedFlow<Set<Int>>()

        upstream.adaptiveThrottle().test {

            upstream.emit(setOf(1, 2))
            assertThat(awaitItem()).isEqualTo(setOf(1, 2))

            upstream.emit(setOf(1, 2, 3))
            assertThat(awaitItem()).isEqualTo(setOf(1, 2, 3))

            upstream.emit(setOf(1))
            assertThat(awaitItem()).isEqualTo(setOf(1))
        }
    }

    @Test
    fun `same size events trigger throttling delay`() = runTest {
        val upstream = MutableSharedFlow<Set<Int>>()

        upstream.adaptiveThrottle().test {

            upstream.emit(setOf(1, 2))
            awaitItem()

            upstream.emit(setOf(3, 4))

            // delay should happen
            expectNoEvents()
            advanceTimeBy(250)

            val item = awaitItem()
            assertThat(item).isEqualTo(setOf(3, 4))
        }
    }

    @Test
    fun `rapid events result in only latest emission due to collectLatest`() = runTest {
        val upstream = MutableSharedFlow<Set<Int>>()

        upstream.adaptiveThrottle().test {

            upstream.emit(setOf(1, 2))
            awaitItem()

            launch {
                upstream.emit(setOf(3, 4))
                upstream.emit(setOf(5, 6))
                upstream.emit(setOf(7, 8))
            }

            // delay should happen
            expectNoEvents()
            advanceTimeBy(250)

            val item = awaitItem()
            assertThat(item).isEqualTo(setOf(7, 8))
        }
    }

    @Test
    fun `throttling resets when cooldown window passed`() = runTest {
        val upstream = MutableSharedFlow<Set<Int>>()

        upstream.adaptiveThrottle().test {

            upstream.emit(setOf(1, 2))
            awaitItem()

            upstream.emit(setOf(3, 4))
            expectNoEvents()
            advanceTimeBy(250)
            awaitItem()

            // wait long enough to reset throttling
            advanceTimeBy(2000)

            upstream.emit(setOf(5, 6))

            val item = awaitItem()
            assertThat(item).isEqualTo(setOf(5, 6))
        }
    }
}