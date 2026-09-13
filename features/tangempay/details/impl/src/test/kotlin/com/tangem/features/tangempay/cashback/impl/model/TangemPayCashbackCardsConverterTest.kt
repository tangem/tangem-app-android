package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CashbackPromotions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class TangemPayCashbackCardsConverterTest {

    private val defaultLocale = Locale.getDefault()
    private val converter = TangemPayCashbackCardsConverter()

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN cards WHEN convert THEN backend rate kept and min purchase formatted`() {
        // Arrange
        val promotions = promotions(
            card(cardType = "basic", title = "Basic Card", rate = "1.0", min = "30"),
            card(cardType = "prestige", title = "Prestige Card", rate = "2.5", min = null),
        )

        // Act
        val result = converter.convert(promotions)

        // Assert
        assertThat(result).containsExactly(
            CashbackCard(
                cardType = "basic",
                title = "Basic Card",
                rate = BigDecimal("1.0"),
                minPurchase = "$30",
            ),
            CashbackCard(
                cardType = "prestige",
                title = "Prestige Card",
                rate = BigDecimal("2.5"),
                minPurchase = null,
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN no cards WHEN convert THEN empty list`() {
        // Act
        val result = converter.convert(
            CashbackPromotions(cards = emptyList(), accountMonthlyCap = null, additionalCashback = emptyList()),
        )

        // Assert
        assertThat(result).isEmpty()
    }

    private fun promotions(vararg cards: CashbackPromotions.CardPromotion) =
        CashbackPromotions(cards = cards.toList(), accountMonthlyCap = null, additionalCashback = emptyList())

    private fun card(cardType: String, title: String, rate: String, min: String?) =
        CashbackPromotions.CardPromotion(
            cardType = cardType,
            title = title,
            cashbackRate = BigDecimal(rate),
            minTransactionAmount = min?.let(::BigDecimal),
            promotionId = "promo-$cardType",
        )
}