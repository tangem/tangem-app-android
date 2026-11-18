package com.tangem.data.polymarket.price

import com.google.common.truth.Truth.assertThat
import com.tangem.core.socket.SocketState
import com.tangem.domain.polymarket.model.PolymarketCryptoPricePoint
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries.Status
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceMessageDto
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceMessageDto.Payload
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceMessageDto.Point
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

internal class PolymarketCryptoPriceSeriesReducerTest {

    private val reducer = PolymarketCryptoPriceSeriesReducer

    @Test
    fun `GIVEN empty series WHEN a snapshot arrives THEN its valid points become the series`() {
        // Arrange
        val snapshot = snapshot(
            Point(timestamp = 3_000.0, value = 3.0),
            Point(timestamp = 1_000.0, value = 1.0, fullAccuracyValue = "1.00000001"),
            Point(timestamp = 2_000.0, value = 0.0),
            Point(timestamp = 3_000.0, value = 3.5),
            Point(timestamp = 0.0, value = 9.0),
            Point(timestamp = 4_000.0),
        )

        // Act
        val actual = reducer.reduce(PolymarketCryptoPriceSeries(), snapshot)

        // Assert
        assertThat(actual).isEqualTo(live(point(1_000, "1.00000001"), point(3_000, "3.0")))
    }

    @Test
    fun `GIVEN series WHEN a snapshot arrives after a reconnect THEN it replaces everything from its first point`() {
        // Arrange
        val series = live(point(1_000, "1.0"), point(2_000, "2.0"), point(3_000, "3.0"), point(4_000, "4.0"))
        val snapshot = snapshot(Point(timestamp = 3_000.0, value = 30.0), Point(timestamp = 5_000.0, value = 50.0))

        // Act
        val actual = reducer.reduce(series, snapshot)

        // Assert
        assertThat(actual)
            .isEqualTo(live(point(1_000, "1.0"), point(2_000, "2.0"), point(3_000, "30.0"), point(5_000, "50.0")))
    }

    @Test
    fun `GIVEN series WHEN an empty snapshot arrives THEN the series is kept`() {
        // Arrange
        val series = live(point(1_000, "1.0"))

        // Act
        val actual = reducer.reduce(series, snapshot())

        // Assert
        assertThat(actual).isEqualTo(series)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Ticks {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN series WHEN a tick arrives THEN only a valid newer one is added`(model: TickModel) {
            // Arrange
            val series = live(point(2_000, "2.0"))

            // Act
            val message = PolymarketPriceMessageDto(channel = "price.crypto", payload = model.tick)
            val actual = reducer.reduce(series, message)

            // Assert
            assertThat(actual).isEqualTo(model.expected ?: series)
        }

        private fun provideTestModels() = listOf(
            TickModel(
                case = "newer tick",
                tick = Payload(timestamp = 3_000.0, value = 3.0),
                expected = live(point(2_000, "2.0"), point(3_000, "3.0")),
            ),
            TickModel(
                case = "exact value wins over the rounded one",
                tick = Payload(timestamp = 3_000.0, value = 3.0, fullAccuracyValue = "3.14159265358979"),
                expected = live(point(2_000, "2.0"), point(3_000, "3.14159265358979")),
            ),
            TickModel(
                case = "unreadable exact value falls back to the rounded one",
                tick = Payload(timestamp = 3_000.0, value = 3.0, fullAccuracyValue = "n/a"),
                expected = live(point(2_000, "2.0"), point(3_000, "3.0")),
            ),
            TickModel(case = "same time as the last point", tick = Payload(timestamp = 2_000.0, value = 9.0)),
            TickModel(case = "older than the last point", tick = Payload(timestamp = 1_000.0, value = 9.0)),
            TickModel(case = "zero price", tick = Payload(timestamp = 3_000.0, value = 0.0)),
            TickModel(case = "negative price", tick = Payload(timestamp = 3_000.0, fullAccuracyValue = "-1")),
            TickModel(case = "no price", tick = Payload(timestamp = 3_000.0)),
            TickModel(case = "no time", tick = Payload(value = 3.0)),
        )
    }

    data class TickModel(val case: String, val tick: Payload, val expected: PolymarketCryptoPriceSeries? = null)

    @Test
    fun `GIVEN long series WHEN a tick arrives THEN points older than the kept history are dropped`() {
        // Arrange
        val newest = 200.seconds.inWholeMilliseconds
        val series = live(point(70_000, "1.0"), point(72_500, "2.0"), point(100_000, "3.0"))

        // Act
        val actual = reducer.reduce(series, tick(timestamp = newest, value = 4.0))

        // Assert
        assertThat(actual).isEqualTo(live(point(72_500, "2.0"), point(100_000, "3.0"), point(newest, "4.0")))
    }

    @Test
    fun `GIVEN snapshot larger than the limit WHEN reduced THEN only the newest points are kept`() {
        // Arrange
        val points = (1..3_700).map { index -> Point(timestamp = index.toDouble(), value = 1.0) }

        // Act
        val actual = reducer.reduce(PolymarketCryptoPriceSeries(), snapshot(*points.toTypedArray()))

        // Assert
        assertThat(actual.points).hasSize(3_600)
        assertThat(actual.points.first().timestampMillis).isEqualTo(101)
        assertThat(actual.points.last().timestampMillis).isEqualTo(3_700)
    }

    @Test
    fun `GIVEN live series WHEN the server answers an error THEN it is unavailable and keeps its points`() {
        // Arrange
        val series = live(point(1_000, "1.0"))
        val error = PolymarketPriceMessageDto(op = "error", code = "provider_required", channel = "price.crypto.twap")

        // Act
        val actual = reducer.reduce(series, error)

        // Assert
        assertThat(actual).isEqualTo(series.copy(status = Status.UNAVAILABLE))
    }

    @Test
    fun `GIVEN unavailable series WHEN a tick arrives THEN it is live again`() {
        // Arrange
        val series = live(point(1_000, "1.0")).copy(status = Status.UNAVAILABLE)

        // Act
        val actual = reducer.reduce(series, tick(timestamp = 2_000, value = 2.0))

        // Assert
        assertThat(actual).isEqualTo(live(point(1_000, "1.0"), point(2_000, "2.0")))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class States {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN socket state WHEN reduced THEN only a stopped socket makes the series unavailable`(
            model: StateModel,
        ) {
            // Arrange
            val series = live(point(1_000, "1.0"))

            // Act
            val actual = reducer.reduce(series, model.state)

            // Assert
            assertThat(actual.status).isEqualTo(model.expected)
            assertThat(actual.points).isEqualTo(series.points)
        }

        private fun provideTestModels() = listOf(
            StateModel(state = SocketState.Stopped(code = 4001, reason = ""), expected = Status.UNAVAILABLE),
            StateModel(state = SocketState.Idle, expected = Status.LIVE),
            StateModel(state = SocketState.Connecting(attempt = 1), expected = Status.LIVE),
            StateModel(state = SocketState.Connected, expected = Status.LIVE),
            StateModel(state = SocketState.WaitingToRetry(attempt = 1, delay = 2.seconds), expected = Status.LIVE),
            StateModel(state = SocketState.Paused(SocketState.PauseReason.NoNetwork), expected = Status.LIVE),
        )
    }

    data class StateModel(val state: SocketState, val expected: Status)

    private fun snapshot(vararg points: Point) = PolymarketPriceMessageDto(
        channel = "price.crypto",
        payload = Payload(symbol = "coinusd", points = points.toList()),
    )

    private fun tick(timestamp: Long, value: Double) = PolymarketPriceMessageDto(
        channel = "price.crypto",
        payload = Payload(symbol = "coinusd", timestamp = timestamp.toDouble(), value = value),
    )

    private companion object {

        fun point(timestampMillis: Long, price: String) = PolymarketCryptoPricePoint(timestampMillis, BigDecimal(price))

        fun live(vararg points: PolymarketCryptoPricePoint) = PolymarketCryptoPriceSeries(points.toList(), Status.LIVE)
    }
}