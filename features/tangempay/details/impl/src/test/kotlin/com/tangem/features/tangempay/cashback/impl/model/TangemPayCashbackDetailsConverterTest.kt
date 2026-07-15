package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import org.junit.jupiter.api.Test

internal class TangemPayCashbackDetailsConverterTest {

    private val converter = TangemPayCashbackDetailsConverter()

    @Test
    fun `GIVEN single tier WHEN convert THEN title and one row with rate scope label min and cap`() {
        // Act
        val result = converter.convert(
            listOf(tier(rate = 1, label = "Basic cards", scope = "All purchases", min = "$30", cap = "$100")),
        )

        // Assert
        assertThat(result.title).isEqualTo(stringReference("Cashback 1%"))
        assertThat(result.rows).containsExactly(
            stringReference("1% for All purchases with your Basic cards, min purchase $30, up to $100 per month"),
        )
    }

    @Test
    fun `GIVEN two tiers WHEN convert THEN up-to title and one row per tier in order`() {
        // Act
        val result = converter.convert(
            listOf(
                tier(rate = 1, label = "Basic cards", scope = "All purchases", min = "$30", cap = "$100"),
                tier(rate = 2, label = "Plus cards", scope = "All purchases", min = "$30", cap = "$300"),
            ),
        )

        // Assert
        assertThat(result.title).isEqualTo(stringReference("Cashback up to 2%"))
        assertThat(result.rows).containsExactly(
            stringReference("1% for All purchases with your Basic cards, min purchase $30, up to $100 per month"),
            stringReference("2% for All purchases with your Plus cards, min purchase $30, up to $300 per month"),
        ).inOrder()
    }

    @Test
    fun `GIVEN tier without min or cap WHEN convert THEN row is rate scope and label only`() {
        // Act
        val result = converter.convert(
            listOf(tier(rate = 1, label = "Basic cards", scope = "All purchases", min = null, cap = null)),
        )

        // Assert
        assertThat(result.rows).containsExactly(stringReference("1% for All purchases with your Basic cards"))
    }

    @Test
    fun `GIVEN tier with unknown rate WHEN convert THEN plain Cashback title and row without percent prefix`() {
        // Act
        val result = converter.convert(
            listOf(tier(rate = null, label = "Gold cards", scope = "All purchases", min = null, cap = null)),
        )

        // Assert
        assertThat(result.title).isEqualTo(stringReference("Cashback"))
        assertThat(result.rows).containsExactly(stringReference("All purchases with your Gold cards"))
    }

    @Test
    fun `GIVEN no tiers WHEN convert THEN plain Cashback title and empty rows`() {
        // Act
        val result = converter.convert(emptyList())

        // Assert
        assertThat(result.title).isEqualTo(stringReference("Cashback"))
        assertThat(result.rows).isEmpty()
    }

    private fun tier(
        rate: Int? = 1,
        label: String = "Basic cards",
        scope: String = "All purchases",
        min: String? = null,
        cap: String? = null,
    ) = CashbackTier(
        tierId = "basic",
        rate = rate,
        label = label,
        scope = scope,
        minPurchase = min,
        monthlyCap = cap,
    )
}