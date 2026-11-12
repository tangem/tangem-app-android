package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSoccerSportsTest {
    private val repository: PolymarketLiveScoresRepository = mockk()
    private val scores = MutableSharedFlow<PolymarketLiveScore>()
    private val connections = MutableStateFlow(false)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN reversed BFF sides WHEN socket merges THEN header stays in button order through stale REST and pause`(
        explicitOrdering: Boolean,
    ) = runTest {
        // Arrange
        val event = soccerEvent(emptyList()).let { event -> event.copy(
            gameId = "001", teams = event.teams.reversed().map {
                if (explicitOrdering) it else it.copy(ordering = null)
            }, dataFetchedAt = Instant.ofEpochSecond(100), score = "1-0",
        ) }
        val rest = MutableStateFlow(event)
        val resumed = MutableStateFlow(true)
        every { repository.observeConnectionState() } returns connections
        every { repository.observeLiveScores(emptySet()) } returns emptyFlow()
        every { repository.observeLiveScores(setOf("001")) } returns scores
        val useCase = ObservePolymarketSportsStateUseCase(
            repository, createTestingCoroutineDispatcherProvider(), testScheduler.timeSource,
        )
        val values = mutableListOf<PolymarketSportsState>()
        backgroundScope.launch { useCase.observeSoccer(rest, resumed).toList(values) }
        runCurrent()

        // Act
        scores.emit(PolymarketLiveScore(
            "001", "3-1", "2H", true, false, "Home", "Away", updatedAt = Instant.ofEpochSecond(110),
        ))
        runCurrent()
        val game = values.last().events.getValue("main")
        rest.value = event.copy(score = "0-0", dataFetchedAt = Instant.ofEpochSecond(105))
        runCurrent()
        resumed.value = false
        runCurrent()

        // Assert
        val header = event.sportsUM(game)
        assertThat(header).isEqualTo(event.sportsUM(null).copy(
            homeScore = "3", awayScore = "1", period = "2H", isLive = true,
        ))
        assertThat(values.last().events.getValue("main")).isEqualTo(game)
        assertThat(scores.subscriptionCount.value).isEqualTo(0)
        resumed.value = true
        runCurrent()
        assertThat(scores.subscriptionCount.value).isEqualTo(1)
        verify { repository.observeLiveScores(setOf("001")) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["HT", "POST", "CAN", "INT", "AB"])
    fun `GIVEN a nonterminal period WHEN rendered THEN phase is not mistaken for completion`(period: String) {
        // Arrange
        val event = soccerEvent(emptyList())
        val game = PolymarketSportsState.Game(
            "game", PolymarketSportsState.Score(1, 0), period, true, false,
            PolymarketSportsState.Team(event.teams[0], 0), PolymarketSportsState.Team(event.teams[1], 1), false,
        )

        // Act
        val header = event.sportsUM(game)

        // Assert
        assertThat(header.isLive).isEqualTo(period == "HT")
        assertThat(header.period).isEqualTo(period)
    }
}