package com.tangem.features.tangempay.cashback.impl.model

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackHistogramUM.Style
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Locale

@Suppress("MagicNumber")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCashbackHistogramConverterTest {

    private val defaultLocale = Locale.getDefault()
    private val converter = TangemPayCashbackHistogramConverter()

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN history WHEN convert THEN total sums months and last month highlighted`() {
        // Arrange
        val history = CashbackHistory(
            currency = "USD",
            months = listOf(
                month(2, BigDecimal("12.02")),
                month(3, BigDecimal("44.22")),
                month(6, BigDecimal("32.15")),
            ),
        )

        // Act
        val actual = converter.convert(history)

        // Assert
        val expected = TangemPayCashbackHistogramUM(
            title = stringReference("$88.39 earned in total"),
            bars = persistentListOf(
                bar("Feb", "$12.02", 12.02f, Style.Regular),
                bar("Mar", "$44.22", 44.22f, Style.Regular),
                bar("Jun", "$32.15", 32.15f, Style.Highlighted),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN zero amounts WHEN convert THEN empty total and last month still highlighted`() {
        // Arrange
        val history = CashbackHistory(
            currency = "USD",
            months = listOf(month(5, BigDecimal.ZERO), month(6, BigDecimal.ZERO)),
        )

        // Act
        val actual = converter.convert(history)

        // Assert
        assertThat(actual.title).isEqualTo(stringReference("$0 earned in total"))
        assertThat(actual.bars.map { it.style }).containsExactly(Style.Regular, Style.Highlighted).inOrder()
        assertThat(actual.bars.last().amount).isEqualTo(stringReference("$0.00"))
    }

    @Test
    fun `GIVEN negative last month WHEN convert THEN last bar highlighted negative`() {
        // Arrange
        val history = CashbackHistory(
            currency = "USD",
            months = listOf(month(5, BigDecimal("26.10")), month(6, BigDecimal("-2.15"))),
        )

        // Act
        val actual = converter.convert(history)

        // Assert
        assertThat(actual.title).isEqualTo(stringReference("$23.95 earned in total"))
        assertThat(actual.bars.last().style).isEqualTo(Style.HighlightedNegative)
        assertThat(actual.bars.last().amount).isEqualTo(stringReference("-$2.15"))
    }

    private fun bar(month: String, amount: String, value: Float, style: Style) =
        TangemPayCashbackHistogramUM.Bar(stringReference(month), stringReference(amount), value, style)

    private fun month(month: Int, amount: BigDecimal) =
        CashbackHistory.MonthlyCashback(year = 2026, month = month, confirmedAmount = amount)
}