package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.transaction.usecase.gasless.GetAvailableFeeTokensUseCase.Companion.isEligibleFeeToken
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetAvailableFeeTokensUseCaseTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isEligible(model: EligibilityModel) {
        // Arrange
        val status = createStatus(model.yieldSupplyStatus)

        // Act
        val actual = isEligibleFeeToken(status, isYieldWithdrawEnabled = model.isYieldWithdrawEnabled)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        // Plain token (no yield status) is always eligible, regardless of the toggle.
        EligibilityModel(yieldSupplyStatus = null, isYieldWithdrawEnabled = false, expected = true),
        EligibilityModel(yieldSupplyStatus = null, isYieldWithdrawEnabled = true, expected = true),
        // Active yield: eligible only when gasless v2 (yield withdraw) is enabled.
        EligibilityModel(yieldSupplyStatus = ACTIVE_YIELD, isYieldWithdrawEnabled = true, expected = true),
        EligibilityModel(yieldSupplyStatus = ACTIVE_YIELD, isYieldWithdrawEnabled = false, expected = false),
        // Inactive yield status: excluded either way (no module to withdraw from).
        EligibilityModel(yieldSupplyStatus = INACTIVE_YIELD, isYieldWithdrawEnabled = true, expected = false),
        EligibilityModel(yieldSupplyStatus = INACTIVE_YIELD, isYieldWithdrawEnabled = false, expected = false),
    )

    internal data class EligibilityModel(
        val yieldSupplyStatus: YieldSupplyStatus?,
        val isYieldWithdrawEnabled: Boolean,
        val expected: Boolean,
    )

    private fun createStatus(yieldSupplyStatus: YieldSupplyStatus?): CryptoCurrencyStatus {
        val status = mockk<CryptoCurrencyStatus>()
        every { status.value.yieldSupplyStatus } returns yieldSupplyStatus
        return status
    }

    private companion object {
        val ACTIVE_YIELD = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("100"),
        )
        val INACTIVE_YIELD = ACTIVE_YIELD.copy(isActive = false)
    }
}