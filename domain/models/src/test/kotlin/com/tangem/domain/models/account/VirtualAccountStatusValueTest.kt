package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Verifies that [VirtualAccountStatusValue.Active] converts its fiat balance to the app's selected currency
 * via [VirtualAccountStatusValue.Active.fiatRate] (mirror of the Payment account fix, [REDACTED_TASK_KEY]).
 */
class VirtualAccountStatusValueTest {

    private val cryptoCurrency = mockk<CryptoCurrency.Token>(relaxed = true)

    private fun activeWith(fiatRate: BigDecimal?, balance: BigDecimal = BigDecimal("100")) =
        VirtualAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            customerId = "customer",
            currencyCode = "USD",
            depositAddress = "0xabc",
            fiatBalance = VirtualAccountStatusValue.FiatBalance(availableBalance = balance, currency = "USD"),
            cryptoBalance = VirtualAccountStatusValue.CryptoBalance(
                id = "usd-coin",
                chainId = 137L,
                depositAddress = "0xabc",
                tokenContractAddress = "0xdef",
                balance = balance,
            ),
            availableForWithdrawal = balance,
            cryptoCurrency = cryptoCurrency,
            fiatRate = fiatRate,
        )

    @Test
    fun `totalFiatBalance converts balance via fiatRate when rate is present`() {
        // Arrange
        val rate = BigDecimal("0.9")
        val active = activeWith(fiatRate = rate, balance = BigDecimal("100"))

        // Act
        val result = active.totalFiatBalance

        // Assert
        Truth.assertThat(result).isInstanceOf(TotalFiatBalance.Loaded::class.java)
        Truth.assertThat((result as TotalFiatBalance.Loaded).amount)
            .isEqualTo(BigDecimal("100").multiply(rate))
    }

    @Test
    fun `totalFiatBalance is Failed when fiatRate is null`() {
        // Arrange
        val active = activeWith(fiatRate = null)

        // Act & Assert
        Truth.assertThat(active.totalFiatBalance).isEqualTo(TotalFiatBalance.Failed)
    }

    @Test
    fun `cryptoCurrencyStatus is NoQuote when fiatRate is null`() {
        // Arrange
        val active = activeWith(fiatRate = null)

        // Act & Assert
        Truth.assertThat(active.cryptoCurrencyStatus.value)
            .isInstanceOf(CryptoCurrencyStatus.NoQuote::class.java)
    }

    @Test
    fun `cryptoCurrencyStatus is Loaded with the rate when fiatRate is present`() {
        // Arrange
        val rate = BigDecimal("0.9")
        val active = activeWith(fiatRate = rate)

        // Act
        val value = active.cryptoCurrencyStatus.value

        // Assert
        Truth.assertThat(value).isInstanceOf(CryptoCurrencyStatus.Loaded::class.java)
        Truth.assertThat((value as CryptoCurrencyStatus.Loaded).fiatRate).isEqualTo(rate)
    }
}