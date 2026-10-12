package com.tangem.domain.pay.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetPaymentAccountCryptoCurrencyStatusUseCaseTest {

    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private val useCase = GetPaymentAccountCryptoCurrencyStatusUseCase(
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    )

    @Test
    fun `GIVEN loaded with issued networks WHEN invoke THEN emits the first network status`() = runTest {
        // Arrange
        val accountStatus = statusWith(FIRST_NETWORK_STATUS, SECOND_NETWORK_STATUS)
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(accountStatus)

        // Act
        val emitted = useCase(USER_WALLET_ID).toList()

        // Assert
        assertThat(emitted).containsExactly(accountStatus to FIRST_NETWORK_STATUS)
    }

    @Test
    fun `GIVEN loaded without any status WHEN invoke THEN emits nothing`() = runTest {
        // Arrange
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(statusWith())

        // Act
        val emitted = useCase(USER_WALLET_ID).toList()

        // Assert
        assertThat(emitted).isEmpty()
    }

    @Test
    fun `GIVEN loaded with issued networks WHEN invokeSync THEN returns the first network status`() = runTest {
        // Arrange
        val accountStatus = statusWith(FIRST_NETWORK_STATUS, SECOND_NETWORK_STATUS)
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(accountStatus)

        // Act
        val result = useCase.invokeSync(USER_WALLET_ID)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(accountStatus to FIRST_NETWORK_STATUS)
    }

    @Test
    fun `GIVEN loaded without any status WHEN invokeSync THEN returns none`() = runTest {
        // Arrange
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(statusWith())

        // Act
        val result = useCase.invokeSync(USER_WALLET_ID)

        // Assert
        assertThat(result.isNone()).isTrue()
    }

    private fun statusWith(vararg statuses: CryptoCurrencyStatus) = paymentStatus(
        value = mockk(relaxed = true) {
            every { cryptoCurrencyStatuses } returns statuses.toList()
        },
    )

    private fun paymentStatus(value: PaymentAccountStatusValue.Loaded) = AccountStatus.Payment(
        account = Account.Payment(userWalletId = USER_WALLET_ID),
        value = value,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val FIRST_NETWORK_STATUS: CryptoCurrencyStatus = mockk()
        val SECOND_NETWORK_STATUS: CryptoCurrencyStatus = mockk()
    }
}