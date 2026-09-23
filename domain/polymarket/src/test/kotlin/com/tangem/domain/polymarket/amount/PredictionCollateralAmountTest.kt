package com.tangem.domain.polymarket.amount

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionCollateralAmountTest {

    @ParameterizedTest
    @ProvideTestModels
    fun toBaseUnits(model: TestModel) {
        // Act
        val actual = PredictionCollateralAmount.toBaseUnits(model.amount)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class TestModel(
        val amount: BigDecimal,
        val expected: arrow.core.Either<PredictionAmountError, BigInteger>,
    )

    private fun provideTestModels() = listOf(
        TestModel(BigDecimal("12.50"), BigInteger("12500000").right()),
        TestModel(BigDecimal("1"), BigInteger("1000000").right()),
        TestModel(BigDecimal("0.01"), BigInteger("10000").right()),
        TestModel(BigDecimal("250.75"), BigInteger("250750000").right()),
        TestModel(BigDecimal("0.000001"), BigInteger.ONE.right()),
        TestModel(BigDecimal.ZERO, PredictionAmountError.NotPositive.left()),
        TestModel(BigDecimal("-1"), PredictionAmountError.NotPositive.left()),
        TestModel(BigDecimal("0.0000001"), PredictionAmountError.TooFine.left()),
        TestModel(BigDecimal("1.23456789"), PredictionAmountError.TooFine.left()),
    )
}