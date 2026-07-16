package com.tangem.domain.account.status.usecase

import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAccountsModeEnabledUseCaseTest {

    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    @AfterEach
    fun tearDown() {
        clearMocks(multiAccountListSupplier, paymentAccountStatusSupplier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `returns false when supplier emits empty list`() = runTest {
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(emptyList())

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when single crypto portfolio account`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when two crypto portfolio accounts`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockCryptoPortfolio()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loaded`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.Loaded>())

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns false when payment account is NotCreated`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, PaymentAccountStatusValue.NotCreated)

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when payment account is Empty`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, PaymentAccountStatusValue.Empty)

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when payment account is UnderReview`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.UnderReview>())

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is IssuingCard`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.IssuingCard>())

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loading`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, PaymentAccountStatusValue.Loading)

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Deactivated`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.Deactivated>())

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when multiple wallets and one has two crypto portfolios`() = runTest {
            val list1 = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            val list2 = createAccountList(WALLET_ID_2, listOf(mockCryptoPortfolio(), mockCryptoPortfolio()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list1, list2))

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when multiple wallets and one has active payment`() = runTest {
            val list1 = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            val list2 = createAccountList(WALLET_ID_2, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list1, list2))
            mockPaymentStatus(WALLET_ID_2, mockk<PaymentAccountStatusValue.Loaded>())

            val actual = settledValue(createUseCase())

            Truth.assertThat(actual).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeSync {

        @Test
        fun `returns false when supplier emits empty list`() = runTest {
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(emptyList())
            val actual = invokeSyncSettled(createUseCase())

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when two crypto portfolio accounts`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockCryptoPortfolio()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            val actual = invokeSyncSettled(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loaded`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.Loaded>())
            val actual = invokeSyncSettled(createUseCase())

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns false when payment account is Empty`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns MutableStateFlow(listOf(list))
            mockPaymentStatus(WALLET_ID_1, PaymentAccountStatusValue.Empty)
            val actual = invokeSyncSettled(createUseCase())

            Truth.assertThat(actual).isFalse()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.createUseCase(): IsAccountsModeEnabledUseCase = IsAccountsModeEnabledUseCase(
        multiAccountListSupplier = multiAccountListSupplier,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
        appCoroutineScope = TestAppCoroutineScope(
            backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler),
        ),
    )

    /**
     * Returns the resolved value of the shared [IsAccountsModeEnabledUseCase.invoke] flow. The flow is
     * a hot [kotlinx.coroutines.flow.SharedFlow] (replay = 1) that never completes, so we read the
     * last value emitted while a subscriber is active.
     */
    private fun TestScope.settledValue(useCase: IsAccountsModeEnabledUseCase): Boolean =
        getEmittedValues(useCase.invoke()).last()

    /**
     * Samples via [IsAccountsModeEnabledUseCase.invokeSync] while a subscriber keeps the shared flow
     * warm — mirroring production, where a screen already observes the flow before invokeSync reads it.
     */
    private suspend fun TestScope.invokeSyncSettled(useCase: IsAccountsModeEnabledUseCase): Boolean {
        getEmittedValues(useCase.invoke())
        return useCase.invokeSync()
    }

    private fun mockCryptoPortfolio(): Account.CryptoPortfolio = mockk()

    private fun mockPaymentAccount(): Account.Payment = mockk()

    private fun mockPaymentStatus(walletId: UserWalletId, value: PaymentAccountStatusValue) {
        val status = mockk<AccountStatus.Payment> { every { this@mockk.value } returns value }
        every { paymentAccountStatusSupplier.invoke(walletId) } returns MutableStateFlow(status)
    }

    private fun createAccountList(walletId: UserWalletId, accounts: List<Account>): AccountList = mockk {
        every { userWalletId } returns walletId
        every { this@mockk.accounts } returns accounts
    }

    private companion object {
        val WALLET_ID_1 = UserWalletId(stringValue = "011")
        val WALLET_ID_2 = UserWalletId(stringValue = "022")
    }
}