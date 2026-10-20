package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.network.NetworkAddress
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
    fun `GIVEN balance with rate WHEN cryptoCurrencyStatus THEN loaded status at the deposit address`() {
        // Arrange
        val account = activeWith(balanceOf(fiatRate = BigDecimal("0.99"))).copy(source = StatusSource.CACHE)

        // Act
        val actual = account.cryptoCurrencyStatus

        // Assert
        val expected = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal("100"),
                fiatAmount = BigDecimal("100").multiply(BigDecimal("0.99")),
                fiatRate = BigDecimal("0.99"),
                priceChange = BigDecimal.ZERO,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = DEPOSIT_NETWORK_ADDRESS,
                sources = CACHE_SOURCES,
            ),
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN balance without rate WHEN cryptoCurrencyStatus THEN no quote status at the deposit address`() {
        // Arrange
        val account = activeWith(balanceOf(fiatRate = null)).copy(source = StatusSource.CACHE)

        // Act
        val actual = account.cryptoCurrencyStatus

        // Assert
        val expected = CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal("100"),
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = DEPOSIT_NETWORK_ADDRESS,
                sources = CACHE_SOURCES,
            ),
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN account without balance WHEN cryptoCurrencyStatus THEN null`() {
        // Act & Assert
        Truth.assertThat(activeWith(balance = null).cryptoCurrencyStatus).isNull()
    }

    @Test
    fun `GIVEN Loading WHEN totalFiatBalance THEN Loading`() {
        // Act & Assert
        Truth.assertThat(MoneyAccountStatusValue.Loading.totalFiatBalance).isEqualTo(TotalFiatBalance.Loading)
    }

    private companion object {
        val DEPOSIT_NETWORK_ADDRESS = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(type = NetworkAddress.Address.Type.Primary, value = "0xabc"),
        )
        val CACHE_SOURCES = CryptoCurrencyStatus.Sources(
            networkSource = StatusSource.CACHE,
            quoteSource = StatusSource.CACHE,
            stakingBalanceSource = StatusSource.CACHE,
        )
    }
}