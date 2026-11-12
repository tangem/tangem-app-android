package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawOptionsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rail limits and the account rule WHEN amountLimitsFor THEN rail limits only when followed`(
        model: LimitsModel,
    ) {
        // Arrange
        val options = bankWithdrawOptions(isLimitedPerRail = model.isLimitedPerRail)
        val destination = offrampDestination(amountLimits = model.railLimits)

        // Act
        val actual = options.amountLimitsFor(destination)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        LimitsModel(isLimitedPerRail = true, railLimits = MONEY_ACCOUNT_LIMITS, expected = MONEY_ACCOUNT_LIMITS),
        LimitsModel(isLimitedPerRail = true, railLimits = null, expected = DEFAULT_LIMITS),
        LimitsModel(
            isLimitedPerRail = true,
            railLimits = MONEY_ACCOUNT_LIMITS.copy(currency = "usd"),
            expected = MONEY_ACCOUNT_LIMITS.copy(currency = "usd"),
        ),
        LimitsModel(
            isLimitedPerRail = true,
            railLimits = MONEY_ACCOUNT_LIMITS.copy(currency = "MXN"),
            expected = DEFAULT_LIMITS,
        ),
        LimitsModel(isLimitedPerRail = false, railLimits = MONEY_ACCOUNT_LIMITS, expected = DEFAULT_LIMITS),
    )

    data class LimitsModel(
        val isLimitedPerRail: Boolean,
        val railLimits: AmountLimits?,
        val expected: AmountLimits,
    )

    private companion object {
        val MONEY_ACCOUNT_LIMITS = AmountLimits(min = BigDecimal("10"), max = null, currency = "USD")
        val DEFAULT_LIMITS = AmountLimits(min = BigDecimal("1.00"), max = BigDecimal("10000.00"), currency = "USD")
    }
}