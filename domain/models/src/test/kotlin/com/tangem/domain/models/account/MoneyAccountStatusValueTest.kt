package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MoneyAccountStatusValueTest {

    private val cryptoCurrency = mockk<CryptoCurrency.Token>(relaxed = true)

    private fun balanceOf(fiatRate: BigDecimal?, cryptoAmount: BigDecimal = BigDecimal("100")) =
        MoneyAccountStatusValue.Balance(
            cryptoAmount = cryptoAmount,
            depositAddress = "0xabc",
            currencyCode = "USD",
            fiatRate = fiatRate,
        )

    private fun activeWith(balance: MoneyAccountStatusValue.Balance?) = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        cryptoCurrency = cryptoCurrency,
        balance = balance,
        customerId = "customer",
    )

    @Test
    fun `GIVEN balance with rate WHEN totalFiatBalance THEN amount converted via rate`() {
        // Arrange
        val rate = BigDecimal("0.9")
        val active = activeWith(balanceOf(fiatRate = rate, cryptoAmount = BigDecimal("100")))

        // Act
        val result = active.totalFiatBalance

        // Assert
        Truth.assertThat(result).isEqualTo(
            TotalFiatBalance.Loaded(amount = BigDecimal("100").multiply(rate), source = StatusSource.ACTUAL),
        )
    }

    @Test
    fun `GIVEN balance without rate WHEN totalFiatBalance THEN Failed`() {
        // Arrange
        val active = activeWith(balanceOf(fiatRate = null))

        // Act & Assert
        Truth.assertThat(active.totalFiatBalance).isEqualTo(TotalFiatBalance.Failed)
    }

    @Test
    fun `GIVEN onboarding status WHEN totalFiatBalance THEN zero`() {
        // Arrange
        val issuing = MoneyAccountStatusValue.Onboarding.Issuing(
            source = StatusSource.ACTUAL,
            customerId = "customer-1",
            step = MoneyAccountStatusValue.IssuingStep.STARTING,
        )

        // Act & Assert
        Truth.assertThat(issuing.totalFiatBalance).isEqualTo(
            TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
        )
    }

    @Test
    fun `GIVEN account without balance WHEN totalFiatBalance THEN zero`() {
        // Act & Assert
        Truth.assertThat(activeWith(balance = null).totalFiatBalance).isEqualTo(
            TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
        )
    }

    @Test
    fun `GIVEN Loading WHEN totalFiatBalance THEN Loading`() {
        // Act & Assert
        Truth.assertThat(MoneyAccountStatusValue.Loading.totalFiatBalance).isEqualTo(TotalFiatBalance.Loading)
    }
}