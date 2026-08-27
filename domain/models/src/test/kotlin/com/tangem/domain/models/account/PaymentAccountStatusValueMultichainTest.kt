package com.tangem.domain.models.account

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PaymentAccountStatusValueMultichainTest {

    private val primaryCurrency: CryptoCurrency.Token = mockk()

    private fun balance() = PaymentAccountStatusValue.Balance(
        fiatBalance = PaymentAccountStatusValue.FiatBalance(BigDecimal("100"), "USD"),
        cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
            id = "usd-coin",
            chainId = 137,
            depositAddress = "0xDEPOSIT",
            tokenContractAddress = "0xCONTRACT",
            balance = BigDecimal("10"),
        ),
        availableForWithdrawal = BigDecimal("10"),
    )

    private fun loaded(
        networks: List<PaymentNetworkStatus>,
        balance: PaymentAccountStatusValue.Balance? = balance(),
    ) = PaymentAccountStatusValue.Loaded(
        source = StatusSource.ACTUAL,
        customerId = "c1",
        depositAddress = "0xDEPOSIT",
        balance = balance,
        cryptoCurrency = primaryCurrency,
        networks = networks,
        cards = emptyList(),
        fiatRate = BigDecimal("1.0"),
        error = null,
        virtualAccount = null,
        tariffPlan = null,
    )

    @Test
    fun `GIVEN no networks WHEN read statuses THEN falls back to single legacy status`() {
        val loaded = loaded(networks = emptyList())

        assertThat(loaded.networks).isEmpty()
        assertThat(loaded.cryptoCurrencyStatuses).containsExactly(loaded.cryptoCurrencyStatus)
    }

    @Test
    fun `GIVEN only non-Available networks WHEN read statuses THEN falls back to single legacy status`() {
        val loaded = loaded(
            networks = listOf(
                PaymentNetworkStatus.NotIssued(network = mockk(), cryptoCurrencies = listOf(mockk())),
                PaymentNetworkStatus.Disabled(network = mockk(), cryptoCurrencies = listOf(mockk())),
            ),
        )

        assertThat(loaded.cryptoCurrencyStatuses).containsExactly(loaded.cryptoCurrencyStatus)
    }

    @Test
    fun `GIVEN Available networks WHEN read statuses THEN flattens their statuses in order`() {
        val s1: CryptoCurrencyStatus = mockk()
        val s2: CryptoCurrencyStatus = mockk()
        val s3: CryptoCurrencyStatus = mockk()
        val loaded = loaded(
            networks = listOf(
                PaymentNetworkStatus.Available(
                    network = mockk(),
                    depositAddress = "0xDEPOSIT",
                    cryptoCurrencyStatuses = listOf(s1, s2),
                ),
                PaymentNetworkStatus.NotIssued(network = mockk(), cryptoCurrencies = listOf(mockk())),
                PaymentNetworkStatus.Available(
                    network = mockk(),
                    depositAddress = "0xDEPOSIT",
                    cryptoCurrencyStatuses = listOf(s3),
                ),
            ),
        )

        assertThat(loaded.cryptoCurrencyStatuses).containsExactly(s1, s2, s3).inOrder()
    }

    @Test
    fun `GIVEN no balance and no networks WHEN read statuses THEN empty and total balance failed`() {
        val loaded = loaded(networks = emptyList(), balance = null)

        assertThat(loaded.cryptoCurrencyStatus).isNull()
        assertThat(loaded.cryptoCurrencyStatuses).isEmpty()
        assertThat(loaded.totalFiatBalance).isEqualTo(TotalFiatBalance.Failed)
    }

    @Test
    fun `GIVEN no balance and Available networks WHEN read statuses THEN network statuses are used`() {
        val networkStatus: CryptoCurrencyStatus = mockk()
        val loaded = loaded(
            networks = listOf(
                PaymentNetworkStatus.Available(
                    network = mockk(),
                    depositAddress = "0xDEPOSIT",
                    cryptoCurrencyStatuses = listOf(networkStatus),
                ),
            ),
            balance = null,
        )

        assertThat(loaded.cryptoCurrencyStatuses).containsExactly(networkStatus)
    }
}