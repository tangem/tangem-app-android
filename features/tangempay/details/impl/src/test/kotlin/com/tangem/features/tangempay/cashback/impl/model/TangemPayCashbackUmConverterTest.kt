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
import org.joda.time.LocalDate
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

    private val converter = TangemPayCashbackUmConverter(today = { LocalDate.parse("2026-07-01") })

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
    fun `GIVEN null or nothing earned without awaiting payout WHEN convert THEN empty state without banner`(
        cashback: TangemPayCashback?,
    ) {
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
    fun `GIVEN previous payout WHEN convert THEN info deposit banner built from previous payout`() {
        // Arrange
        val cashback = createCashback(
            confirmedAmount = BigDecimal("22.54"),
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-03"),
                amount = BigDecimal("10.50"),
            ),
        )

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
                    formatArgs = wrappedList("$10.50", "June", "July 3"),
                ),
                type = TangemPayCashbackUM.Banner.Type.Info,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN zero amount AND positive total earned AND awaiting payout WHEN convert THEN zero month state with banner`() {
        // Arrange
        val cashback = createCashback(
            confirmedAmount = BigDecimal.ZERO,
            totalEarnedAmount = BigDecimal("132.15"),
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-03"),
                amount = BigDecimal("10.50"),
            ),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = resourceReference(
                R.string.tangempay_cashback_earned_title,
                wrappedList("$0.00", arrayItemReference(R.array.common_month_in, index = 5)),
            ),
            subtitle = resourceReference(R.string.tangempay_cashback_deposited_on, wrappedList(PAYOUT_WINDOW)),
            isEmpty = false,
            banner = TangemPayCashbackUM.Banner(
                text = resourceReference(
                    id = R.string.tangempay_cashback_deposit_banner,
                    formatArgs = wrappedList("$10.50", "June", "July 3"),
                ),
                type = TangemPayCashbackUM.Banner.Type.Info,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN nothing earned AND awaiting previous payout WHEN convert THEN empty state with deposit banner`() {
        // Arrange
        val cashback = createCashback(
            confirmedAmount = BigDecimal.ZERO,
            totalEarnedAmount = BigDecimal.ZERO,
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-03"),
                amount = BigDecimal("10.50"),
            ),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = resourceReference(R.string.tangempay_cashback_empty_title),
            subtitle = resourceReference(R.string.tangempay_cashback_empty_subtitle),
            isEmpty = true,
            banner = TangemPayCashbackUM.Banner(
                text = resourceReference(
                    id = R.string.tangempay_cashback_deposit_banner,
                    formatArgs = wrappedList("$10.50", "June", "July 3"),
                ),
                type = TangemPayCashbackUM.Banner.Type.Info,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("nonPositivePayoutAmounts")
    fun `GIVEN previous payout with non-positive amount WHEN convert THEN no deposit banner`(amount: BigDecimal) {
        // Arrange
        val cashback = createCashback(
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-03"),
                amount = amount,
            ),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        assertThat(actual.banner).isNull()
    }

    @Test
    fun `GIVEN previous payout ended before today WHEN convert THEN no deposit banner`() {
        // Arrange
        val cashback = createCashback(
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-06-30"),
                amount = BigDecimal("10.50"),
            ),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        assertThat(actual.banner).isNull()
    }

    @Test
    fun `GIVEN previous payout ending today WHEN convert THEN deposit banner kept`() {
        // Arrange
        val cashback = createCashback(
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-01"),
                amount = BigDecimal("10.50"),
            ),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        assertThat(actual.banner).isEqualTo(
            TangemPayCashbackUM.Banner(
                text = resourceReference(
                    id = R.string.tangempay_cashback_deposit_banner,
                    formatArgs = wrappedList("$10.50", "June", "July 1"),
                ),
                type = TangemPayCashbackUM.Banner.Type.Info,
            ),
        )
    }

    @Test
    fun `GIVEN positive amount AND no previous payout WHEN convert THEN subtitle kept and no banner`() {
        // Arrange
        val cashback = createCashback(confirmedAmount = BigDecimal("22.54"), previousPayout = null)

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
            banner = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN no payout window AND previous payout WHEN convert THEN deposit banner without subtitle`() {
        // Arrange
        val cashback = createCashback(
            payoutStart = null,
            payoutEnd = null,
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-03"),
                amount = BigDecimal("10.50"),
            ),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        assertThat(actual.subtitle).isNull()
        assertThat(actual.banner).isEqualTo(
            TangemPayCashbackUM.Banner(
                text = resourceReference(
                    id = R.string.tangempay_cashback_deposit_banner,
                    formatArgs = wrappedList("$10.50", "June", "July 3"),
                ),
                type = TangemPayCashbackUM.Banner.Type.Info,
            ),
        )
    }

    @Test
    fun `GIVEN negative amount WHEN convert THEN no deposit subtitle and refund error banner`() {
        // Arrange
        val cashback = createCashback(
            confirmedAmount = BigDecimal("-22.54"),
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-03"),
                amount = BigDecimal("10.50"),
            ),
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = resourceReference(
                R.string.tangempay_cashback_earned_title,
                wrappedList("-$22.54", arrayItemReference(R.array.common_month_in, index = 5)),
            ),
            subtitle = null,
            isEmpty = false,
            banner = TangemPayCashbackUM.Banner(
                text = resourceReference(R.string.tangempay_cashback_refund_banner),
                type = TangemPayCashbackUM.Banner.Type.Error,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN no payout window WHEN convert THEN no subtitle and no banner`() {
        // Arrange
        val cashback = createCashback(payoutStart = null, payoutEnd = null)

        // Act
        val actual = converter.convert(cashback)

        // Assert
        val expected = TangemPayCashbackUM(
            title = resourceReference(
                R.string.tangempay_cashback_earned_title,
                wrappedList("$22.54", arrayItemReference(R.array.common_month_in, index = 5)),
            ),
            subtitle = null,
            isEmpty = false,
            banner = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN no payout window AND negative amount WHEN convert THEN refund banner is kept`() {
        // Arrange
        val cashback = createCashback(
            confirmedAmount = BigDecimal("-22.54"),
            payoutStart = null,
            payoutEnd = null,
        )

        // Act
        val actual = converter.convert(cashback)

        // Assert
        assertThat(actual.subtitle).isNull()
        assertThat(actual.banner).isEqualTo(
            TangemPayCashbackUM.Banner(
                text = resourceReference(R.string.tangempay_cashback_refund_banner),
                type = TangemPayCashbackUM.Banner.Type.Error,
            ),
        )
    }

    private fun emptyStateCashback(): List<TangemPayCashback?> = listOf(
        null,
        createCashback(confirmedAmount = BigDecimal.ZERO, totalEarnedAmount = BigDecimal.ZERO),
        createCashback(confirmedAmount = BigDecimal("0.00"), totalEarnedAmount = BigDecimal("0.00")),
        createCashback(confirmedAmount = BigDecimal.ZERO, totalEarnedAmount = null),
        createCashback(
            confirmedAmount = BigDecimal.ZERO,
            totalEarnedAmount = BigDecimal.ZERO,
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-06-30"),
                amount = BigDecimal("10.50"),
            ),
        ),
        createCashback(
            confirmedAmount = BigDecimal.ZERO,
            totalEarnedAmount = BigDecimal.ZERO,
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-07-03"),
                amount = BigDecimal.ZERO,
            ),
        ),
    )

    private fun nonPositivePayoutAmounts(): List<BigDecimal> = listOf(BigDecimal("-5.00"), BigDecimal.ZERO)

    private fun createCashback(
        confirmedAmount: BigDecimal = BigDecimal("22.54"),
        totalEarnedAmount: BigDecimal? = BigDecimal("132.15"),
        currency: String = "USD",
        year: Int = 2026,
        month: Int = 6,
        payoutStart: DateTime? = DateTime.parse("2026-07-01"),
        payoutEnd: DateTime? = DateTime.parse("2026-07-05"),
        previousPayout: TangemPayCashback.PreviousPayout? = null,
    ): TangemPayCashback = TangemPayCashback(
        confirmedAmount = confirmedAmount,
        totalEarnedAmount = totalEarnedAmount,
        currency = currency,
        payoutCurrency = "USDC",
        period = TangemPayCashback.Period(
            year = year,
            month = month,
            payoutStart = payoutStart,
            payoutEnd = payoutEnd,
        ),
        previousPayout = previousPayout,
    )

    private companion object {
        const val PAYOUT_WINDOW = "July 1 – 5"
    }
}