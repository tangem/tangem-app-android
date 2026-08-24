package com.tangem.feature.swap.domain.account

import arrow.core.none
import arrow.core.toOption
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentAccountUnderlyingCurrenciesTest {

    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase = mockk()
    private val sut = PaymentAccountUnderlyingCurrencies(getPaymentAccountCryptoCurrencyStatusUseCase)

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun reset() = clearMocks(getPaymentAccountCryptoCurrencyStatusUseCase)

    @Test
    fun `GIVEN account holds several currencies WHEN get THEN returns all of them`() = runTest {
        // Arrange
        val usdcPolygon = paymentCurrencyStatus()
        val usdcTron = paymentCurrencyStatus()
        coEvery {
            getPaymentAccountCryptoCurrencyStatusUseCase.invokeSyncCurrencies(walletId)
        } returns listOf(usdcPolygon, usdcTron)

        // Act
        val result = sut.get(walletId)

        // Assert
        assertThat(result).containsExactly(usdcPolygon, usdcTron).inOrder()
    }

    @Test
    fun `GIVEN no payment account WHEN get THEN returns empty`() = runTest {
        // Arrange
        coEvery { getPaymentAccountCryptoCurrencyStatusUseCase.invokeSyncCurrencies(walletId) } returns emptyList()

        // Act
        val result = sut.get(walletId)

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN account holds several currencies WHEN getWithdrawable THEN returns only the default one`() = runTest {
        // Arrange
        val defaultCurrency = paymentCurrencyStatus()
        val paymentStatus = MockAccounts.createPaymentAccountStatus(userWalletId = walletId)
        coEvery {
            getPaymentAccountCryptoCurrencyStatusUseCase.invokeSync(walletId)
        } returns (paymentStatus to defaultCurrency).toOption()

        // Act
        val result = sut.getWithdrawable(walletId)

        // Assert
        assertThat(result).containsExactly(defaultCurrency)
    }

    @Test
    fun `GIVEN no payment account WHEN getWithdrawable THEN returns empty`() = runTest {
        // Arrange
        coEvery { getPaymentAccountCryptoCurrencyStatusUseCase.invokeSync(walletId) } returns none()

        // Act
        val result = sut.getWithdrawable(walletId)

        // Assert
        assertThat(result).isEmpty()
    }

    private fun paymentCurrencyStatus() = requireNotNull(
        (MockAccounts.createPaymentAccountStatus(userWalletId = walletId).value as PaymentAccountStatusValue.Loaded)
            .cryptoCurrencyStatus,
    )
}