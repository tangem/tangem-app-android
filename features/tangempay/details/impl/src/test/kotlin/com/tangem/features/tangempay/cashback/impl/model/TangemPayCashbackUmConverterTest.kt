package com.tangem.features.tangempay.cashback.impl.model

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackUM
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCashbackUmConverterTest {

    private val defaultLocale = Locale.getDefault()

    private val converter = TangemPayCashbackUmConverter()

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

    @ParameterizedTest
    @MethodSource("emptyStateCashback")
    fun `GIVEN null or zero amount WHEN convert THEN empty state without banner`(cashback: TangemPayCashback?) {
        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = stringReference("Start spending and earn cashback"),
            subtitle = stringReference("Collected amount will be shown here"),
            isEmpty = true,
            banner = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN positive amount WHEN convert THEN earned title and info deposit banner`() {
        // Arrange
        val cashback = createCashback(confirmedAmount = BigDecimal("22.54"))

        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = stringReference("$22.54 earned in June"),
            subtitle = stringReference("Will be deposited on July 1–5"),
            isEmpty = false,
            banner = TangemPayCashbackUM.Banner(
                text = stringReference("Cashback $22.54 for June will be deposited till July 5"),
                type = TangemPayCashbackUM.Banner.Type.Info,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN negative amount WHEN convert THEN earned title and refund error banner`() {
        // Arrange
        val cashback = createCashback(confirmedAmount = BigDecimal("-22.54"))

        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = stringReference("-$22.54 earned in June"),
            subtitle = stringReference("Will be deposited on July 1–5"),
            isEmpty = false,
            banner = TangemPayCashbackUM.Banner(
                text = stringReference(
                    "We received a refund for a purchase for which cashback had previously been awarded",
                ),
                type = TangemPayCashbackUM.Banner.Type.Error,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN payout window spanning two months WHEN convert THEN subtitle shows month on both sides`() {
        // Arrange
        val cashback = createCashback(
            payoutStart = DateTime.parse("2026-07-30"),
            payoutEnd = DateTime.parse("2026-08-02"),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = stringReference("$22.54 earned in June"),
            subtitle = stringReference("Will be deposited on July 30 – August 2"),
            isEmpty = false,
            banner = TangemPayCashbackUM.Banner(
                text = stringReference("Cashback $22.54 for June will be deposited till August 2"),
                type = TangemPayCashbackUM.Banner.Type.Info,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun emptyStateCashback(): List<TangemPayCashback?> = listOf(
        null,
        createCashback(confirmedAmount = BigDecimal.ZERO),
    )

    private fun createCashback(
        confirmedAmount: BigDecimal = BigDecimal("22.54"),
        currency: String = "USD",
        year: Int = 2026,
        month: Int = 6,
        payoutStart: DateTime = DateTime.parse("2026-07-01"),
        payoutEnd: DateTime = DateTime.parse("2026-07-05"),
    ): TangemPayCashback = TangemPayCashback(
        confirmedAmount = confirmedAmount,
        pendingAmount = BigDecimal.ZERO,
        currency = currency,
        payoutCurrency = "USDC",
        payoutNetwork = "Polygon",
        period = TangemPayCashback.Period(
            year = year,
            month = month,
            payoutStart = payoutStart,
            payoutEnd = payoutEnd,
        ),
    )
}