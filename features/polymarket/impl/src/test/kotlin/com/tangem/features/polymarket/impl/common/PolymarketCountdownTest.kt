package com.tangem.features.polymarket.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.PolymarketServerClock
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class PolymarketCountdownTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SecondTicks {

        @Test
        fun `GIVEN clock between seconds WHEN collected THEN it ticks at once and then on whole seconds`() = runTest {
            // Arrange
            val clock = PolymarketServerClock { START + currentTime }

            // Act
            val actual = clock.secondTicks().take(count = 4).toList()

            // Assert
            assertThat(actual).containsExactly(START, 11_000L, 12_000L, 13_000L).inOrder()
        }

        @Test
        fun `GIVEN clock set forward between ticks WHEN collected THEN the next tick lands on its whole second`() =
            runTest {
                // Arrange
                var offset = 0L
                val clock = PolymarketServerClock { START + currentTime + offset }
                val ticks = mutableListOf<Long>()

                // Act
                clock.secondTicks().take(count = 3).collect { tick ->
                    ticks += tick
                    if (ticks.size == 1) offset = 5_400L
                }

                // Assert
                assertThat(ticks).containsExactly(START, 16_000L, 17_000L).inOrder()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FormatCountdown {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN time left WHEN formatted THEN it is rounded up and shown by its length`(model: FormatModel) {
            // Act
            val actual = formatPolymarketCountdown(model.remaining)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FormatModel(remaining = (-5).seconds, expected = "0:00"),
            FormatModel(remaining = Duration.ZERO, expected = "0:00"),
            FormatModel(remaining = 1.milliseconds, expected = "0:01"),
            FormatModel(remaining = 1.seconds, expected = "0:01"),
            FormatModel(remaining = 59_001.milliseconds, expected = "1:00"),
            FormatModel(remaining = 299.seconds, expected = "4:59"),
            FormatModel(remaining = 3_599.seconds, expected = "59:59"),
            FormatModel(remaining = 3_599_001.milliseconds, expected = "1:00:00"),
            FormatModel(remaining = 3_661_500.milliseconds, expected = "1:01:02"),
            FormatModel(remaining = 1.days, expected = "24:00:00"),
        )
    }

    data class FormatModel(val remaining: Duration, val expected: String)

    private companion object {
        const val START = 10_250L
    }
}