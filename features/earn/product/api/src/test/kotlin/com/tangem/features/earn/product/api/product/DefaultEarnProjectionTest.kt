package com.tangem.features.earn.product.api.product

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class DefaultEarnProjectionTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Project {

        @Test
        fun `GIVEN MAC-27 inputs WHEN project THEN total and earned match the vector`() {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal("0.0468"))

            // Act
            val actual = projection.project(INITIAL, TOP_UP, EarnHorizon.Year3)

            // Assert
            assertThat(actual.total).isEqualTo(BigDecimal("22124.34"))
            assertThat(actual.earned).isEqualTo(BigDecimal("1624.34"))
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN net rate and horizon WHEN project THEN total and earned match`(model: ProjectModel) {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal(model.rate))

            // Act
            val actual = projection.project(INITIAL, TOP_UP, model.horizon)

            // Assert
            assertThat(actual.total).isEqualTo(BigDecimal(model.total))
            assertThat(actual.earned).isEqualTo(BigDecimal(model.earned))
        }

        @Test
        fun `GIVEN zero rate WHEN project 3 years THEN total is initial plus all top-ups and earned is zero`() {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal.ZERO)

            // Act
            val actual = projection.project(INITIAL, TOP_UP, EarnHorizon.Year3)

            // Assert
            assertThat(actual.total).isEqualTo(BigDecimal("20500.00"))
            assertThat(actual.earned).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `GIVEN negative rate WHEN project THEN result equals the zero rate result`() {
            // Arrange
            val negative = DefaultEarnProjection(netRate = BigDecimal("-0.05"))
            val zero = DefaultEarnProjection(netRate = BigDecimal.ZERO)

            // Act
            val actual = negative.project(INITIAL, TOP_UP, EarnHorizon.Year1)

            // Assert
            assertThat(actual).isEqualTo(zero.project(INITIAL, TOP_UP, EarnHorizon.Year1))
        }

        @Test
        fun `GIVEN no top-up WHEN project 1 year THEN total is compound interest`() {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal("0.0468"))

            // Act
            val actual = projection.project(INITIAL, BigDecimal.ZERO, EarnHorizon.Year1)

            // Assert
            assertThat(actual.total).isEqualTo(BigDecimal("2617.00"))
            assertThat(actual.earned).isEqualTo(BigDecimal("117.00"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["20", "60", "100", "140", "180", "220", "260", "300", "340", "380", "420", "460", "2500"])
        fun `GIVEN exact half-cent gain WHEN project 1 year without top-up THEN earned agrees with yearlyEstimate`(
            amount: String,
        ) {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal("0.04675"))

            // Act
            val actual = projection.project(BigDecimal(amount), BigDecimal.ZERO, EarnHorizon.Year1)

            // Assert
            assertThat(actual.earned).isEqualTo(projection.yearlyEstimate(BigDecimal(amount)))
        }

        @Test
        fun `GIVEN 2500 at 4 675 percent WHEN project 1 year without top-up THEN total is 2616 88`() {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal("0.04675"))

            // Act
            val actual = projection.project(INITIAL, BigDecimal.ZERO, EarnHorizon.Year1)

            // Assert
            assertThat(actual.total).isEqualTo(BigDecimal("2616.88"))
            assertThat(actual.earned).isEqualTo(BigDecimal("116.88"))
        }

        @Test
        fun `GIVEN 1 year WHEN project THEN 12 points indexed 1 to 12 and the last point equals total and earned`() {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal("0.0468"))

            // Act
            val actual = projection.project(INITIAL, TOP_UP, EarnHorizon.Year1)

            // Assert
            assertThat(actual.points.map { it.index }).isEqualTo((1..12).toList())
            assertThat(actual.points.last().capital).isEqualTo(actual.total)
            assertThat(actual.points.last().income).isEqualTo(actual.earned)
        }

        @ParameterizedTest
        @EnumSource(EarnHorizon::class)
        fun `GIVEN any horizon WHEN project THEN total minus initial minus all top-ups equals earned`(
            horizon: EarnHorizon,
        ) {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal("0.0468"))
            val invested = INITIAL + TOP_UP * BigDecimal(horizon.months)

            // Act
            val actual = projection.project(INITIAL, TOP_UP, horizon)

            // Assert
            assertThat(actual.points).hasSize(horizon.months)
            assertThat(actual.total - invested).isEqualTo(actual.earned)
        }

        @Test
        fun `GIVEN maximum inputs WHEN project 10 years THEN total is finite and above invested capital`() {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal("0.0468"))
            val max = BigDecimal(1_000_000)

            // Act
            val actual = projection.project(max, max, EarnHorizon.Year10)

            // Assert
            assertThat(actual.total).isGreaterThan(BigDecimal(121_000_000))
            assertThat(actual.earned).isGreaterThan(BigDecimal.ZERO)
            assertThat(actual.total.scale()).isEqualTo(2)
        }

        private fun provideTestModels() = listOf(
            ProjectModel(rate = "0.0468", horizon = EarnHorizon.Months6, total = "5586.62", earned = "86.62"),
            ProjectModel(rate = "0.0468", horizon = EarnHorizon.Year1, total = "8744.64", earned = "244.64"),
            ProjectModel(rate = "0.0468", horizon = EarnHorizon.Year3, total = "22124.34", earned = "1624.34"),
            ProjectModel(rate = "0.0468", horizon = EarnHorizon.Year5, total = "36785.68", earned = "4285.68"),
            ProjectModel(rate = "0.0468", horizon = EarnHorizon.Year10, total = "79881.12", earned = "17381.12"),
            ProjectModel(rate = "0.04675", horizon = EarnHorizon.Months6, total = "5586.53", earned = "86.53"),
            ProjectModel(rate = "0.04675", horizon = EarnHorizon.Year1, total = "8744.38", earned = "244.38"),
            ProjectModel(rate = "0.04675", horizon = EarnHorizon.Year3, total = "22122.55", earned = "1622.55"),
            ProjectModel(rate = "0.04675", horizon = EarnHorizon.Year5, total = "36780.83", earned = "4280.83"),
            ProjectModel(rate = "0.04675", horizon = EarnHorizon.Year10, total = "79859.88", earned = "17359.88"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YearlyEstimate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN net rate and amount WHEN yearlyEstimate THEN amount times rate rounded to cents`(
            model: YearlyEstimateModel,
        ) {
            // Arrange
            val projection = DefaultEarnProjection(netRate = BigDecimal(model.rate))

            // Act
            val actual = projection.yearlyEstimate(BigDecimal(model.amount))

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(model.expected))
        }

        private fun provideTestModels() = listOf(
            YearlyEstimateModel(name = "MAC-26", rate = "0.04675", amount = "800", expected = "37.40"),
            YearlyEstimateModel(name = "confirm amount", rate = "0.04675", amount = "2130.88", expected = "99.62"),
            YearlyEstimateModel(name = "negative rate", rate = "-0.05", amount = "800", expected = "0.00"),
        )
    }

    internal data class ProjectModel(val rate: String, val horizon: EarnHorizon, val total: String, val earned: String) {
        override fun toString(): String = "$rate $horizon -> $total / $earned"
    }

    internal data class YearlyEstimateModel(val name: String, val rate: String, val amount: String, val expected: String) {
        override fun toString(): String = "$name -> $expected"
    }

    private companion object {
        val INITIAL = BigDecimal(2500)
        val TOP_UP = BigDecimal(500)
    }
}