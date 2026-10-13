package com.tangem.data.polymarket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.socket.SocketState
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.FakePolymarketSportsSocket
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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

    @Test
    fun `GIVEN a repeated score WHEN the socket reconnects in between THEN the score is emitted again`() = runTest {
        // Arrange
        val received = mutableListOf<PolymarketLiveScore>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeLiveScores(gameIds = setOf("100")).toList(received)
        }

        // Act
        socket.results.emit(result(gameId = 100, score = "2-1"))
        socket.state.value = SocketState.Connecting(attempt = 0)
        socket.state.value = SocketState.Connected
        socket.results.emit(result(gameId = 100, score = "2-1"))
        job.cancel()

        // Assert
        assertThat(received.map { it.score }).containsExactly("2-1", "2-1")
    }

    @Test
    fun `GIVEN two feed ids of one socket game WHEN its score arrives THEN it is emitted for both feed ids`() = runTest {
        // Arrange
        val received = mutableListOf<PolymarketLiveScore>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeLiveScores(gameIds = setOf("90108924", "090108924")).toList(received)
        }

        // Act
        socket.results.emit(result(gameId = 90108924, score = "2-6"))
        job.cancel()

        // Assert
        assertThat(received.map { it.gameId }).containsExactly("90108924", "090108924")
    }

    @Test
    fun `GIVEN an unchanged score WHEN only the game status changes THEN the update is emitted`() = runTest {
        // Arrange
        val received = mutableListOf<PolymarketLiveScore>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeLiveScores(gameIds = setOf("100")).toList(received)
        }

        // Act
        socket.results.emit(result(gameId = 100, score = "2-1"))
        socket.results.emit(result(gameId = 100, score = "2-1", isLive = false, isEnded = true))
        job.cancel()

        // Assert
        assertThat(received.map { it.isEnded }).containsExactly(false, true).inOrder()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "not-a-number"])
    fun `GIVEN no numeric game ids WHEN observing THEN the socket is not subscribed`(gameId: String) = runTest {
        // Arrange
        val gameIds = if (gameId.isEmpty()) emptySet() else setOf(gameId)

        // Act
        val received = repository.observeLiveScores(gameIds = gameIds).toList()

        // Assert
        assertThat(received).isEmpty()
        assertThat(socket.results.subscriptionCount.value).isEqualTo(0)
    }

    private fun result(gameId: Long, score: String, isLive: Boolean = true, isEnded: Boolean = false) =
        SportsResultDto(
            gameId = gameId,
            score = score,
            period = "Q1",
            isLive = isLive,
            isEnded = isEnded,
        )
}