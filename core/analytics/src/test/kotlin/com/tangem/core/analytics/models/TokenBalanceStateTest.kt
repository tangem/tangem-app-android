package com.tangem.core.analytics.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenBalanceStateTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN amount WHEN fromAmount THEN expected state returned`(model: FromAmountModel) {
        // Act
        val actual = AnalyticsParam.TokenBalanceState.fromAmount(model.amount)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class FromAmountModel(val amount: BigDecimal?, val expected: AnalyticsParam.TokenBalanceState)

    private fun provideTestModels() = listOf(
        FromAmountModel(amount = null, expected = AnalyticsParam.TokenBalanceState.Unavailable),
        FromAmountModel(amount = BigDecimal.ZERO, expected = AnalyticsParam.TokenBalanceState.Empty),
        FromAmountModel(amount = BigDecimal("0.000"), expected = AnalyticsParam.TokenBalanceState.Empty),
        FromAmountModel(amount = BigDecimal("0.00000001"), expected = AnalyticsParam.TokenBalanceState.Full),
        FromAmountModel(amount = BigDecimal.TEN, expected = AnalyticsParam.TokenBalanceState.Full),
    )
}