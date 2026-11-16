package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.sports.createSportsEvent
import com.tangem.domain.polymarket.sports.createSportsScore
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class ObservePolymarketSportsStateUseCaseTest {

    private val repository: PolymarketLiveScoresRepository = mockk()
    private val scores = MutableSharedFlow<PolymarketLiveScore>()
    private val connections = MutableStateFlow(false)
    private val events = MutableStateFlow(listOf(createSportsEvent()))

    @Test
    fun `GIVEN a final score WHEN paused and resumed THEN subscriptions stop but the result survives`() = runTest {
        // Arrange
        every { repository.observeLiveScores(emptySet()) } returns emptyFlow()
        every { repository.observeLiveScores(setOf("1")) } returns scores
        every { repository.observeConnectionState() } returns connections
        val resumed = MutableStateFlow(true)
        val useCase = ObservePolymarketSportsStateUseCase(
            repository, createTestingCoroutineDispatcherProvider(), testScheduler.timeSource,
        )
        val received = mutableListOf<PolymarketSportsState>()
        val job = backgroundScope.launch { useCase(events, resumed).toList(received) }
        runCurrent()
        scores.emit(createSportsScore(score = "2-0", period = "FT", live = false, ended = true))
        runCurrent()
        val finalGame = received.last().events.getValue("event")

        // Act
        resumed.value = false
        runCurrent()
        val pausedSubscriptions = scores.subscriptionCount.value
        val pausedConnections = connections.subscriptionCount.value
        events.value = listOf(createSportsEvent(score = "1-0", fetchedAt = Instant.ofEpochSecond(130)))
        advanceTimeBy(120_000)
        resumed.value = true
        runCurrent()
        val resumedSubscriptions = scores.subscriptionCount.value
        val afterResume = received.last().events.getValue("event")
        job.cancel()
        runCurrent()

        // Assert
        assertThat(finalGame.isEnded).isTrue()
        assertThat(finalGame.score).isEqualTo(PolymarketSportsState.Score(2, 0))
        assertThat(pausedSubscriptions).isEqualTo(0)
        assertThat(pausedConnections).isEqualTo(0)
        assertThat(resumedSubscriptions).isEqualTo(1)
        assertThat(afterResume).isEqualTo(finalGame)
        assertThat(scores.subscriptionCount.value).isEqualTo(0)
        assertThat(events.subscriptionCount.value).isEqualTo(0)
        assertThat(connections.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `GIVEN unchanged frames and REST polls WHEN observing THEN freshness advances without redrawing or resubscribing`() =
        runTest {
            // Arrange
            every { repository.observeLiveScores(emptySet()) } returns emptyFlow()
            every { repository.observeLiveScores(setOf("1")) } returns scores
            every { repository.observeConnectionState() } returns connections
            val useCase = ObservePolymarketSportsStateUseCase(
                repository, createTestingCoroutineDispatcherProvider(), testScheduler.timeSource,
            )
            val received = mutableListOf<PolymarketSportsState>()
            val job = backgroundScope.launch { useCase(events).toList(received) }
            runCurrent()
            scores.emit(createSportsScore())
            runCurrent()
            val afterFirstFrame = received.toList()

            // Act
            advanceTimeBy(50_000)
            scores.emit(createSportsScore())
            events.value = listOf(createSportsEvent().copy(title = "Updated metadata"))
            advanceTimeBy(20_000)
            runCurrent()
            val beforeExpiry = received.toList()
            advanceTimeBy(41_000)
            runCurrent()
            job.cancel()
            runCurrent()

            // Assert
            assertThat(afterFirstFrame.last().events["event"]?.score).isEqualTo(PolymarketSportsState.Score(1, 0))
            assertThat(beforeExpiry).isEqualTo(afterFirstFrame)
            assertThat(received.last().events["event"]?.isStale).isTrue()
            assertThat(received.last().events["event"]?.score).isEqualTo(PolymarketSportsState.Score(0, 0))
            verify(exactly = 1) { repository.observeLiveScores(setOf("1")) }
            assertThat(scores.subscriptionCount.value).isEqualTo(0)
            assertThat(events.subscriptionCount.value).isEqualTo(0)
            assertThat(connections.subscriptionCount.value).isEqualTo(0)
        }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN an observer WHEN connection recovers THEN only subsequent connections request REST`(initiallyConnected: Boolean) =
        runTest {
            // Arrange
            connections.value = initiallyConnected
            every { repository.observeLiveScores(any()) } returns emptyFlow()
            every { repository.observeConnectionState() } returns connections
            val useCase = ObservePolymarketSportsStateUseCase(repository, createTestingCoroutineDispatcherProvider())
            val received = mutableListOf<PolymarketSportsState>()
            backgroundScope.launch { useCase(events).toList(received) }
            runCurrent()
            connections.value = true
            runCurrent()
            val initial = received.last()

            // Act
            connections.value = false
            runCurrent()
            connections.value = true
            runCurrent()

            // Assert
            assertThat(initial.resnapshotRevision).isEqualTo(0)
            assertThat(received.last().resnapshotRevision).isEqualTo(1)
        }

    @Test
    fun `GIVEN a connected observer WHEN resumed after an unobserved reconnect THEN REST is requested`() = runTest {
        // Arrange
        connections.value = true
        every { repository.observeLiveScores(any()) } returns emptyFlow()
        every { repository.observeConnectionState() } returns connections
        val resumed = MutableStateFlow(true)
        val useCase = ObservePolymarketSportsStateUseCase(repository, createTestingCoroutineDispatcherProvider())
        val received = mutableListOf<PolymarketSportsState>()
        backgroundScope.launch { useCase(events, resumed).toList(received) }
        runCurrent()

        // Act
        resumed.value = false
        runCurrent()
        connections.value = false
        connections.value = true
        resumed.value = true
        runCurrent()

        // Assert
        assertThat(received.last().resnapshotRevision).isEqualTo(1)
    }
}