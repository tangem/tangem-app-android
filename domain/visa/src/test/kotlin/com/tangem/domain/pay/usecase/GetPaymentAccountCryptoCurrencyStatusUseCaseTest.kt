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
    fun `GIVEN loaded with account level balance WHEN invoke THEN emits account level status`() = runTest {
        // Arrange
        val accountStatus = accountLevelStatus()
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(accountStatus)

        // Act
        val emitted = useCase(USER_WALLET_ID).toList()

        // Assert
        assertThat(emitted).containsExactly(accountStatus to ACCOUNT_LEVEL_STATUS)
    }

    @Test
    fun `GIVEN loaded without balance but with network statuses WHEN invoke THEN emits network status`() = runTest {
        // Arrange
        val accountStatus = networkOnlyStatus()
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(accountStatus)

        // Act
        val emitted = useCase(USER_WALLET_ID).toList()

        // Assert — a response without account-level balances must not starve swap / tx history
        assertThat(emitted).containsExactly(accountStatus to NETWORK_STATUS)
    }

    @Test
    fun `GIVEN loaded without any status WHEN invoke THEN emits nothing`() = runTest {
        // Arrange
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(statusWithoutCurrency())

        // Act
        val emitted = useCase(USER_WALLET_ID).toList()

        // Assert
        assertThat(emitted).isEmpty()
    }

    @Test
    fun `GIVEN loaded without balance but with network statuses WHEN invokeSync THEN returns network status`() =
        runTest {
            // Arrange
            val accountStatus = networkOnlyStatus()
            every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(accountStatus)

            // Act
            val result = useCase.invokeSync(USER_WALLET_ID)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(accountStatus to NETWORK_STATUS)
        }

    @Test
    fun `GIVEN loaded without any status WHEN invokeSync THEN returns none`() = runTest {
        // Arrange
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(statusWithoutCurrency())

        // Act
        val result = useCase.invokeSync(USER_WALLET_ID)

        // Assert
        assertThat(result.isNone()).isTrue()
    }

    private fun accountLevelStatus() = paymentStatus(
        value = mockk(relaxed = true) {
            every { cryptoCurrencyStatus } returns ACCOUNT_LEVEL_STATUS
            every { cryptoCurrencyStatuses } returns listOf(ACCOUNT_LEVEL_STATUS)
        },
    )

    private fun networkOnlyStatus() = paymentStatus(
        value = mockk(relaxed = true) {
            every { cryptoCurrencyStatus } returns null
            every { cryptoCurrencyStatuses } returns listOf(NETWORK_STATUS)
        },
    )

    private fun statusWithoutCurrency() = paymentStatus(
        value = mockk(relaxed = true) {
            every { cryptoCurrencyStatus } returns null
            every { cryptoCurrencyStatuses } returns emptyList()
        },
    )

    private fun paymentStatus(value: PaymentAccountStatusValue.Loaded) = AccountStatus.Payment(
        account = Account.Payment(userWalletId = USER_WALLET_ID),
        value = value,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val ACCOUNT_LEVEL_STATUS: CryptoCurrencyStatus = mockk()
        val NETWORK_STATUS: CryptoCurrencyStatus = mockk()
    }
}