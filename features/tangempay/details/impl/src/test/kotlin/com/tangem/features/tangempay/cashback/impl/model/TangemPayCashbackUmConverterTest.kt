package com.tangem.features.tangempay.cashback.impl.model

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.arrayItemReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackUM
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
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
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers {
            val skeleton = secondArg<String>()
            if (skeleton == "d MMMM") "MMMM d" else skeleton
        }
        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.formatDateRange(any(), any(), any()) } returns PAYOUT_WINDOW
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        unmockkObject(DateTimeFormatters)
        Locale.setDefault(defaultLocale)
    }

    @ParameterizedTest
    @MethodSource("emptyStateCashback")
    fun `GIVEN null or zero amount WHEN convert THEN empty state without banner`(cashback: TangemPayCashback?) {
        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = resourceReference(R.string.tangempay_cashback_empty_title),
            subtitle = resourceReference(R.string.tangempay_cashback_empty_subtitle),
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
            title = resourceReference(
                R.string.tangempay_cashback_earned_title,
                wrappedList("$22.54", arrayItemReference(R.array.common_month_in, index = 5)),
            ),
            subtitle = resourceReference(R.string.tangempay_cashback_deposited_on, wrappedList(PAYOUT_WINDOW)),
            isEmpty = false,
            banner = TangemPayCashbackUM.Banner(
                text = resourceReference(
                    id = R.string.tangempay_cashback_deposit_banner,
                    formatArgs = wrappedList("$22.54", "June", "July 5"),
                ),
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
            title = resourceReference(
                R.string.tangempay_cashback_earned_title,
                wrappedList("-$22.54", arrayItemReference(R.array.common_month_in, index = 5)),
            ),
            subtitle = resourceReference(R.string.tangempay_cashback_deposited_on, wrappedList(PAYOUT_WINDOW)),
            isEmpty = false,
            banner = TangemPayCashbackUM.Banner(
                text = resourceReference(R.string.tangempay_cashback_refund_banner),
                type = TangemPayCashbackUM.Banner.Type.Error,
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

    private companion object {
        const val PAYOUT_WINDOW = "July 1 – 5"
    }
}