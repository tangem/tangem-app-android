package com.tangem.features.earn.impl.widgets.calculator.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorTexts
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnCalculatorUM
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnProjectionBarUM
import com.tangem.features.earn.product.api.product.DefaultEarnProjection
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnProjectionResult
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale

internal class EarnCalculatorConverterTest {

    private val converter = EarnCalculatorConverter()
    private val projection = DefaultEarnProjection(netRate = BigDecimal("0.0468"))

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Nested
    inner class Texts {

        @Test
        fun `GIVEN MAC-27 result and USD WHEN convert THEN total and earned texts`() {
            // Act
            val actual = convert()

            // Assert
            assertThat(actual.total).isEqualTo(stringReference("$22,124.34"))
            assertThat(actual.earned).isEqualTo(stringReference("+$1,624.34"))
            assertThat(actual.earnedLabel).isEqualTo(EarnCalculatorTexts.earnedIn)
        }

        @Test
        fun `GIVEN second currency WHEN convert THEN amounts use its symbol`() {
            // Arrange
            val euro = AppCurrency.Default.copy(code = "EUR", symbol = "€")
            val result = projection.project(CalculatorInput.Default.initialFiat, BigDecimal(500), EarnHorizon.Year3)

            // Act
            val actual = convert(appCurrency = euro)

            // Assert
            assertThat(actual.total).isEqualTo(stringReference(result.total.format { fiat(euro.code, euro.symbol) }))
            assertThat((actual.total as TextReference.Str).value).contains("€")
            assertThat(actual.initialBalance.value).isEqualTo(
                stringReference(CalculatorInput.Default.initialFiat.format { fiat(euro.code, euro.symbol).optionalDecimals() }),
            )
        }
    }

    @Nested
    inner class HorizonSelect {

        @Test
        fun `GIVEN Year3 input WHEN convert THEN five items with Year3 selected`() {
            // Act
            val actual = convert().horizon

            // Assert
            assertThat(actual.selected).isEqualTo(EarnCalculatorTexts.horizon(EarnHorizon.Year3))
            assertThat(actual.items.map { it.label })
                .containsExactlyElementsIn(EarnHorizon.entries.map(EarnCalculatorTexts::horizon))
                .inOrder()
            assertThat(actual.items.map { it.isSelected })
                .containsExactlyElementsIn(EarnHorizon.entries.map { it == EarnHorizon.Year3 })
                .inOrder()
        }

        @Test
        fun `GIVEN items WHEN each is clicked THEN onHorizonSelect receives its horizon`() {
            // Arrange
            val selected = mutableListOf<EarnHorizon>()
            val actual = convert(onHorizonSelect = { selected += it }).horizon

            // Act
            actual.items.forEach { it.onClick() }

            // Assert
            assertThat(selected).containsExactlyElementsIn(EarnHorizon.entries).inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AxisLabels {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN horizon WHEN convert THEN three labels at thirds`(model: AxisModel) {
            // Arrange
            val input = CalculatorInput.Default.copy(horizon = model.horizon)

            // Act
            val actual = convert(input = input).axisLabels

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AxisModel(
                horizon = EarnHorizon.Months6,
                expected = listOf(
                    EarnCalculatorTexts.axisMonths(2),
                    EarnCalculatorTexts.axisMonths(4),
                    EarnCalculatorTexts.axisMonths(6),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year1,
                expected = listOf(
                    EarnCalculatorTexts.axisMonths(4),
                    EarnCalculatorTexts.axisMonths(8),
                    EarnCalculatorTexts.axisYears(1),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year3,
                expected = listOf(
                    EarnCalculatorTexts.axisYears(1),
                    EarnCalculatorTexts.axisYears(2),
                    EarnCalculatorTexts.axisYears(3),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year5,
                expected = listOf(
                    EarnCalculatorTexts.axisMonths(20),
                    EarnCalculatorTexts.axisMonths(40),
                    EarnCalculatorTexts.axisYears(5),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year10,
                expected = listOf(
                    EarnCalculatorTexts.axisMonths(40),
                    EarnCalculatorTexts.axisMonths(80),
                    EarnCalculatorTexts.axisYears(10),
                ),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Bars {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN horizon WHEN convert THEN bars are capped at 24`(model: BarsCountModel) {
            // Arrange
            val input = CalculatorInput.Default.copy(horizon = model.horizon)

            // Act
            val actual = convert(input = input).bars

            // Assert
            assertThat(actual).hasSize(model.expected)
        }

        @Test
        fun `GIVEN amounts above the visible minimum WHEN convert THEN layers follow initial total and top-up`() {
            // Arrange
            val input = CalculatorInput.Default.copy(
                initialFiat = BigDecimal(5_000),
                monthlyTopUpFiat = BigDecimal(5_000),
                horizon = EarnHorizon.Year1,
            )
            val result = projection.project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)
            val tallest = input.initialFiat + result.points.last().capital + input.monthlyTopUpFiat

            // Act
            val actual = convert(input = input, result = result).bars

            // Assert
            val expected = result.points.map { point ->
                EarnProjectionBarUM(
                    initial = input.initialFiat.share(of = tallest),
                    total = point.capital.share(of = tallest),
                    topUp = input.monthlyTopUpFiat.share(of = tallest),
                )
            }
            assertThat(actual).hasSize(expected.size)
            actual.zip(expected).forEach { (bar, expectedBar) ->
                assertThat(bar.initial).isWithin(TOLERANCE).of(expectedBar.initial)
                assertThat(bar.total).isWithin(TOLERANCE).of(expectedBar.total)
                assertThat(bar.topUp).isWithin(TOLERANCE).of(expectedBar.topUp)
            }
        }

        @Test
        fun `GIVEN 120 points WHEN convert THEN bar k shows the total of month 5 times k`() {
            // Arrange
            val input = CalculatorInput.Default.copy(horizon = EarnHorizon.Year10)
            val result = projection.project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)

            // Act
            val actual = convert(input = input, result = result).bars

            // Assert
            actual.forEachIndexed { index, bar ->
                val point = result.points[5 * (index + 1) - 1]
                assertThat(bar.total / actual.last().total)
                    .isWithin(TOLERANCE)
                    .of(point.capital.share(of = result.total))
            }
        }

        @Test
        fun `GIVEN a tiny monthly top-up WHEN convert THEN its layer stays visible`() {
            // Arrange
            val input = CalculatorInput.Default.copy(monthlyTopUpFiat = BigDecimal.ONE)

            // Act
            val actual = convert(input = input).bars

            // Assert
            assertThat(actual.map { it.topUp }.min()).isAtLeast(MIN_VISIBLE_SHARE)
        }

        @Test
        fun `GIVEN a visible minimum WHEN convert THEN the tallest bar is exactly one`() {
            // Act
            val actual = convert().bars

            // Assert
            assertThat(actual.maxOf { it.initial + it.total + it.topUp }).isWithin(TOLERANCE).of(1f)
        }

        @Test
        fun `GIVEN zero monthly top-up WHEN convert THEN the top-up layer is empty`() {
            // Arrange
            val input = CalculatorInput.Default.copy(monthlyTopUpFiat = BigDecimal.ZERO)

            // Act
            val actual = convert(input = input).bars

            // Assert
            assertThat(actual.map { it.topUp }.toSet()).containsExactly(0f)
        }

        @Test
        fun `GIVEN zero inputs WHEN convert THEN all bar fractions are zero`() {
            // Arrange
            val input = CalculatorInput.Default.copy(initialFiat = BigDecimal.ZERO, monthlyTopUpFiat = BigDecimal.ZERO)

            // Act
            val actual = convert(input = input)

            // Assert
            assertThat(actual.bars).hasSize(24)
            assertThat(actual.bars.map { listOf(it.initial, it.total, it.topUp) }.flatten().toSet()).containsExactly(0f)
            assertThat(actual.total).isEqualTo(stringReference("$0.00"))
        }

        private fun provideTestModels() = listOf(
            BarsCountModel(horizon = EarnHorizon.Months6, expected = 6),
            BarsCountModel(horizon = EarnHorizon.Year1, expected = 12),
            BarsCountModel(horizon = EarnHorizon.Year3, expected = 24),
            BarsCountModel(horizon = EarnHorizon.Year5, expected = 24),
            BarsCountModel(horizon = EarnHorizon.Year10, expected = 24),
        )

        private fun BigDecimal.share(of: BigDecimal): Float = divide(of, MathContext.DECIMAL64).toFloat()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Incentive {

        @Test
        fun `GIVEN no incentive rate WHEN convert THEN no badge`() {
            // Act
            val actual = convert(incentiveRate = null).incentive

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN zero incentive rate WHEN convert THEN no badge`() {
            // Arrange
            val rate = EarnProductRate(fraction = BigDecimal.ZERO, type = EarnRateType.APY)

            // Act
            val actual = convert(incentiveRate = rate).incentive

            // Assert
            assertThat(actual).isNull()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN incentive rate WHEN convert THEN info badge with percent`(model: IncentiveModel) {
            // Arrange
            val rate = EarnProductRate(fraction = BigDecimal(model.fraction), type = EarnRateType.APY)

            // Act
            val actual = convert(incentiveRate = rate).incentive

            // Assert
            val expected = TangemBadgeUM(
                text = resourceReference(
                    id = R.string.yield_module_promo_screen_incentivised_chip,
                    formatArgs = wrappedList(model.expectedPercent),
                ),
                status = TangemBadge.Status.Info,
                size = TangemBadge.Size.X6,
            )
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            IncentiveModel(fraction = "0.014", expectedPercent = "1.4%"),
            IncentiveModel(fraction = "0.0125", expectedPercent = "1.25%"),
            IncentiveModel(fraction = "0.02", expectedPercent = "2%"),
            IncentiveModel(fraction = "0.00001", expectedPercent = "<0.01%"),
        )
    }

    @Nested
    inner class Steppers {

        @Test
        fun `GIVEN Default input WHEN convert THEN stepper values labels and descriptions`() {
            // Act
            val actual = convert()

            // Assert
            assertThat(actual.initialBalance.label).isEqualTo(EarnCalculatorTexts.initialBalance)
            assertThat(actual.initialBalance.value).isEqualTo(stringReference("$2,500"))
            assertThat(actual.initialBalance.minusDescription)
                .isEqualTo(EarnCalculatorTexts.decrease(EarnCalculatorTexts.initialBalance))
            assertThat(actual.initialBalance.plusDescription)
                .isEqualTo(EarnCalculatorTexts.increase(EarnCalculatorTexts.initialBalance))
            assertThat(actual.monthlyTopUp.label).isEqualTo(EarnCalculatorTexts.monthlyTopUp)
            assertThat(actual.monthlyTopUp.value).isEqualTo(stringReference("$500"))
            assertThat(actual.monthlyTopUp.minusDescription)
                .isEqualTo(EarnCalculatorTexts.decrease(EarnCalculatorTexts.monthlyTopUp))
            assertThat(actual.monthlyTopUp.plusDescription)
                .isEqualTo(EarnCalculatorTexts.increase(EarnCalculatorTexts.monthlyTopUp))
            assertThat(listOf(actual.initialBalance, actual.monthlyTopUp).flatMap { listOf(it.isMinusEnabled, it.isPlusEnabled) })
                .containsExactly(true, true, true, true)
        }

        @Test
        fun `GIVEN values at their bounds WHEN convert THEN the matching buttons are disabled`() {
            // Arrange
            val atMinimum = CalculatorInput.Default.copy(initialFiat = BigDecimal.ZERO, monthlyTopUpFiat = BigDecimal.ZERO)
            val atMaximum = CalculatorInput.Default.copy(
                initialFiat = BigDecimal(1_000_000),
                monthlyTopUpFiat = BigDecimal(1_000_000),
            )

            // Act
            val minimum = convert(input = atMinimum)
            val maximum = convert(input = atMaximum)

            // Assert
            assertThat(listOf(minimum.initialBalance.isMinusEnabled, minimum.monthlyTopUp.isMinusEnabled))
                .containsExactly(false, false)
            assertThat(listOf(minimum.initialBalance.isPlusEnabled, minimum.monthlyTopUp.isPlusEnabled))
                .containsExactly(true, true)
            assertThat(listOf(maximum.initialBalance.isPlusEnabled, maximum.monthlyTopUp.isPlusEnabled))
                .containsExactly(false, false)
            assertThat(listOf(maximum.initialBalance.isMinusEnabled, maximum.monthlyTopUp.isMinusEnabled))
                .containsExactly(true, true)
        }

        @Test
        fun `GIVEN steppers WHEN minus and plus are clicked THEN the step callbacks get -1 and 1`() {
            // Arrange
            val initialSteps = mutableListOf<Int>()
            val topUpSteps = mutableListOf<Int>()
            val actual = convert(onInitialStep = { initialSteps += it }, onTopUpStep = { topUpSteps += it })

            // Act
            actual.initialBalance.onMinus()
            actual.initialBalance.onPlus()
            actual.monthlyTopUp.onMinus()
            actual.monthlyTopUp.onPlus()

            // Assert
            assertThat(initialSteps).containsExactly(-1, 1).inOrder()
            assertThat(topUpSteps).containsExactly(-1, 1).inOrder()
        }
    }

    internal data class AxisModel(val horizon: EarnHorizon, val expected: List<TextReference>) {
        override fun toString(): String = "$horizon"
    }

    internal data class BarsCountModel(val horizon: EarnHorizon, val expected: Int) {
        override fun toString(): String = "$horizon -> $expected bars"
    }

    internal data class IncentiveModel(val fraction: String, val expectedPercent: String) {
        override fun toString(): String = "$fraction -> $expectedPercent"
    }

    @Suppress("LongParameterList")
    private fun convert(
        input: CalculatorInput = CalculatorInput.Default,
        result: EarnProjectionResult = projection.project(input.initialFiat, input.monthlyTopUpFiat, input.horizon),
        appCurrency: AppCurrency = AppCurrency.Default,
        incentiveRate: EarnProductRate? = null,
        onInitialStep: (Int) -> Unit = {},
        onTopUpStep: (Int) -> Unit = {},
        onHorizonSelect: (EarnHorizon) -> Unit = {},
    ): EarnCalculatorUM = converter.convert(
        result = result,
        input = input,
        appCurrency = appCurrency,
        incentiveRate = incentiveRate,
        onInitialStep = onInitialStep,
        onTopUpStep = onTopUpStep,
        onHorizonSelect = onHorizonSelect,
    )

    private companion object {
        const val TOLERANCE = 1e-5f
        const val MIN_VISIBLE_SHARE = 0.04f
    }
}