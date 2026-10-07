package com.tangem.core.socket

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class KeyedSubscriptionsTest {

    private val services = mutableListOf<FakeService>()

    @Test
    fun `GIVEN two observers of one key WHEN both collect THEN one subscribe frame goes out`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()

        // Act
        observe(subscriptions, setOf("a"))
        observe(subscriptions, setOf("a"))
        advanceTimeBy(BATCH + 1.milliseconds)

        // Assert
        assertThat(services.single().frames).containsExactly(Frame.Subscribe(setOf("a")))
    }

    @Test
    fun `GIVEN keys added within the batch window WHEN flushed THEN they go out as one frame`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()

        // Act
        observe(subscriptions, setOf("a"))
        advanceTimeBy(BATCH / 2)
        observe(subscriptions, setOf("b", "c"))
        advanceTimeBy(BATCH)

        // Assert
        assertThat(services.single().frames).containsExactly(Frame.Subscribe(setOf("a", "b", "c")))
    }

    @Test
    fun `GIVEN last observer leaves WHEN unsubscribe delay passes THEN one unsubscribe frame goes out`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val first = observe(subscriptions, setOf("a"))
        val second = observe(subscriptions, setOf("a"))
        advanceTimeBy(BATCH + 1.milliseconds)

        // Act
        first.cancel()
        second.cancel()
        advanceTimeBy(UNSUBSCRIBE_DELAY + BATCH + 1.milliseconds)

        // Assert
        assertThat(services.single().frames)
            .containsExactly(Frame.Subscribe(setOf("a")), Frame.Unsubscribe(setOf("a")))
            .inOrder()
    }

    @Test
    fun `GIVEN key released WHEN observed again before the delay THEN no unsubscribe frame goes out`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val first = observe(subscriptions, setOf("a"))
        advanceTimeBy(BATCH + 1.milliseconds)

        // Act
        first.cancel()
        advanceTimeBy(UNSUBSCRIBE_DELAY / 2)
        observe(subscriptions, setOf("a"))
        advanceTimeBy(UNSUBSCRIBE_DELAY + BATCH)

        // Assert
        assertThat(services.single().frames).containsExactly(Frame.Subscribe(setOf("a")))
    }

    @Test
    fun `GIVEN key released again after a quick reuse WHEN the first delay ends THEN it stays subscribed`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val first = observe(subscriptions, setOf("a"))
        advanceTimeBy(BATCH + 1.milliseconds)
        first.cancel()
        advanceTimeBy(UNSUBSCRIBE_DELAY / 4)
        val second = observe(subscriptions, setOf("a"))
        advanceTimeBy(UNSUBSCRIBE_DELAY / 2)

        // Act
        second.cancel()
        advanceTimeBy(UNSUBSCRIBE_DELAY / 2 + BATCH)
        val framesAfterFirstDelay = services.single().frames.toList()
        advanceTimeBy(UNSUBSCRIBE_DELAY)

        // Assert
        assertThat(framesAfterFirstDelay).containsExactly(Frame.Subscribe(setOf("a")))
        assertThat(services.single().frames)
            .containsExactly(Frame.Subscribe(setOf("a")), Frame.Unsubscribe(setOf("a")))
            .inOrder()
    }

    @Test
    fun `GIVEN invalid limits or windows WHEN subscriptions created THEN they are rejected`() = runTest {
        // Arrange
        val invalid = listOf<() -> Unit>(
            { createSubscriptions(maxKeysPerService = 0) },
            { createSubscriptions(batchWindow = (-1).milliseconds) },
            { createSubscriptions(unsubscribeDelay = (-1).milliseconds) },
        )

        // Act
        val errors = invalid.map { create -> runCatching { create() }.exceptionOrNull() }

        // Assert
        errors.forEach { error -> assertThat(error).isInstanceOf(IllegalArgumentException::class.java) }
    }

    @Test
    fun `GIVEN connection opens WHEN openKeys called THEN it returns every key and suppresses the delta frame`() =
        runTest {
            // Arrange
            val subscriptions = createSubscriptions()
            observe(subscriptions, setOf("a", "b"))

            // Act
            val openKeys = services.single().openKeys()
            advanceTimeBy(BATCH + 1.milliseconds)

            // Assert
            assertThat(openKeys).containsExactly("a", "b")
            assertThat(services.single().frames).isEmpty()
        }

    @Test
    fun `GIVEN send fails WHEN next change flushes THEN pending keys go out with it`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        observe(subscriptions, setOf("a"))
        services.single().isConnected = false
        advanceTimeBy(BATCH + 1.milliseconds)

        // Act
        services.single().isConnected = true
        observe(subscriptions, setOf("b"))
        advanceTimeBy(BATCH + 1.milliseconds)

        // Assert
        assertThat(services.single().frames).containsExactly(Frame.Subscribe(setOf("a", "b")))
    }

    @Test
    fun `GIVEN per-service key limit WHEN more keys observed THEN they go to a new shard`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions(maxKeysPerService = 2)

        // Act
        observe(subscriptions, setOf("a", "b", "c"))
        advanceTimeBy(BATCH + 1.milliseconds)

        // Assert
        assertThat(services).hasSize(2)
        assertThat(services[0].frames).containsExactly(Frame.Subscribe(setOf("a", "b")))
        assertThat(services[1].frames).containsExactly(Frame.Subscribe(setOf("c")))
    }

    @Test
    fun `GIVEN full shard WHEN a key is replaced within one batch THEN unsubscribe goes out before subscribe`() =
        runTest {
            // Arrange
            val subscriptions = createSubscriptions(maxKeysPerService = 2)
            val first = observe(subscriptions, setOf("a"))
            observe(subscriptions, setOf("b"))
            advanceTimeBy(BATCH + 1.milliseconds)

            // Act
            first.cancel()
            advanceTimeBy(UNSUBSCRIBE_DELAY + 1.milliseconds)
            observe(subscriptions, setOf("c"))
            advanceTimeBy(BATCH)

            // Assert
            assertThat(services.single().frames)
                .containsExactly(
                    Frame.Subscribe(setOf("a", "b")),
                    Frame.Unsubscribe(setOf("a")),
                    Frame.Subscribe(setOf("c")),
                )
                .inOrder()
        }

    @Test
    fun `GIVEN scope running jobs inline and zero windows WHEN keys change repeatedly THEN every change is sent`() =
        runTest {
            // Arrange
            val subscriptions = createSubscriptions(
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                batchWindow = Duration.ZERO,
                unsubscribeDelay = Duration.ZERO,
            )

            // Act
            val first = observe(subscriptions, setOf("a"))
            first.cancel()
            runCurrent()
            observe(subscriptions, setOf("b"))

            // Assert
            assertThat(services.single().frames)
                .containsExactly(
                    Frame.Subscribe(setOf("a")),
                    Frame.Unsubscribe(setOf("a")),
                    Frame.Subscribe(setOf("b")),
                )
                .inOrder()
        }

    @Test
    fun `GIVEN events for several keys WHEN observing one key THEN only its events are delivered`() = runTest {
        // Arrange
        val subscriptions = createSubscriptions()
        val received = mutableListOf<Pair<String, Int>>()
        backgroundScope.launch {
            subscriptions.observe(setOf("a"), events = { events }, keysOf = { listOf(it.first) }).toList(received)
        }
        runCurrent()

        // Act
        services.single().events.emit("a" to 1)
        services.single().events.emit("b" to 2)
        services.single().events.emit("a" to 3)
        runCurrent()

        // Assert
        assertThat(received).containsExactly("a" to 1, "a" to 3).inOrder()
    }

    private fun TestScope.createSubscriptions(
        scope: CoroutineScope = backgroundScope,
        maxKeysPerService: Int = Int.MAX_VALUE,
        batchWindow: Duration = BATCH,
        unsubscribeDelay: Duration = UNSUBSCRIBE_DELAY,
    ) = KeyedSubscriptions<String, FakeService>(
        scope = scope,
        newService = { _, openKeys -> FakeService(openKeys).also(services::add) },
        subscribe = { keys -> send(Frame.Subscribe(keys)) },
        unsubscribe = { keys -> send(Frame.Unsubscribe(keys)) },
        maxKeysPerService = maxKeysPerService,
        batchWindow = batchWindow,
        unsubscribeDelay = unsubscribeDelay,
    )

    private fun TestScope.observe(subscriptions: KeyedSubscriptions<String, FakeService>, keys: Set<String>): Job {
        val job = backgroundScope.launch {
            subscriptions.observe(keys, events = { events }, keysOf = { listOf(it.first) }).collect {}
        }
        runCurrent()
        return job
    }

    private class FakeService(val openKeys: () -> Set<String>) {

        val events = MutableSharedFlow<Pair<String, Int>>()
        val frames = mutableListOf<Frame>()
        var isConnected = true

        fun send(frame: Frame): Boolean {
            if (isConnected) frames += frame
            return isConnected
        }
    }

    private sealed interface Frame {

        data class Subscribe(val keys: Set<String>) : Frame

        data class Unsubscribe(val keys: Set<String>) : Frame
    }

    private companion object {
        val BATCH = 100.milliseconds
        val UNSUBSCRIBE_DELAY = 2.seconds
    }
}