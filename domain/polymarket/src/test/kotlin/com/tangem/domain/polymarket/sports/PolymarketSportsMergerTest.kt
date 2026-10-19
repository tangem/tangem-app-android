package com.tangem.domain.polymarket.sports

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSportsState
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

internal class PolymarketSportsMergerTest {

    private val time = TestTimeSource()
    private val merger = PolymarketSportsMerger(time)

    @Test
    fun `GIVEN a tennis match WHEN REST and socket update THEN scores use the period of the selected source`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent(score = "3-2", period = "S1")))
        val initial = game()

        // Act
        merger.update(createSportsScore(score = "6-4", period = null))
        val afterFirstSet = game()
        merger.update(createSportsScore(score = "6-4, 2-3", period = "S2", timestamp = 120))
        val secondSet = game()
        merger.setEvents(listOf(createSportsEvent(score = "3-2", period = "S1", fetchedAt = Instant.ofEpochSecond(105))))
        val afterOlderRest = game()
        time += 61.seconds

        // Assert
        assertThat(initial).isEqualTo(expected(0, 0, "S1"))
        assertThat(afterFirstSet).isEqualTo(expected(1, 0, "S1"))
        assertThat(secondSet).isEqualTo(expected(1, 0, "S2"))
        assertThat(afterOlderRest).isEqualTo(secondSet)
        assertThat(game()).isEqualTo(expected(0, 0, "S1").copy(isStale = true))
    }

    @Test
    fun `GIVEN REST metadata WHEN no socket frame has arrived THEN the initial score is available`() {
        // Arrange
        val event = createSportsEvent(score = "2-1")

        // Act
        merger.setEvents(listOf(event, createSportsEvent(id = "other", gameId = null)))

        // Assert
        assertThat(merger.snapshot()).isEqualTo(PolymarketSportsState(mapOf("event" to expected(2, 1, "Q1"))))
    }

    @Test
    fun `GIVEN a socket update WHEN REST polls arrive out of order THEN the freshest source wins`() {
        // Arrange
        val event = createSportsEvent()
        merger.setEvents(listOf(event))
        merger.update(createSportsScore())

        // Act
        merger.setEvents(listOf(event.copy(score = "0-1", dataFetchedAt = Instant.ofEpochSecond(105))))
        val socketWins = game()
        merger.setEvents(listOf(event.copy(score = "2-1", period = "Q3", dataFetchedAt = Instant.ofEpochSecond(120))))
        val restWins = game()
        merger.update(createSportsScore(score = "0-1", timestamp = 109))
        merger.setEvents(listOf(event))

        // Assert
        assertThat(socketWins).isEqualTo(expected(1, 0))
        assertThat(restWins).isEqualTo(expected(2, 1, "Q3"))
        assertThat(game()).isEqualTo(restWins)
    }

    @Test
    fun `GIVEN two live games WHEN only one repeats THEN only the silent game falls back to REST`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent(), createSportsEvent(id = "other", gameId = "2")))
        merger.update(createSportsScore())
        merger.update(createSportsScore(gameId = "2"))
        time += 50.seconds
        merger.update(createSportsScore())

        // Act
        time += 11.seconds
        val snapshot = merger.snapshot()
        time += 50.seconds
        val expired = game()

        // Assert
        assertThat(snapshot.events).containsExactly(
            "event", expected(1, 0), "other", expected(0, 0, "Q1").copy(gameId = "2", isStale = true),
        )
        assertThat(expired).isEqualTo(expected(0, 0, "Q1").copy(isStale = true))
    }

    @Test
    fun `GIVEN REST newer than cached socket data WHEN a partial frame arrives THEN omitted fields retain the freshest value`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))
        merger.update(createSportsScore(score = "1-0"))
        merger.setEvents(listOf(createSportsEvent(score = "2-0", fetchedAt = Instant.ofEpochSecond(120))))

        // Act
        merger.update(createSportsScore(score = null, period = "Q3", live = null, ended = null, timestamp = 130))

        // Assert
        assertThat(game()).isEqualTo(expected(2, 0, "Q3"))
    }

    @Test
    fun `GIVEN a metadata only frame WHEN REST later provides a score THEN the empty frame cannot mask it`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))
        merger.update(createSportsScore(score = null, period = null, live = null, ended = null, timestamp = 130))

        // Act
        merger.setEvents(listOf(createSportsEvent(score = "2-0", fetchedAt = Instant.ofEpochSecond(120))))

        // Assert
        assertThat(game()).isEqualTo(expected(2, 0, "Q1"))
    }

    @Test
    fun `GIVEN a score update WHEN an older socket frame follows THEN it cannot roll back the score`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))
        merger.update(createSportsScore(score = "2-1", timestamp = 120))

        // Act
        merger.update(createSportsScore(score = "1-1", timestamp = 110))

        // Assert
        assertThat(game()).isEqualTo(expected(2, 1))
    }

    @Test
    fun `GIVEN a live score WHEN a partial ended frame arrives THEN the final score is retained without expiry`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))
        merger.update(createSportsScore(score = "3-1"))

        // Act
        merger.update(createSportsScore(score = null, period = "FT", live = null, ended = true, timestamp = 120))
        time += 120.seconds

        // Assert
        assertThat(game()).isEqualTo(expected(3, 1, "FT").copy(isLive = false, isEnded = true))
    }

    @Test
    fun `GIVEN a final socket result WHEN newer REST still reports live THEN the final result is retained`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))
        merger.update(createSportsScore(score = "2-0", period = "FT", live = false, ended = true))

        // Act
        merger.setEvents(listOf(createSportsEvent(score = "1-0", fetchedAt = Instant.ofEpochSecond(130))))
        time += 120.seconds
        val afterRest = game()
        merger.update(createSportsScore(score = null, period = null, live = null, ended = null, timestamp = 140))

        // Assert
        val final = expected(2, 0, "FT").copy(isLive = false, isEnded = true)
        assertThat(afterRest).isEqualTo(final)
        assertThat(game()).isEqualTo(final)
    }

    @Test
    fun `GIVEN a final socket result WHEN REST supplies a newer final correction THEN the correction is shown`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))
        merger.update(createSportsScore(score = "2-0", period = "FT", live = false, ended = true))

        // Act
        merger.setEvents(
            listOf(createSportsEvent(score = "2-1", live = false, ended = true, period = "FT", fetchedAt = Instant.ofEpochSecond(130))),
        )

        // Assert
        assertThat(game()).isEqualTo(expected(2, 1, "FT").copy(isLive = false, isEnded = true))
    }

    @Test
    fun `GIVEN a final socket result WHEN a newer socket frame explicitly reopens the match THEN it is applied`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))
        merger.update(createSportsScore(score = "2-0", period = "FT", live = false, ended = true))
        merger.setEvents(listOf(createSportsEvent(fetchedAt = Instant.ofEpochSecond(130))))

        // Act
        merger.update(createSportsScore(score = "2-1", period = "Q3", live = true, ended = false, timestamp = 140))

        // Assert
        assertThat(game()).isEqualTo(expected(2, 1, "Q3"))
    }

    @Test
    fun `GIVEN a removed game WHEN it is loaded again THEN old socket state does not leak into it`() {
        // Arrange
        val event = createSportsEvent()
        merger.setEvents(listOf(event))
        merger.update(createSportsScore())

        // Act
        merger.setEvents(emptyList())
        merger.update(createSportsScore(score = "2-1"))
        merger.setEvents(listOf(event))

        // Assert
        assertThat(game()).isEqualTo(expected(0, 0, "Q1"))
    }

    @Test
    fun `GIVEN an unknown REST timestamp WHEN a frame arrives THEN the socket supplies the score`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent(fetchedAt = null)))

        // Act
        merger.update(createSportsScore())

        // Assert
        assertThat(game()).isEqualTo(expected(1, 0))
    }

    @Test
    fun `GIVEN an unparseable socket score WHEN it replaces REST THEN no invented numbers are shown`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent(score = "2-1")))

        // Act
        merger.update(createSportsScore(score = "unknown"))

        // Assert
        assertThat(game()).isEqualTo(expected(0, 0).copy(score = null))
    }

    @ParameterizedTest
    @ValueSource(strings = ["CAN", "POST", "INT", "AB"])
    fun `GIVEN a suspended match WHEN presenting THEN its raw period stays and Live is hidden`(period: String) {
        // Arrange
        merger.setEvents(listOf(createSportsEvent()))

        // Act
        merger.update(createSportsScore(period = period))

        // Assert
        assertThat(game()).isEqualTo(expected(1, 0, period).copy(isLive = false))
    }

    @Test
    fun `GIVEN pre match metadata WHEN presenting THEN score and Live badge are absent`() {
        // Arrange
        val event = createSportsEvent(score = "0-0", live = false, period = null)

        // Act
        merger.setEvents(listOf(event))

        // Assert
        assertThat(game()).isEqualTo(expected(0, 0, null).copy(score = null, isLive = false))
    }

    @Test
    fun `GIVEN two events for one game WHEN a score arrives THEN both cards share the game state`() {
        // Arrange
        merger.setEvents(listOf(createSportsEvent(), createSportsEvent(id = "other")))

        // Act
        merger.update(createSportsScore())

        // Assert
        assertThat(merger.snapshot().events).containsExactly("event", expected(1, 0), "other", expected(1, 0))
    }

    private fun game() = merger.snapshot().events.getValue("event")

    private fun expected(home: Int, away: Int, period: String? = "Q2") = PolymarketSportsState.Game(
        "1", PolymarketSportsState.Score(home, away), period, isLive = true, isEnded = false,
        home = null, away = null, isStale = false,
    )
}