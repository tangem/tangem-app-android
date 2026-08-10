package com.tangem.feature.swap.domain.account

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentAccountUnderlyingCurrenciesTest {

    private val supplier: SingleAccountStatusListSupplier = mockk()
    private val sut = PaymentAccountUnderlyingCurrencies(supplier)

    @BeforeEach
    fun reset() = clearMocks(supplier)

    @Test
    fun `GIVEN loaded payment account WHEN get THEN returns its single currency status`() = runTest {
        // Arrange
        val walletId = UserWalletId("011")
        val paymentStatus = buildPaymentAccountStatus(walletId)
        coEvery {
            supplier.getSyncOrNull(SingleAccountStatusListProducer.Params(walletId))
        } returns accountStatusListWith(walletId, paymentStatus)

        // Act
        val result = sut.get(walletId)

        // Assert
        val expected = (paymentStatus.value as PaymentAccountStatusValue.Loaded).cryptoCurrencyStatus
        assertThat(result).containsExactly(expected)
    }

    @Test
    fun `GIVEN deactivated payment account WHEN get THEN returns its single currency status`() = runTest {
        // Arrange
        val walletId = UserWalletId("011")
        val paymentStatus = buildPaymentAccountStatus(walletId, deactivated = true)
        coEvery {
            supplier.getSyncOrNull(SingleAccountStatusListProducer.Params(walletId))
        } returns accountStatusListWith(walletId, paymentStatus)

        // Act
        val result = sut.get(walletId)

        // Assert
        val expected = (paymentStatus.value as PaymentAccountStatusValue.Deactivated).cryptoCurrencyStatus
        assertThat(result).containsExactly(expected)
    }

    @Test
    fun `GIVEN no payment account WHEN get THEN returns empty`() = runTest {
        // Arrange
        val walletId = UserWalletId("011")
        val cryptoPortfolioStatus: AccountStatus.CryptoPortfolio = mockk(relaxed = true)
        coEvery {
            supplier.getSyncOrNull(SingleAccountStatusListProducer.Params(walletId))
        } returns accountStatusListWith(walletId, cryptoPortfolioStatus)

        // Act
        val result = sut.get(walletId)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN supplier returns null WHEN get THEN returns empty`() = runTest {
        // Arrange
        val walletId = UserWalletId("011")
        coEvery {
            supplier.getSyncOrNull(SingleAccountStatusListProducer.Params(walletId))
        } returns null

        // Act
        val result = sut.get(walletId)

        // Assert
        assertThat(result).isEmpty()
    }

    private fun buildPaymentAccountStatus(userWalletId: UserWalletId, deactivated: Boolean = false): AccountStatus.Payment {
        val token: CryptoCurrency.Token = mockk(relaxed = true)
        val balance = PaymentAccountStatusValue.Balance(
            fiatBalance = PaymentAccountStatusValue.FiatBalance(availableBalance = BigDecimal.TEN, currency = "USD"),
            cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                id = "usdc",
                chainId = 137L,
                depositAddress = "0xdeposit",
                tokenContractAddress = "0xcontract",
                balance = BigDecimal.TEN,
            ),
            availableForWithdrawal = BigDecimal.TEN,
        )
        val value = if (deactivated) {
            PaymentAccountStatusValue.Deactivated(
                source = StatusSource.ACTUAL,
                customerId = "cust_1",
                balance = balance,
                cryptoCurrency = token,
                networks = emptyList(),
                fiatRate = BigDecimal.ONE,
                error = null,
            )
        } else {
            PaymentAccountStatusValue.Loaded(
                source = StatusSource.ACTUAL,
                customerId = "cust_1",
                depositAddress = "0xdeposit",
                balance = balance,
                cryptoCurrency = token,
                networks = emptyList(),
                cards = emptyList(),
                fiatRate = BigDecimal.ONE,
                error = null,
                virtualAccount = null,
                tariffPlan = null,
            )
        }
        return AccountStatus.Payment(account = Account.Payment(userWalletId), value = value)
    }

    private fun accountStatusListWith(userWalletId: UserWalletId, vararg statuses: AccountStatus): AccountStatusList {
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = statuses.toList(),
            totalAccounts = statuses.size,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }
}