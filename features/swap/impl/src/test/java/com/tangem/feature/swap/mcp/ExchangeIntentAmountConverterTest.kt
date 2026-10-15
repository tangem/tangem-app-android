package com.tangem.feature.swap.mcp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExchangeIntentAmountConverterTest {

    private val converter = ExchangeIntentAmountConverter()

    private val intent = createExchangeIntent()

    @ParameterizedTest
    @ProvideTestModels
    fun `amount of the intent`(model: AmountModel) {
        // Arrange
        val exchangeIntent = intent.copy(amountType = model.side, amount = model.amount)

        // Act
        val actual = converter.convert(exchangeIntent)

        // Assert
        assertThat(actual?.toPlainString()).isEqualTo(model.expected)
    }

    data class AmountModel(val side: SwapAmountType, val amount: String, val expected: String?)

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        AmountModel(SwapAmountType.From, amount = "42000000", expected = "42.000000"),
        AmountModel(SwapAmountType.To, amount = "2500000000000000000", expected = "2.500000000000000000"),
        AmountModel(SwapAmountType.From, amount = "0", expected = "0.000000"),
        AmountModel(SwapAmountType.From, amount = "forty two", expected = null),
    )
}