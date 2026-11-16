package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketPriceInterval.DAY
import com.tangem.domain.polymarket.model.PolymarketPriceInterval.HOUR
import com.tangem.domain.polymarket.model.PolymarketPriceInterval.MAX
import com.tangem.domain.polymarket.model.PolymarketPriceInterval.MONTH
import com.tangem.domain.polymarket.model.PolymarketPriceInterval.SIX_HOURS
import com.tangem.domain.polymarket.model.PolymarketPriceInterval.WEEK
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Duration
import java.time.Instant

internal class PolymarketPriceIntervalTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Availability {
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN market age WHEN resolving periods THEN availability follows iOS`(model: AvailabilityCase) {
            // Arrange
            val lifetime = lifetime(age = model.age)

            // Act
            val actual = PolymarketPriceInterval.available(lifetime, NOW)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AvailabilityCase(Duration.ZERO, listOf(HOUR, SIX_HOURS, MAX)),
            AvailabilityCase(Duration.ofDays(1).minusSeconds(1), listOf(HOUR, SIX_HOURS, MAX)),
            AvailabilityCase(Duration.ofDays(1), listOf(HOUR, SIX_HOURS, DAY, MAX)),
            AvailabilityCase(Duration.ofDays(7).minusSeconds(1), listOf(HOUR, SIX_HOURS, DAY, MAX)),
            AvailabilityCase(Duration.ofDays(7), listOf(HOUR, SIX_HOURS, DAY, WEEK, MAX)),
            AvailabilityCase(Duration.ofDays(30).minusSeconds(1), listOf(HOUR, SIX_HOURS, DAY, WEEK, MAX)),
            AvailabilityCase(Duration.ofDays(30), listOf(HOUR, DAY, WEEK, MONTH, MAX)),
            AvailabilityCase(null, listOf(HOUR, DAY, WEEK, MONTH, MAX)),
        )

        @Test
        fun `GIVEN closed market without dates WHEN resolving periods THEN only MAX is offered`() {
            // Arrange
            val lifetime = lifetime(age = null, status = PolymarketMarketLifetime.Status.Closed(null, null))

            // Act
            val actual = PolymarketPriceInterval.available(lifetime, NOW)

            // Assert
            assertThat(actual).containsExactly(MAX)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Grid {
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN period and market age WHEN building query THEN grid follows iOS`(model: GridCase) {
            // Arrange
            val lifetime = lifetime(age = model.age)

            // Act
            val actual = model.interval.historyQuery(lifetime, NOW)

            // Assert
            val window = if (model.interval == MAX && lifetime.start != null) {
                PolymarketPriceHistoryQuery.Window.Range(lifetime.start)
            } else {
                PolymarketPriceHistoryQuery.Window.Interval(model.resolved)
            }
            assertThat(actual).isEqualTo(PolymarketPriceHistoryQuery(window, model.fidelity))
        }

        private fun provideTestModels() = listOf(
            GridCase(MAX, Duration.ZERO, MAX, 1),
            GridCase(MAX, Duration.ofHours(6).minusSeconds(1), MAX, 1),
            GridCase(MAX, Duration.ofHours(6), MAX, 5),
            GridCase(MAX, Duration.ofDays(1), MAX, 15),
            GridCase(MAX, Duration.ofHours(84), MAX, 30),
            GridCase(MAX, Duration.ofDays(7), MAX, 60),
            GridCase(MAX, Duration.ofDays(15), MAX, 180),
            GridCase(MAX, Duration.ofDays(30), MAX, 720),
            GridCase(SIX_HOURS, Duration.ofMinutes(30), HOUR, 1),
            GridCase(DAY, Duration.ofHours(3), SIX_HOURS, 1),
            GridCase(WEEK, Duration.ofHours(12), DAY, 5),
            GridCase(MONTH, Duration.ofDays(2), WEEK, 15),
            GridCase(WEEK, Duration.ofHours(84).minusSeconds(1), WEEK, 15),
            GridCase(WEEK, Duration.ofHours(84), WEEK, 30),
            GridCase(MONTH, Duration.ofDays(15).minusSeconds(1), MONTH, 60),
            GridCase(MONTH, Duration.ofDays(15), MONTH, 180),
        ) + PolymarketPriceInterval.entries.map { GridCase(it, null, it, if (it == MAX) 720 else 10) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClosedHistory {
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN closed market WHEN requesting MAX THEN trading span sets grid and confirmed close bounds query`(
            model: ClosedCase,
        ) {
            // Arrange
            val lifetime = PolymarketMarketLifetime(
                START,
                PolymarketMarketLifetime.Status.Closed(model.closedAt, model.scheduledEnd),
            )

            // Act
            val actual = MAX.historyQuery(lifetime, NOW)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketPriceHistoryQuery(
                    PolymarketPriceHistoryQuery.Window.Range(START, model.expectedEnd),
                    model.fidelity,
                ),
            )
        }

        private fun provideTestModels() = listOf(
            ClosedCase(START.plus(Duration.ofDays(6)), NOW, START.plus(Duration.ofDays(6)), 30),
            ClosedCase(START.plus(Duration.ofDays(14)), null, START.plus(Duration.ofDays(14)), 60),
            ClosedCase(START.plus(Duration.ofDays(14)).plusSeconds(1), null, null, 60),
            ClosedCase(START.plus(Duration.ofDays(20)), null, null, 180),
            ClosedCase(null, START.plus(Duration.ofDays(2)), null, 15),
            ClosedCase(null, NOW.plus(Duration.ofDays(30)), null, 720),
            ClosedCase(null, null, null, 720),
            ClosedCase(START.minusSeconds(1), null, null, 1),
        )
    }

    data class AvailabilityCase(val age: Duration?, val expected: List<PolymarketPriceInterval>)
    data class GridCase(
        val interval: PolymarketPriceInterval,
        val age: Duration?,
        val resolved: PolymarketPriceInterval,
        val fidelity: Int,
    )
    data class ClosedCase(
        val closedAt: Instant?,
        val scheduledEnd: Instant?,
        val expectedEnd: Instant?,
        val fidelity: Int,
    )

    private fun lifetime(
        age: Duration? = Duration.ofDays(60),
        status: PolymarketMarketLifetime.Status = PolymarketMarketLifetime.Status.Open,
    ) = PolymarketMarketLifetime(age?.let(NOW::minus), status)

    private companion object {
        val NOW: Instant = Instant.ofEpochSecond(1_792_668_524)
        val START: Instant = NOW.minus(Duration.ofDays(60))
    }
}