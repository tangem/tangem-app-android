package com.tangem.features.earn.impl.widgets.calculator.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.R
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
            assertThat(actual.earnedLabel).isEqualTo(resourceReference(R.string.earn_calculator_earned_in_label))
        }

        @Test
        fun `GIVEN the design example WHEN convert THEN total and earned match the design`() {
            // Arrange
            val input = CalculatorInput.Default
            val designProjection = DefaultEarnProjection(netRate = BigDecimal("0.012155"))
            val result = designProjection.project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)

            // Act
            val actual = convert(input = input, result = result)

            // Assert
            assertThat(actual.total).isEqualTo(stringReference("$20,913.23"))
            assertThat(actual.earned).isEqualTo(stringReference("+$413.23"))
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
            assertThat(actual.selected).isEqualTo(years(3))
            assertThat(actual.items.map { it.label })
                .containsExactly(months(6), years(1), years(3), years(5), years(10))
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
                    axisMonths(2),
                    axisMonths(4),
                    axisMonths(6),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year1,
                expected = listOf(
                    axisMonths(4),
                    axisMonths(8),
                    axisYears(1),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year3,
                expected = listOf(
                    axisYears(1),
                    axisYears(2),
                    axisYears(3),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year5,
                expected = listOf(
                    axisMonths(20),
                    axisMonths(40),
                    axisYears(5),
                ),
            ),
            AxisModel(
                horizon = EarnHorizon.Year10,
                expected = listOf(
                    axisMonths(40),
                    axisMonths(80),
                    axisYears(10),
                ),
            ),
        )
    }

    @Nested
    inner class BarTexts {

        @Test
        fun `GIVEN default input WHEN convert THEN the last bar shows the header total and earned`() {
            // Act
            val actual = convert().bars.last()

            // Assert
            val expected = EarnProjectionBarUM(
                height = 1f,
                balance = stringReference("$22,124.34"),
                earned = stringReference("+$1,624.34"),
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN 120 points WHEN convert THEN the first bar shows month 5`() {
            // Arrange
            val input = CalculatorInput.Default.copy(horizon = EarnHorizon.Year10)
            val result = projection.project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)
            val month5 = result.points[4]

            // Act
            val actual = convert(input = input, result = result).bars.first()

            // Assert
            val usd = AppCurrency.Default
            assertThat(actual.balance).isEqualTo(stringReference(month5.capital.format { fiat(usd.code, usd.symbol) }))
            assertThat(actual.earned)
                .isEqualTo(stringReference("+" + month5.income.format { fiat(usd.code, usd.symbol) }))
        }

        @Test
        fun `GIVEN second currency WHEN convert THEN bar texts use its symbol`() {
            // Arrange
            val euro = AppCurrency.Default.copy(code = "EUR", symbol = "€")

            // Act
            val actual = convert(appCurrency = euro).bars.last()

            // Assert
            assertThat((actual.balance as TextReference.Str).value).contains("€")
            assertThat((actual.earned as TextReference.Str).value).contains("€")
        }
    }

    @Nested
    inner class BarSelection {

        @Test
        fun `GIVEN any input WHEN convert THEN nothing is selected`() {
            // Act
            val actual = convert()

            // Assert
            assertThat(actual.selectedBar).isNull()
        }

        @Test
        fun `GIVEN callback WHEN onBarSelected is invoked THEN the callback receives the index`() {
            // Arrange
            val selected = mutableListOf<Int?>()
            val actual = convert(onBarSelected = { selected += it })

            // Act
            actual.onBarSelected(5)
            actual.onBarSelected(null)

            // Assert
            assertThat(selected).containsExactly(5, null).inOrder()
        }
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
        fun `GIVEN 120 points WHEN convert THEN bar k is the income of month 5 times k as a share of the largest income`() {
            // Arrange
            val input = CalculatorInput.Default.copy(horizon = EarnHorizon.Year10)
            val result = projectionOf("0.0553").project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)
            val largest = result.points.maxOf { it.income }

            // Act
            val actual = convert(input = input, result = result).bars

            // Assert
            val checked = actual.withIndex().filter { (index, _) ->
                result.points[5 * (index + 1) - 1].income.share(of = largest) > ABOVE_MINIMUM_SHARE
            }
            assertThat(checked.size).isAtLeast(12)
            checked.forEach { (index, bar) ->
                val point = result.points[5 * (index + 1) - 1]
                assertThat(bar.height).isWithin(TOLERANCE).of(point.income.share(of = largest))
            }
        }

        @Test
        fun `GIVEN the design example WHEN convert THEN bars never shrink and the last one is full height`() {
            // Arrange
            val input = CalculatorInput.Default
            val result = projectionOf("0.012155").project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)

            // Act
            val actual = convert(input = input, result = result).bars.map { it.height }

            // Assert
            assertThat(actual).isInOrder()
            assertThat(actual.last()).isEqualTo(1f)
        }

        @Test
        fun `GIVEN a small earned amount WHEN convert THEN every bar stays visible`() {
            // Arrange
            val input = CalculatorInput.Default
            val result = projectionOf("0.012155").project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)

            // Act
            val actual = convert(input = input, result = result).bars

            // Assert
            assertThat(actual.map { it.height }.min()).isAtLeast(MIN_VISIBLE_SHARE)
        }

        @Test
        fun `GIVEN a zero rate WHEN convert THEN every bar is empty`() {
            // Arrange
            val input = CalculatorInput.Default
            val result = projectionOf("0").project(input.initialFiat, input.monthlyTopUpFiat, input.horizon)

            // Act
            val actual = convert(input = input, result = result).bars

            // Assert
            assertThat(actual.map { it.height }.toSet()).containsExactly(0f)
        }

        @Test
        fun `GIVEN zero inputs WHEN convert THEN every bar is empty`() {
            // Arrange
            val input = CalculatorInput.Default.copy(initialFiat = BigDecimal.ZERO, monthlyTopUpFiat = BigDecimal.ZERO)

            // Act
            val actual = convert(input = input)

            // Assert
            assertThat(actual.bars).hasSize(24)
            assertThat(actual.bars.map { it.height }.toSet()).containsExactly(0f)
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

        private fun projectionOf(netRate: String) = DefaultEarnProjection(netRate = BigDecimal(netRate))
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
                iconEnd = TangemIconUM.Icon(Icons.ic_info_16),
            )
            assertThat(actual?.badge).isEqualTo(expected)
        }

        @Test
        fun `GIVEN incentive rate WHEN badge clicked THEN onIncentiveClick is called once`() {
            // Arrange
            var clicks = 0
            val rate = EarnProductRate(fraction = BigDecimal("0.014"), type = EarnRateType.APY)
            val actual = convert(incentiveRate = rate, onIncentiveClick = { clicks++ }).incentive

            // Act
            actual?.onClick?.invoke()

            // Assert
            assertThat(clicks).isEqualTo(1)
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
            assertThat(actual.initialBalance.label).isEqualTo(initialBalance)
            assertThat(actual.initialBalance.value).isEqualTo(stringReference("$2,500"))
            assertThat(actual.initialBalance.minusDescription)
                .isEqualTo(decrease(initialBalance))
            assertThat(actual.initialBalance.plusDescription)
                .isEqualTo(increase(initialBalance))
            assertThat(actual.monthlyTopUp.label).isEqualTo(monthlyTopUp)
            assertThat(actual.monthlyTopUp.value).isEqualTo(stringReference("$500"))
            assertThat(actual.monthlyTopUp.minusDescription)
                .isEqualTo(decrease(monthlyTopUp))
            assertThat(actual.monthlyTopUp.plusDescription)
                .isEqualTo(increase(monthlyTopUp))
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

    private val initialBalance = resourceReference(R.string.earn_calculator_initial_balance)

    private val monthlyTopUp = resourceReference(R.string.earn_calculator_monthly_top_up)

    private fun months(count: Int) =
        pluralReference(R.plurals.earn_calculator_months, count = count, formatArgs = wrappedList(count))

    private fun years(count: Int) =
        pluralReference(R.plurals.earn_calculator_years, count = count, formatArgs = wrappedList(count))

    private fun axisMonths(count: Int) =
        resourceReference(R.string.earn_calculator_axis_months, wrappedList(count))

    private fun axisYears(count: Int) =
        resourceReference(R.string.earn_calculator_axis_years, wrappedList(count))

    private fun decrease(label: TextReference) =
        resourceReference(R.string.earn_calculator_decrease_accessibility, wrappedList(label))

    private fun increase(label: TextReference) =
        resourceReference(R.string.earn_calculator_increase_accessibility, wrappedList(label))

    @Suppress("LongParameterList")
    private fun convert(
        input: CalculatorInput = CalculatorInput.Default,
        result: EarnProjectionResult = projection.project(input.initialFiat, input.monthlyTopUpFiat, input.horizon),
        appCurrency: AppCurrency = AppCurrency.Default,
        incentiveRate: EarnProductRate? = null,
        onIncentiveClick: () -> Unit = {},
        onInitialStep: (Int) -> Unit = {},
        onTopUpStep: (Int) -> Unit = {},
        onHorizonSelect: (EarnHorizon) -> Unit = {},
        onBarSelected: (Int?) -> Unit = {},
    ): EarnCalculatorUM = converter.convert(
        result = result,
        input = input,
        appCurrency = appCurrency,
        incentiveRate = incentiveRate,
        onIncentiveClick = onIncentiveClick,
        onInitialStep = onInitialStep,
        onTopUpStep = onTopUpStep,
        onHorizonSelect = onHorizonSelect,
        onBarSelected = onBarSelected,
    )

    private companion object {
        const val TOLERANCE = 1e-5f
        const val MIN_VISIBLE_SHARE = 0.04f
        const val ABOVE_MINIMUM_SHARE = 0.06f
    }
}