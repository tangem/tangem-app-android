package com.tangem.data.polymarket.price

import com.google.common.truth.Truth.assertThat
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceAuthDto.Credentials
import com.tangem.test.core.TestAppCoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPolymarketPriceCredentialsSourceTest {

    @Test
    fun `GIVEN the socket does not watch WHEN an observer authorizes THEN no credentials are exposed`() = runTest {
        // Arrange
        val source = createSource()

        // Act
        val observer = launch { source.authorize(FIRST) { awaitCancellation() } }
        runCurrent()

        // Assert
        assertThat(source.credentials.value).isNull()
        observer.cancelAndJoin()
    }

    @Test
    fun `GIVEN two observers WHEN they leave one by one THEN the newest authorizes and the last one's stay`() =
        runTest {
            // Arrange
            val source = createSource()
            watch(source)
            val firstLeaves = CompletableDeferred<Unit>()
            val secondLeaves = CompletableDeferred<Unit>()
            val seen = mutableListOf<Credentials?>()

            // Act
            launch { source.authorize(FIRST) { firstLeaves.await() } }
            runCurrent()
            seen += source.credentials.value
            launch { source.authorize(SECOND) { secondLeaves.await() } }
            runCurrent()
            seen += source.credentials.value
            secondLeaves.complete(Unit)
            runCurrent()
            seen += source.credentials.value
            firstLeaves.complete(Unit)
            runCurrent()
            seen += source.credentials.value

            // Assert
            assertThat(seen).containsExactly(FIRST, SECOND, FIRST, FIRST).inOrder()
        }

    @Test
    fun `GIVEN one wallet observes twice around another WHEN its newest observer leaves THEN the other authorizes`() =
        runTest {
            // Arrange
            val source = createSource()
            watch(source)
            val newestLeaves = CompletableDeferred<Unit>()
            backgroundScope.launch { source.authorize(FIRST) { awaitCancellation() } }
            runCurrent()
            backgroundScope.launch { source.authorize(SECOND) { awaitCancellation() } }
            runCurrent()
            launch { source.authorize(FIRST.copy()) { newestLeaves.await() } }
            runCurrent()
            val whileNewestObserves = source.credentials.value

            // Act
            newestLeaves.complete(Unit)
            runCurrent()

            // Assert
            assertThat(whileNewestObserves).isEqualTo(FIRST)
            assertThat(source.credentials.value).isEqualTo(SECOND)
        }

    @Test
    fun `GIVEN observers left WHEN the socket stops watching and watches again THEN it starts with nothing`() =
        runTest {
            // Arrange
            val source = createSource()
            val socket = watch(source)
            source.authorize(FIRST) { runCurrent() }
            runCurrent()
            val keptForIdleTail = source.credentials.value

            // Act
            socket.cancelAndJoin()
            runCurrent()
            val afterSocketLeft = source.credentials.value
            val seenAgain = mutableListOf<Credentials?>()
            backgroundScope.launch { source.credentials.toList(seenAgain) }
            runCurrent()

            // Assert
            assertThat(keptForIdleTail).isEqualTo(FIRST)
            assertThat(afterSocketLeft).isNull()
            assertThat(seenAgain).containsExactly(null)
        }

    @Test
    fun `GIVEN observer WHEN it is cancelled THEN its credentials never authorize the socket again`() = runTest {
        // Arrange
        val source = createSource()
        watch(source)
        val cancelled = launch { source.authorize(FIRST) { awaitCancellation() } }
        runCurrent()

        // Act
        cancelled.cancelAndJoin()
        source.authorize(SECOND) { runCurrent() }
        runCurrent()

        // Assert
        assertThat(source.credentials.value).isEqualTo(SECOND)
    }

    @Test
    fun `GIVEN observer WHEN its work fails THEN the failure is passed on and its credentials are released`() =
        runTest {
            // Arrange
            val source = createSource()
            watch(source)

            // Act
            val failure = runCatching { source.authorize(FIRST) { error("boom") } }.exceptionOrNull()
            source.authorize(SECOND) { runCurrent() }
            runCurrent()

            // Assert
            assertThat(failure).hasMessageThat().isEqualTo("boom")
            assertThat(source.credentials.value).isEqualTo(SECOND)
        }

    private fun TestScope.createSource() =
        DefaultPolymarketPriceCredentialsSource(TestAppCoroutineScope(backgroundScope.coroutineContext))

    private fun TestScope.watch(source: DefaultPolymarketPriceCredentialsSource): Job {
        val socket = backgroundScope.launch { source.credentials.collect {} }
        runCurrent()
        return socket
    }

    private companion object {
        val FIRST = Credentials(apiKey = "first-key", secret = "first-secret", passphrase = "first-pass")
        val SECOND = Credentials(apiKey = "second-key", secret = "second-secret", passphrase = "second-pass")
    }
}