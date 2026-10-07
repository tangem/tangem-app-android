package com.tangem.features.tangempay.withdraw.bank.amount.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.tangempay.withdraw.bank.amount.model.BankWithdrawAmountValidator.Result
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawAmountValidatorTest {

    private val minAmount = BigDecimal("1.00")
    private val maxAmount = BigDecimal("10000.00")

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN amount balance and fee WHEN validate THEN expected result`(model: ValidateModel) {
        // Act
        val actual = BankWithdrawAmountValidator.validate(
            amount = model.amount,
            minAmount = minAmount,
            maxAmount = maxAmount,
            balance = model.balance,
            fee = model.fee,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN balance below fee WHEN sendableAmount THEN zero`() {
        // Act
        val actual = BankWithdrawAmountValidator.sendableAmount(
            maxAmount = maxAmount,
            balance = BigDecimal("0.50"),
            fee = BigDecimal("1"),
        )

        // Assert
        assertThat(actual).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN balance minus fee within limit WHEN sendableAmount THEN balance minus fee`() {
        // Act
        val actual = BankWithdrawAmountValidator.sendableAmount(
            maxAmount = maxAmount,
            balance = BigDecimal("2325.55"),
            fee = BigDecimal("1"),
        )

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("2324.55"))
    }

    @Test
    fun `GIVEN balance minus fee above limit WHEN sendableAmount THEN limit`() {
        // Act
        val actual = BankWithdrawAmountValidator.sendableAmount(
            maxAmount = maxAmount,
            balance = BigDecimal("20000"),
            fee = BigDecimal("1"),
        )

        // Assert
        assertThat(actual).isEqualTo(maxAmount)
    }

    data class ValidateModel(
        val amount: BigDecimal?,
        val balance: BigDecimal = BigDecimal("2325.55"),
        val fee: BigDecimal = BigDecimal("1"),
        val expected: Result,
    )

    private fun provideTestModels() = listOf(
        ValidateModel(amount = null, expected = Result.Empty),
        ValidateModel(amount = BigDecimal("0"), expected = Result.Empty),
        ValidateModel(amount = BigDecimal("0.5"), expected = Result.OutOfRange),
        ValidateModel(amount = BigDecimal("0.5"), balance = BigDecimal("0.3"), expected = Result.OutOfRange),
        ValidateModel(amount = BigDecimal("1"), expected = Result.Valid),
        ValidateModel(amount = BigDecimal("2324.55"), expected = Result.Valid),
        ValidateModel(amount = BigDecimal("2325"), expected = Result.NotEnoughFunds),
        ValidateModel(amount = BigDecimal("500"), balance = BigDecimal("100"), expected = Result.NotEnoughFunds),
        ValidateModel(amount = BigDecimal("10000"), balance = BigDecimal("20000"), expected = Result.Valid),
        ValidateModel(amount = BigDecimal("10000"), balance = BigDecimal("10000"), expected = Result.NotEnoughFunds),
        ValidateModel(amount = BigDecimal("10000.01"), balance = BigDecimal("20000"), expected = Result.OutOfRange),
        ValidateModel(amount = BigDecimal("10001"), expected = Result.OutOfRange),
        ValidateModel(amount = BigDecimal("100015"), expected = Result.OutOfRange),
    )
}