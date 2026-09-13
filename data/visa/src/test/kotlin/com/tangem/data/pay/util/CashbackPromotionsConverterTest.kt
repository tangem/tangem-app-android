package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.spend.datasource.pay.models.response.CashbackPromotionsResponse
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CashbackPromotionsConverterTest {

    @Test
    fun `GIVEN cards WHEN convert THEN each card mapped with its fields`() {
        // Arrange
        val response = response(card(min = BigDecimal("30")))

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result).isEqualTo(
            CashbackPromotions(
                cards = listOf(
                    CashbackPromotions.CardPromotion(
                        cardType = "prestige",
                        title = "Prestige Card",
                        cashbackRate = BigDecimal("1.0"),
                        minTransactionAmount = BigDecimal("30"),
                        promotionId = "3f2a1c60-8d4b-4e7a-9c21-5b6d0e8f42aa",
                    ),
                ),
                accountMonthlyCap = null,
                additionalCashback = emptyList(),
            ),
        )
    }

    @Test
    fun `GIVEN account monthly cap WHEN convert THEN mapped with amount and currency`() {
        // Arrange
        val response = envelope(
            cashbackOnCards = CashbackPromotionsResponse.CashbackOnCards(
                cards = null,
                accountMonthlyCapAmount = BigDecimal("300"),
                accountMonthlyCapCurrency = "USD",
            ),
            additionalCashback = null,
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.accountMonthlyCap)
            .isEqualTo(CashbackPromotions.MonthlyCap(amount = BigDecimal("300"), currency = "USD"))
    }

    @Test
    fun `GIVEN no account monthly cap amount WHEN convert THEN monthly cap is null`() {
        // Arrange
        val response = envelope(
            cashbackOnCards = CashbackPromotionsResponse.CashbackOnCards(
                cards = null,
                accountMonthlyCapAmount = null,
                accountMonthlyCapCurrency = "USD",
            ),
            additionalCashback = null,
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.accountMonthlyCap).isNull()
    }

    @Test
    fun `GIVEN additional cashback WHEN convert THEN each promo mapped with its fields`() {
        // Arrange
        val response = envelope(
            cashbackOnCards = null,
            additionalCashback = listOf(
                additional(id = "p1", name = "Groceries", description = "+1%", endDate = null, priority = 99),
                additional(
                    id = "p2",
                    name = "Cashback",
                    description = "+2%",
                    endDate = "2026-09-26",
                    priority = 50,
                    cardType = "plus",
                    promoCapAmount = BigDecimal("10"),
                    promoCapPeriod = "monthly",
                    capCurrency = "USD",
                    minTransactionAmount = BigDecimal("30"),
                ),
            ),
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.additionalCashback).containsExactly(
            CashbackPromotions.AdditionalCashback(
                id = "p1",
                cardType = null,
                name = "Groceries",
                description = "+1%",
                endDate = null,
                promoCap = null,
                minTransactionAmount = null,
                priority = 99,
            ),
            CashbackPromotions.AdditionalCashback(
                id = "p2",
                cardType = "plus",
                name = "Cashback",
                description = "+2%",
                endDate = DateTime.parse("2026-09-26"),
                promoCap = CashbackPromotions.PromoCap(
                    amount = BigDecimal("10"),
                    period = CashbackPromotions.PromoCap.Period.MONTHLY,
                    currency = "USD",
                ),
                minTransactionAmount = BigDecimal("30"),
                priority = 50,
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN promo cap with unrecognized period WHEN convert THEN period is UNKNOWN`() {
        // Arrange
        val response = envelope(
            cashbackOnCards = null,
            additionalCashback = listOf(
                additional(promoCapAmount = BigDecimal("10"), promoCapPeriod = "weekly"),
                additional(promoCapAmount = BigDecimal("10"), promoCapPeriod = null),
            ),
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.additionalCashback.map { it.promoCap?.period })
            .containsExactly(
                CashbackPromotions.PromoCap.Period.UNKNOWN,
                CashbackPromotions.PromoCap.Period.UNKNOWN,
            ).inOrder()
    }

    @Test
    fun `GIVEN additional cashback with blank description WHEN convert THEN description is null`() {
        // Arrange
        val response = envelope(
            cashbackOnCards = null,
            additionalCashback = listOf(additional(description = " ")),
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.additionalCashback.single().description).isNull()
    }

    @Test
    fun `GIVEN additional cashback with malformed end date WHEN convert THEN end date null and payload kept`() {
        // Arrange
        val response = envelope(
            cashbackOnCards = null,
            additionalCashback = listOf(additional(endDate = "not-a-date")),
        )

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.additionalCashback).containsExactly(
            CashbackPromotions.AdditionalCashback(
                id = "id",
                cardType = null,
                name = "name",
                description = "description",
                endDate = null,
                promoCap = null,
                minTransactionAmount = null,
                priority = 0,
            ),
        )
    }

    @Test
    fun `GIVEN missing result envelope WHEN convert THEN everything is empty`() {
        // Act
        val result = CashbackPromotionsConverter.convert(CashbackPromotionsResponse(result = null))

        // Assert
        assertThat(result)
            .isEqualTo(
                CashbackPromotions(cards = emptyList(), accountMonthlyCap = null, additionalCashback = emptyList()),
            )
    }

    @Test
    fun `GIVEN null cashbackOnCards WHEN convert THEN no cards`() {
        // Arrange
        val response = envelope(cashbackOnCards = null, additionalCashback = null)

        // Act
        val result = CashbackPromotionsConverter.convert(response)

        // Assert
        assertThat(result.cards).isEmpty()
    }

    private fun envelope(
        cashbackOnCards: CashbackPromotionsResponse.CashbackOnCards?,
        additionalCashback: List<CashbackPromotionsResponse.AdditionalCashback>?,
    ) = CashbackPromotionsResponse(
        result = CashbackPromotionsResponse.Result(
            cashbackOnCards = cashbackOnCards,
            additionalCashback = additionalCashback,
        ),
    )

    private fun response(vararg cards: CashbackPromotionsResponse.Card) = envelope(
        cashbackOnCards = CashbackPromotionsResponse.CashbackOnCards(
            cards = cards.toList(),
            accountMonthlyCapAmount = null,
            accountMonthlyCapCurrency = null,
        ),
        additionalCashback = null,
    )

    private fun card(
        cardType: String = "prestige",
        title: String? = "Prestige Card",
        rate: BigDecimal = BigDecimal("1.0"),
        min: BigDecimal? = null,
        promotionId: String = "3f2a1c60-8d4b-4e7a-9c21-5b6d0e8f42aa",
    ) = CashbackPromotionsResponse.Card(
        cardType = cardType,
        title = title,
        cardCashbackRate = rate,
        minTransactionAmount = min,
        promotionId = promotionId,
    )

    @Suppress("LongParameterList")
    private fun additional(
        id: String = "id",
        name: String = "name",
        description: String? = "description",
        endDate: String? = null,
        cardType: String? = null,
        promoCapAmount: BigDecimal? = null,
        promoCapPeriod: String? = null,
        capCurrency: String? = null,
        minTransactionAmount: BigDecimal? = null,
        priority: Int = 0,
    ) = CashbackPromotionsResponse.AdditionalCashback(
        id = id,
        cardType = cardType,
        name = name,
        description = description,
        endDate = endDate,
        promoCapAmount = promoCapAmount,
        promoCapPeriod = promoCapPeriod,
        capCurrency = capCurrency,
        minTransactionAmount = minTransactionAmount,
        priority = priority,
    )
}