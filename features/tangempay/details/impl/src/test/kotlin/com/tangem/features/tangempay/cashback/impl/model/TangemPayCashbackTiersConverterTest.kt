package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CashbackPromotions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class TangemPayCashbackTiersConverterTest {

    private val defaultLocale = Locale.getDefault()
    private val converter = TangemPayCashbackTiersConverter()

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN known tiers WHEN convert THEN rate plan and formatted amounts resolved once`() {
        // Arrange
        val promotions = promotions(
            tier(id = "basic", label = "Basic cards", scope = "All purchases", min = "30", cap = "100"),
            tier(id = "plus", label = "Plus cards", scope = "Everywhere", min = null, cap = "300"),
        )

        // Act
        val result = converter.convert(promotions)

        // Assert
        assertThat(result).containsExactly(
            CashbackTier(
                tierId = "basic",
                rate = 1,
                label = "Basic cards",
                scope = "All purchases",
                minPurchase = "$30",
                monthlyCap = "$100",
            ),
            CashbackTier(
                tierId = "plus",
                rate = 2,
                label = "Plus cards",
                scope = "Everywhere",
                minPurchase = null,
                monthlyCap = "$300",
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN unknown tier WHEN convert THEN rate is null and tier id kept`() {
        // Arrange
        val promotions = promotions(tier(id = "gold", label = "Gold", scope = "All", min = null, cap = null))

        // Act
        val result = converter.convert(promotions)

        // Assert
        assertThat(result).containsExactly(
            CashbackTier(
                tierId = "gold",
                rate = null,
                label = "Gold",
                scope = "All",
                minPurchase = null,
                monthlyCap = null,
            ),
        )
    }

    @Test
    fun `GIVEN no tiers WHEN convert THEN empty list`() {
        // Act
        val result = converter.convert(
            CashbackPromotions(cardTiers = emptyList(), monthlyCap = null, additionalCashback = emptyList()),
        )

        // Assert
        assertThat(result).isEmpty()
    }

    private fun promotions(vararg tiers: CashbackPromotions.CardTier) =
        CashbackPromotions(cardTiers = tiers.toList(), monthlyCap = null, additionalCashback = emptyList())

    private fun tier(
        id: String = "basic",
        label: String = "Basic cards",
        scope: String = "All purchases",
        min: String? = null,
        cap: String? = null,
    ) = CashbackPromotions.CardTier(
        tier = id,
        label = label,
        scope = scope,
        minTransactionAmount = min?.let(::BigDecimal),
        monthlyCapAmount = cap?.let(::BigDecimal),
    )
}