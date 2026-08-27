package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.CardDeliveryQuoteResponse
import com.tangem.domain.pay.model.CardDeliveryQuote
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CardDeliveryQuoteConverterTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun convert(model: ConvertModel) {
        // Act
        val actual = CardDeliveryQuoteConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "plastic available, fee charged, sufficient balance",
            response = createResponse(),
            expected = CardDeliveryQuote(
                country = "US",
                isPlasticAvailable = true,
                isDeliveryFeeWaived = false,
                deliveryFee = CardDeliveryQuote.DeliveryFee(
                    amount = BigDecimal("5.00"),
                    currency = Currency.getInstance("USD"),
                ),
                deliveryEta = CardDeliveryQuote.DeliveryEta(minBusinessDays = 5, maxBusinessDays = 10),
                hasSufficientBalance = true,
            ),
        ),
        ConvertModel(
            name = "fee waived, insufficient balance, plastic unavailable, EUR",
            response = createResponse(
                country = "DE",
                isPlasticAvailable = false,
                isDeliveryFeeWaived = true,
                amount = "0.00",
                currency = "EUR",
                minBusinessDays = 7,
                maxBusinessDays = 14,
                hasSufficientBalance = false,
            ),
            expected = CardDeliveryQuote(
                country = "DE",
                isPlasticAvailable = false,
                isDeliveryFeeWaived = true,
                deliveryFee = CardDeliveryQuote.DeliveryFee(
                    amount = BigDecimal("0.00"),
                    currency = Currency.getInstance("EUR"),
                ),
                deliveryEta = CardDeliveryQuote.DeliveryEta(minBusinessDays = 7, maxBusinessDays = 14),
                hasSufficientBalance = false,
            ),
        ),
    )

    internal data class ConvertModel(
        val name: String,
        val response: CardDeliveryQuoteResponse,
        val expected: CardDeliveryQuote,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        fun createResponse(
            country: String = "US",
            isPlasticAvailable: Boolean = true,
            isDeliveryFeeWaived: Boolean = false,
            amount: String = "5.00",
            currency: String = "USD",
            minBusinessDays: Int = 5,
            maxBusinessDays: Int = 10,
            hasSufficientBalance: Boolean = true,
        ) = CardDeliveryQuoteResponse(
            country = country,
            isPlasticAvailable = isPlasticAvailable,
            isDeliveryFeeWaived = isDeliveryFeeWaived,
            deliveryFee = CardDeliveryQuoteResponse.DeliveryFee(amount = BigDecimal(amount), currency = currency),
            deliveryEta = CardDeliveryQuoteResponse.DeliveryEta(
                minBusinessDays = minBusinessDays,
                maxBusinessDays = maxBusinessDays,
            ),
            hasSufficientBalance = hasSufficientBalance,
        )
    }
}