package com.tangem.data.yield.supply.incentives.cache

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class DefaultYieldIncentivesCacheTest {

    @Test
    fun `GIVEN a stored value within the window WHEN read again THEN it is not fetched again`() = runTest {
        // Arrange
        val cache = createCache()
        var fetches = 0
        cache.getOrFetch(key = KEY, window = WINDOW) { (++fetches).right() }

        // Act
        val actual = cache.getOrFetch(key = KEY, window = WINDOW) { (++fetches).right() }

        // Assert
        assertThat(actual).isEqualTo(1.right())
        assertThat(fetches).isEqualTo(1)
    }

    @Test
    fun `GIVEN a fetch in flight WHEN invalidated THEN its answer is not served afterwards`() = runTest {
        // Arrange
        val cache = createCache()
        val staleAnswer = CompletableDeferred<Int>()
        val inFlight = backgroundScope.async {
            cache.getOrFetch(key = KEY, window = WINDOW) { staleAnswer.await().right() }
        }
        runCurrent()

        // Act
        cache.invalidate(KEY)
        staleAnswer.complete(STALE)
        inFlight.await()
        val actual = cache.getOrFetch(key = KEY, window = WINDOW) { FRESH.right() }

        // Assert
        assertThat(actual).isEqualTo(FRESH.right())
    }

    @Test
    fun `GIVEN a fetch in flight WHEN invalidated THEN the next read does not wait for it`() = runTest {
        // Arrange
        val cache = createCache()
        val staleAnswer = CompletableDeferred<Int>()
        backgroundScope.async {
            cache.getOrFetch(key = KEY, window = WINDOW) { staleAnswer.await().right() }
        }
        runCurrent()

        // Act
        cache.invalidate(KEY)
        val fresh = backgroundScope.async { cache.getOrFetch(key = KEY, window = WINDOW) { FRESH.right() } }
        runCurrent()

        // Assert
        assertThat(fresh.isCompleted).isTrue()
        assertThat(fresh.await()).isEqualTo(FRESH.right())
        staleAnswer.complete(STALE)
    }

    private fun TestScope.createCache() = DefaultYieldIncentivesCache(currentTimeMillis = { testScheduler.currentTime })

    private companion object {
        const val KEY = "incentives:1:0xowner"
        const val STALE = 1
        const val FRESH = 2
        val WINDOW = 30.seconds
    }
}