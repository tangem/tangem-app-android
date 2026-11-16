package com.tangem.domain.pay.model.bankofframp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.AccountFee.CalculationType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfframpFeeTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fee WHEN amountFor transfer amount THEN fee in money is returned`(model: AmountForModel) {
        // Arrange
        val fee = OfframpFee(
            name = "SPEI",
            calculationType = model.calculationType,
            amount = BigDecimal(model.feeAmount),
            currency = "USD",
            description = null,
        )

        // Act
        val actual = fee.amountFor(BigDecimal(model.transferAmount))

        // Assert
        assertThat(actual).isEqualTo(BigDecimal(model.expected))
    }

    internal data class AmountForModel(
        val calculationType: CalculationType,
        val feeAmount: String,
        val transferAmount: String,
        val expected: String,
    )

    private fun provideTestModels() = listOf(
        AmountForModel(CalculationType.FIXED, feeAmount = "1.50", transferAmount = "100", expected = "1.50"),
        AmountForModel(CalculationType.FIXED, feeAmount = "15.00", transferAmount = "0", expected = "15.00"),
        AmountForModel(CalculationType.PERCENTAGE, feeAmount = "1", transferAmount = "100", expected = "1.00"),
        AmountForModel(CalculationType.PERCENTAGE, feeAmount = "1.5", transferAmount = "33.33", expected = "0.50"),
        AmountForModel(CalculationType.PERCENTAGE, feeAmount = "1", transferAmount = "0.50", expected = "0.01"),
        AmountForModel(CalculationType.PERCENTAGE, feeAmount = "1", transferAmount = "0.49", expected = "0.00"),
        AmountForModel(CalculationType.PERCENTAGE, feeAmount = "0", transferAmount = "250", expected = "0.00"),
    )
}