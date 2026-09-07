package com.tangem.features.polymarket.impl.main.model

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketStaleDataReporterTest {

    private var reports = 0

    @Test
    fun `GIVEN nothing reported yet WHEN report THEN the user is told`() = runTest {
        // Arrange
        val reporter = createReporter()

        // Act
        reporter.report()

        // Assert
        assertThat(reports).isEqualTo(1)
    }

    @Test
    fun `GIVEN a report within the cooldown WHEN report again THEN the user is not told twice`() = runTest {
        // Arrange
        val reporter = createReporter()
        reporter.report()

        // Act
        repeat(times = 3) {
            advanceTimeBy(COOLDOWN / 4)
            reporter.report()
        }

        // Assert
        assertThat(reports).isEqualTo(1)
    }

    @Test
    fun `GIVEN the cooldown passed since the report WHEN report THEN the user is told again`() = runTest {
        // Arrange
        val reporter = createReporter()
        reporter.report()

        // Act
        advanceTimeBy(COOLDOWN)
        reporter.report()

        // Assert
        assertThat(reports).isEqualTo(2)
    }

    private fun TestScope.createReporter() = PolymarketStaleDataReporter(
        onReport = { reports++ },
        timeSource = testScheduler.timeSource,
        cooldown = COOLDOWN,
    )

    private companion object {
        val COOLDOWN = 5.minutes
    }
}