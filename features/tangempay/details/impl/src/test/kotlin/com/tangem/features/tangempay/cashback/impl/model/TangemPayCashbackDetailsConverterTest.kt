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
    fun `GIVEN single tier WHEN convert THEN title and tier eu paid-in and cap rows`() {
        // Act
        val result = converter.convert(
            tiers = listOf(tier(rate = 1, label = "Basic", min = "$30")),
            payoutCurrency = "USDC",
            monthlyCap = cap("150"),
        )

        // Assert
        assertThat(result.title).isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1")))
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList("USDC")),
            resourceReference(R.string.tangempay_cashback_details_cap, wrappedList("$150")),
        ).inOrder()
    }

    @Test
    fun `GIVEN two tiers WHEN convert THEN up-to title and one tier row per tier before the fixed rows`() {
        // Act
        val result = converter.convert(
            tiers = listOf(tier(rate = 1, label = "Basic", min = "$30"), tier(rate = 2, label = "Plus", min = "$30")),
            payoutCurrency = "USDC",
            monthlyCap = cap("150"),
        )

        // Assert
        assertThat(result.title)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_rate_title_up_to, wrappedList("2")))
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic", "$30")),
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("2", "Plus", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
            resourceReference(R.string.tangempay_cashback_details_paid_in, wrappedList("USDC")),
            resourceReference(R.string.tangempay_cashback_details_cap, wrappedList("$150")),
        ).inOrder()
    }

    @Test
    fun `GIVEN no payout currency and no cap WHEN convert THEN only tier and eu rows`() {
        // Act
        val result = converter.convert(
            tiers = listOf(tier(rate = 1, label = "Basic", min = "$30")),
            payoutCurrency = null,
            monthlyCap = null,
        )

        // Assert
        assertThat(result.rows).containsExactly(
            resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("1", "Basic", "$30")),
            resourceReference(R.string.tangempay_cashback_details_eu_excluded),
        ).inOrder()
    }

    @Test
    fun `GIVEN unknown rate tier WHEN convert THEN plain Cashback title and empty rate arg`() {
        // Act
        val result = converter.convert(
            tiers = listOf(tier(rate = null, label = "Gold", min = null)),
            payoutCurrency = "USDC",
            monthlyCap = cap("150"),
        )

        // Assert
        assertThat(result.title).isEqualTo(resourceReference(R.string.tangempay_cashback_title))
        assertThat(result.rows.first())
            .isEqualTo(resourceReference(R.string.tangempay_cashback_details_tier, wrappedList("", "Gold", "")))
    }

    @Test
    fun `GIVEN no tiers WHEN convert THEN plain Cashback title and empty rows`() {
        // Act
        val result = converter.convert(tiers = emptyList(), payoutCurrency = "USDC", monthlyCap = cap("150"))

        // Assert
        assertThat(result.title).isEqualTo(resourceReference(R.string.tangempay_cashback_title))
        assertThat(result.rows).isEmpty()
    }

    private fun tier(
        rate: Int? = 1,
        label: String = "Basic",
        min: String? = null,
    ) = CashbackTier(
        tierId = "basic",
        rate = rate,
        label = label,
        scope = "All purchases",
        minPurchase = min,
        monthlyCap = null,
    )

    private fun cap(amount: String, currency: String = "USD") =
        CashbackPromotions.MonthlyCap(amount = BigDecimal(amount), currency = currency)
}