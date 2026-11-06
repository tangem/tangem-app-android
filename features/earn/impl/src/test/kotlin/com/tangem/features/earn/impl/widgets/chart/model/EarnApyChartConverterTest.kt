package com.tangem.features.earn.impl.widgets.chart.model

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartComponent
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartTexts
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyAverageUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyBarUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyChartUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyScale
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

internal class EarnApyChartConverterTest {

    private val converter = EarnApyChartConverter()

    private val periods = listOf(
        EarnPeriod(id = "1m", label = stringReference("1M")),
        EarnPeriod(id = "3m", label = stringReference("3M")),
        EarnPeriod(id = "6m", label = stringReference("6M")),
    )

    private val history = EarnApyHistory(
        points = listOf(
            point(at = "2026-01-10T12:00:00Z", fraction = "0.03"),
            point(at = "2026-02-10T12:00:00Z", fraction = "0.0512"),
            point(at = "2026-03-10T12:00:00Z", fraction = "0.09"),
        ),
        average = BigDecimal("0.0512"),
    )

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

    @Test
    fun `GIVEN history WHEN convert THEN bars carry percent values and tooltips`() {
        // Act
        val actual = convert(history)

        // Assert
        assertThat(actual.bars).containsExactly(
            EarnApyBarUM(value = 3.0, tooltip = stringReference("3.00% · Jan 10, 2026")),
            EarnApyBarUM(value = 5.12, tooltip = stringReference("5.12% · Feb 10, 2026")),
            EarnApyBarUM(value = 9.0, tooltip = stringReference("9.00% · Mar 10, 2026")),
        ).inOrder()
    }

    @Test
    fun `GIVEN history WHEN convert THEN scale is 0 to 9 by 3 and month labels follow the dates`() {
        // Act
        val actual = convert(history)

        // Assert
        assertThat(actual.scale).isEqualTo(EarnApyScale(max = 9.0, step = 3.0, decimals = 0))
        assertThat(actual.xLabels).containsExactly("Jan", "Feb", "Mar", "Apr").inOrder()
    }

    @Test
    fun `GIVEN points newest first WHEN convert THEN bars and month labels run oldest first`() {
        // Arrange
        val newestFirst = history.copy(points = history.points.reversed())

        // Act
        val actual = convert(newestFirst)

        // Assert
        assertThat(actual.bars.map(EarnApyBarUM::value)).containsExactly(3.0, 5.12, 9.0).inOrder()
        assertThat(actual.xLabels).containsExactly("Jan", "Feb", "Mar", "Apr").inOrder()
    }

    @Test
    fun `GIVEN average WHEN convert THEN average carries the value, the Avg text and the header block`() {
        // Act
        val actual = convert(history)

        // Assert
        val expected = EarnApyAverageUM(
            value = 5.12,
            text = resourceReference(R.string.yield_module_rate_info_sheet_chart_average, wrappedList("5.12%")),
            title = EarnApyChartTexts.averageApy,
            rate = stringReference("5.12%"),
        )
        assertThat(actual.average).isEqualTo(expected)
    }

    @Test
    fun `GIVEN no average WHEN convert THEN there is no average`() {
        // Act
        val actual = convert(history.copy(average = null))

        // Assert
        assertThat(actual.average).isNull()
    }

    @Test
    fun `GIVEN current rate WHEN convert THEN header shows it with a percent sign`() {
        // Act
        val actual = convert(history)

        // Assert
        assertThat(actual.currentRate).isEqualTo(stringReference("5.50%"))
    }

    @Test
    fun `GIVEN periods and 3M selected WHEN convert THEN three items with only the second selected`() {
        // Act
        val actual = convert(history, period = periods[1])

        // Assert
        assertThat(actual.periods.map { it.id }).containsExactly("1m", "3m", "6m").inOrder()
        assertThat(actual.periods.map { it.label }).containsExactlyElementsIn(periods.map { it.label }).inOrder()
        assertThat(actual.periods.map { it.isSelected }).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun `GIVEN a period with the id of an item but another label WHEN convert THEN that item is selected`() {
        // Arrange
        val sameIdOtherLabel = EarnPeriod(id = "3m", label = stringReference("Three months"))

        // Act
        val actual = convert(history, period = sameIdOtherLabel)

        // Assert
        assertThat(actual.periods.map { it.isSelected }).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun `GIVEN a period item WHEN clicked THEN onPeriodClick gets that period`() {
        // Arrange
        val clicked = mutableListOf<EarnPeriod>()
        val actual = convert(history, onPeriodClick = { clicked += it })

        // Act
        actual.periods[0].onClick()

        // Assert
        assertThat(clicked).containsExactly(periods[0])
    }

    @Test
    fun `GIVEN bar selection WHEN onBarSelected THEN the handler gets the index`() {
        // Arrange
        val selected = mutableListOf<Int?>()
        val actual = convert(history, onBarSelected = { selected += it })

        // Act
        actual.onBarSelected(2)
        actual.onBarSelected(null)

        // Assert
        assertThat(selected).containsExactly(2, null).inOrder()
    }

    @Test
    fun `GIVEN loading WHEN loading THEN header and selected period stay and there is no chart`() {
        // Act
        val actual = converter.loading(params(), periods[1], onPeriodClick = {}, onBarSelected = {})

        // Assert
        assertThat(actual.isLoading).isTrue()
        assertThat(actual.bars).isEmpty()
        assertThat(actual.average).isNull()
        assertThat(actual.selectedBar).isNull()
        assertThat(actual.currentRate).isEqualTo(stringReference("5.50%"))
        assertThat(actual.periods.map { it.isSelected }).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun `GIVEN failure WHEN failed THEN empty chart that is not loading`() {
        // Act
        val actual = converter.failed(params(), periods[1], onPeriodClick = {}, onBarSelected = {})

        // Assert
        assertThat(actual.isLoading).isFalse()
        assertThat(actual.bars).isEmpty()
        assertThat(actual.average).isNull()
        assertThat(actual.xLabels).isEmpty()
        assertThat(actual.scale).isEqualTo(EarnApyScale(max = 3.0, step = 1.0, decimals = 0))
    }

    @Test
    fun `GIVEN empty history WHEN convert THEN the chart is empty and not loading`() {
        // Act
        val actual = convert(EarnApyHistory(points = emptyList(), average = null))

        // Assert
        assertThat(actual.isLoading).isFalse()
        assertThat(actual.bars).isEmpty()
        assertThat(actual.average).isNull()
        assertThat(actual.xLabels).isEmpty()
        assertThat(actual.scale).isEqualTo(EarnApyScale(max = 3.0, step = 1.0, decimals = 0))
    }

    @Test
    fun `GIVEN no callback WHEN convert THEN there is no calculator button`() {
        // Act
        val actual = convert(history, params = params(callback = null))

        // Assert
        assertThat(actual.onCalculatorClick).isNull()
    }

    @Test
    fun `GIVEN a callback WHEN onCalculatorClick THEN the callback is called once`() {
        // Arrange
        val callback = mockk<EarnApyChartComponent.ModelCallback>(relaxed = true)
        val actual = convert(history, params = params(callback = callback))

        // Act
        requireNotNull(actual.onCalculatorClick).invoke()

        // Assert
        verify(exactly = 1) { callback.onCalculatorClick() }
    }

    @Test
    fun `GIVEN one point WHEN convert THEN one bar and no exception`() {
        // Act
        val actual = convert(history.copy(points = history.points.take(1)))

        // Assert
        assertThat(actual.bars).hasSize(1)
    }

    @Test
    fun `GIVEN zero rates and zero average WHEN convert THEN fallback scale and a zero average text`() {
        // Arrange
        val zero = EarnApyHistory(
            points = history.points.map { it.copy(rate = it.rate.copy(fraction = BigDecimal.ZERO)) },
            average = BigDecimal.ZERO,
        )

        // Act
        val actual = convert(zero)

        // Assert
        assertThat(actual.bars.map { it.value }).containsExactly(0.0, 0.0, 0.0)
        assertThat(actual.scale).isEqualTo(EarnApyScale(max = 3.0, step = 1.0, decimals = 0))
        assertThat(actual.average?.text).isEqualTo(
            resourceReference(R.string.yield_module_rate_info_sheet_chart_average, wrappedList("0.00%")),
        )
    }

    @Test
    fun `GIVEN a point just before midnight UTC WHEN convert in UTC plus 3 THEN the tooltip date is the next day`() {
        // Arrange
        val late = EarnApyHistory(points = listOf(point("2026-11-05T23:30:00Z", "0.03")), average = null)

        // Act
        val inMoscow = convert(late, timeZone = TimeZone.of("Europe/Moscow"))
        val inUtc = convert(late, timeZone = TimeZone.UTC)

        // Assert
        assertThat(inMoscow.bars.single().tooltip).isEqualTo(stringReference("3.00% · Nov 6, 2026"))
        assertThat(inUtc.bars.single().tooltip).isEqualTo(stringReference("3.00% · Nov 5, 2026"))
    }

    @Test
    fun `GIVEN German locale and US default WHEN convert THEN every rate and the date follow the given locale`() {
        // Act
        val actual = convert(history, locale = Locale.GERMANY)

        // Assert
        assertThat(actual.bars.first().tooltip).isEqualTo(stringReference("3,00% · 10.01.2026"))
        assertThat(actual.currentRate).isEqualTo(stringReference("5,50%"))
        assertThat(actual.average?.text)
            .isEqualTo(resourceReference(R.string.yield_module_rate_info_sheet_chart_average, wrappedList("5,12%")))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RateTitle {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN rate type WHEN convert THEN titles say APY or APR`(model: RateTitleModel) {
            // Act
            val actual = convert(history, params = params(rate = rate("0.055", model.type)))

            // Assert
            assertThat(actual.rateTitle).isEqualTo(model.expectedCurrent)
            assertThat(actual.average?.title).isEqualTo(model.expectedAverage)
        }

        private fun provideTestModels() = listOf(
            RateTitleModel(
                type = EarnRateType.APY,
                expectedCurrent = EarnApyChartTexts.currentApy,
                expectedAverage = EarnApyChartTexts.averageApy,
            ),
            RateTitleModel(
                type = EarnRateType.APR,
                expectedCurrent = EarnApyChartTexts.currentApr,
                expectedAverage = EarnApyChartTexts.averageApr,
            ),
        )
    }

    data class RateTitleModel(
        val type: EarnRateType,
        val expectedCurrent: TextReference,
        val expectedAverage: TextReference,
    )

    private fun convert(
        history: EarnApyHistory,
        period: EarnPeriod = periods[2],
        params: EarnApyChartComponent.Params = params(),
        timeZone: TimeZone = TimeZone.UTC,
        locale: Locale = Locale.US,
        onPeriodClick: (EarnPeriod) -> Unit = {},
        onBarSelected: (Int?) -> Unit = {},
    ): EarnApyChartUM = converter.convert(
        params = params,
        period = period,
        history = history,
        onPeriodClick = onPeriodClick,
        onBarSelected = onBarSelected,
        timeZone = timeZone,
        locale = locale,
    )

    private fun params(
        callback: EarnApyChartComponent.ModelCallback? = null,
        rate: EarnProductRate = rate("0.055", EarnRateType.APY),
    ) = EarnApyChartComponent.Params(
        periods = periods,
        defaultPeriod = periods[2],
        history = { IllegalStateException("unused").left() },
        currentRate = rate,
        callback = callback,
    )

    private fun rate(fraction: String, type: EarnRateType) = EarnProductRate(BigDecimal(fraction), type)

    private fun point(at: String, fraction: String) = EarnApyPoint(
        at = Instant.parse(at),
        rate = rate(fraction, EarnRateType.APY),
    )
}