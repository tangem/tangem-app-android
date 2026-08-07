package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.pay.models.response.CashbackPromotionsResponse
import com.tangem.domain.pay.model.CashbackPromotions
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CashbackPromotionsConverterTest {

    @Test
    fun `GIVEN tiers WHEN convert THEN each tier mapped with its fields`() {
        // Arrange
        val response = response(tier(min = BigDecimal("30"), cap = BigDecimal("100")))

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result).isEqualTo(
            CashbackPromotions(
                cardTiers = listOf(
                    CashbackPromotions.CardTier(
                        tier = "basic",
                        label = "Basic",
                        scope = "All purchases",
                        minTransactionAmount = BigDecimal("30"),
                        monthlyCapAmount = BigDecimal("100"),
                    ),
                ),
                additionalCashback = emptyList(),
            ),
        )
    }

    @Test
    fun `GIVEN additional cashback WHEN convert THEN each promo mapped with its fields`() {
        // Arrange
        val response = CashbackPromotionsResponse(
            cashbackOnCards = null,
            additionalCashback = listOf(
                additional(id = "p1", name = "Groceries", description = "+1%", isPermanent = true, endDate = null),
                additional(
                    id = "p2",
                    name = "Cashback",
                    description = "+2%",
                    isPermanent = false,
                    endDate = "2026-09-26",
                ),
            ),
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.additionalCashback).containsExactly(
            CashbackPromotions.AdditionalCashback(
                id = "p1",
                name = "Groceries",
                description = "+1%",
                isPermanent = true,
                endDate = null,
            ),
            CashbackPromotions.AdditionalCashback(
                id = "p2",
                name = "Cashback",
                description = "+2%",
                isPermanent = false,
                endDate = DateTime.parse("2026-09-26"),
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN additional cashback with null isPermanent WHEN convert THEN it is derived from end date`() {
        // Arrange
        val response = CashbackPromotionsResponse(
            cashbackOnCards = null,
            additionalCashback = listOf(
                additional(isPermanent = null, endDate = null),
                additional(isPermanent = null, endDate = "2026-09-26"),
            ),
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.additionalCashback.map { it.isPermanent }).containsExactly(true, false).inOrder()
    }

    @Test
    fun `GIVEN additional cashback with malformed end date WHEN convert THEN end date null and payload kept`() {
        // Arrange
        val response = CashbackPromotionsResponse(
            cashbackOnCards = null,
            additionalCashback = listOf(additional(isPermanent = false, endDate = "not-a-date")),
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.additionalCashback).containsExactly(
            CashbackPromotions.AdditionalCashback(
                id = "id",
                name = "name",
                description = "description",
                isPermanent = false,
                endDate = null,
            ),
        )
    }

    @Test
    fun `GIVEN null cashbackOnCards WHEN convert THEN no card tiers`() {
        // Arrange
        val response = CashbackPromotionsResponse(cashbackOnCards = null, additionalCashback = null)

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.cardTiers).isEmpty()
    }

    @Test
    fun `GIVEN tier with null strings WHEN convert THEN strings default to empty and amounts stay null`() {
        // Arrange
        val response = response(tier(tier = null, label = null, scope = null))

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.cardTiers).containsExactly(
            CashbackPromotions.CardTier(
                tier = "",
                label = "",
                scope = "",
                minTransactionAmount = null,
                monthlyCapAmount = null,
            ),
        )
    }

    private fun response(vararg tiers: CashbackPromotionsResponse.CardTier) = CashbackPromotionsResponse(
        cashbackOnCards = CashbackPromotionsResponse.CashbackOnCards(
            tiers = tiers.toList(),
            monthlyCapAmount = null,
            monthlyCapCurrency = null,
        ),
        additionalCashback = null,
    )

    private fun tier(
        tier: String? = "basic",
        label: String? = "Basic",
        scope: String? = "All purchases",
        min: BigDecimal? = null,
        cap: BigDecimal? = null,
    ) = CashbackPromotionsResponse.CardTier(
        tier = tier,
        label = label,
        scope = scope,
        minTransactionAmount = min,
        tierMonthlyCapAmount = cap,
        promotionId = null,
    )

    private fun additional(
        id: String? = "id",
        name: String? = "name",
        description: String? = "description",
        isPermanent: Boolean? = false,
        endDate: String? = null,
    ) = CashbackPromotionsResponse.AdditionalCashback(
        id = id,
        name = name,
        description = description,
        isPermanent = isPermanent,
        endDate = endDate,
    )
}