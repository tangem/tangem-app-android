package com.tangem.data.polymarket

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.FakePolymarketSportsSocket
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPolymarketLiveScoresRepositoryTest {

    private val socket = FakePolymarketSportsSocket()
    private val repository = DefaultPolymarketLiveScoresRepository(sportsSocket = socket)

    @Test
    fun `GIVEN broadcast of several games WHEN observing some game ids THEN only their scores are emitted`() =
        runTest {
            // Arrange
            val received = mutableListOf<PolymarketLiveScore>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeLiveScores(gameIds = setOf("100", "300")).toList(received)
            }

            // Act
            socket.results.emit(result(gameId = 100, score = "1-0"))
            socket.results.emit(result(gameId = 200, score = "2-2"))
            socket.results.emit(result(gameId = 300, score = "0-3"))
            job.cancel()

            // Assert
            assertThat(received).containsExactly(
                PolymarketLiveScore(gameId = "100", score = "1-0", period = "Q1", isLive = true, isEnded = false),
                PolymarketLiveScore(gameId = "300", score = "0-3", period = "Q1", isLive = true, isEnded = false),
            ).inOrder()
        }

    @Test
    fun `GIVEN a game rebroadcast unchanged between other games WHEN observing it THEN only its changes are emitted`() =
        runTest {
            // Arrange
            val received = mutableListOf<PolymarketLiveScore>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeLiveScores(gameIds = setOf("100", "300")).toList(received)
            }

            // Act
            socket.results.emit(result(gameId = 100, score = "1-0"))
            socket.results.emit(result(gameId = 300, score = "0-3"))
            socket.results.emit(result(gameId = 100, score = "1-0"))
            socket.results.emit(result(gameId = 100, score = "2-0"))
            socket.results.emit(result(gameId = 300, score = "0-3"))
            job.cancel()

            // Assert
            assertThat(received.map { it.gameId to it.score }).containsExactly(
                "100" to "1-0",
                "300" to "0-3",
                "100" to "2-0",
            ).inOrder()
        }

    @Test
    fun `GIVEN a feed game id with a leading zero WHEN the socket sends it as a number THEN the feed id is emitted`() =
        runTest {
            // Arrange
            val received = mutableListOf<PolymarketLiveScore>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeLiveScores(gameIds = setOf("090108924")).toList(received)
            }

            // Act
            socket.results.emit(result(gameId = 90108924, score = "2-6"))
            job.cancel()

            // Assert
            assertThat(received).containsExactly(
                PolymarketLiveScore(gameId = "090108924", score = "2-6", period = "Q1", isLive = true, isEnded = false),
            )
        }

    private fun result(gameId: Long, score: String) = SportsResultDto(
        gameId = gameId,
        score = score,
        period = "Q1",
        isLive = true,
        isEnded = false,
    )
}