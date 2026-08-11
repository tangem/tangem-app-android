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

    @BeforeEach
    fun reset() = clearMocks(getPaymentAccountCryptoCurrencyStatusUseCase)

    @Test
    fun `GIVEN payment account present WHEN get THEN returns its single currency status`() = runTest {
        // Arrange
        val walletId = UserWalletId("011")
        val paymentStatus = MockAccounts.createPaymentAccountStatus(userWalletId = walletId)
        val expected = (paymentStatus.value as PaymentAccountStatusValue.Loaded).cryptoCurrencyStatus
        coEvery {
            getPaymentAccountCryptoCurrencyStatusUseCase.invokeSync(walletId)
        } returns (paymentStatus to expected).toOption()

        // Act
        val result = sut.get(walletId)

        // Assert
        assertThat(result).containsExactly(expected)
    }

    @Test
    fun `GIVEN no payment account WHEN get THEN returns empty`() = runTest {
        // Arrange
        val walletId = UserWalletId("011")
        coEvery { getPaymentAccountCryptoCurrencyStatusUseCase.invokeSync(walletId) } returns none()

        // Act
        val result = sut.get(walletId)

        // Assert
        assertThat(result).isEmpty()
    }
}