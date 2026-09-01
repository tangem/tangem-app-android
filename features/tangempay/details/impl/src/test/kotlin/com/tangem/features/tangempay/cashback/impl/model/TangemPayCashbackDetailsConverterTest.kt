package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.CashbackPromotions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class TangemPayCashbackDetailsConverterTest {

    private val defaultLocale = Locale.getDefault()
    private val converter = TangemPayCashbackDetailsConverter()

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN single card WHEN convert THEN title and card eu paid-in and cap rows`() {
        // Act
        val result = converter.convert(
            cards = listOf(card(rate = "1.0", title = "Basic Card", min = "$30")),
            payoutCurrency = "USDC",
            accountMonthlyCap = cap("300"),
        )

        // Assert
        assertThat(result.title).isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1")))
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList("USDC")),
            resourceReference(R.string.tangempay_cashback_details_cap, wrappedList("$300")),
        ).inOrder()
    }

    @Test
    fun `GIVEN two cards WHEN convert THEN up-to title and one row per card before the fixed rows`() {
        // Act
        val result = converter.convert(
            cards = listOf(
                card(rate = "1.0", title = "Basic Card", min = "$30"),
                card(rate = "2.0", title = "Plus Card", min = "$30"),
            ),
            payoutCurrency = "USDC",
            accountMonthlyCap = cap("300"),
        )

        // Assert
        assertThat(result.title)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title_up_to, wrappedList("2")))
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("2", "Plus Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList("USDC")),
            resourceReference(R.string.tangempay_cashback_details_cap, wrappedList("$300")),
        ).inOrder()
    }

    @Test
    fun `GIVEN no payout currency and no cap WHEN convert THEN paid-in and cap rows are dropped`() {
        // Act
        val result = converter.convert(
            cards = listOf(card(rate = "1.0", title = "Basic Card", min = "$30")),
            payoutCurrency = null,
            accountMonthlyCap = null,
        )

        // Assert
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
        ).inOrder()
    }

    @Test
    fun `GIVEN blank payout currency WHEN convert THEN paid-in row is dropped`() {
        // Act
        val result = converter.convert(
            cards = listOf(card(rate = "1.0", title = "Basic Card", min = "$30")),
            payoutCurrency = " ",
            accountMonthlyCap = null,
        )

        // Assert
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
        ).inOrder()
    }

    @Test
    fun `GIVEN payout currency differs from the cap currency WHEN convert THEN paid-in uses the payout one`() {
        // Act
        val result = converter.convert(
            cards = listOf(card(rate = "1.0", title = "Basic Card", min = "$30")),
            payoutCurrency = "USDT",
            accountMonthlyCap = cap("300", currency = "USD"),
        )

        // Assert
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList("USDT")),
            resourceReference(R.string.tangempay_cashback_details_cap, wrappedList("$300")),
        ).inOrder()
    }

    @Test
    fun `GIVEN fractional rate and no minimum WHEN convert THEN rate keeps decimals and min arg is empty`() {
        // Act
        val result = converter.convert(
            cards = listOf(card(rate = "1.50", title = "Prestige Card", min = null)),
            payoutCurrency = "USDC",
            accountMonthlyCap = null,
        )

        // Assert
        assertThat(result.rows.first()).isEqualTo(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1.5", "Prestige Card", "")),
        )
    }

    @Test
    fun `GIVEN card without title WHEN convert THEN its row is skipped`() {
        // Act
        val result = converter.convert(
            cards = listOf(
                card(rate = "1.0", title = null, min = "$30"),
                card(rate = "2.0", title = "Plus Card", min = "$30"),
            ),
            payoutCurrency = "USDC",
            accountMonthlyCap = null,
        )

        // Assert
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("2", "Plus Card", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList("USDC")),
        ).inOrder()
    }

    @Test
    fun `GIVEN no cards WHEN convert THEN plain Cashback title and empty rows`() {
        // Act
        val result = converter.convert(cards = emptyList(), payoutCurrency = "USDC", accountMonthlyCap = cap("300"))

        // Assert
        assertThat(result.title).isEqualTo(resourceReference(R.string.tangempay_cashback_title))
        assertThat(result.rows).isEmpty()
    }

    private fun card(rate: String, title: String?, min: String?) = CashbackCard(
        cardType = "basic",
        title = title,
        rate = BigDecimal(rate),
        minPurchase = min,
    )

    private fun cap(amount: String, currency: String = "USD") =
        CashbackPromotions.MonthlyCap(amount = BigDecimal(amount), currency = currency)
}