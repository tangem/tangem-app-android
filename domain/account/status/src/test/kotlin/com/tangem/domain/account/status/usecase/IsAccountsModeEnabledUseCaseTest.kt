package com.tangem.domain.account.status.usecase

import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusProducer
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAccountsModeEnabledUseCaseTest {

    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private val useCase = IsAccountsModeEnabledUseCase(
        multiAccountListSupplier = multiAccountListSupplier,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(multiAccountListSupplier, paymentAccountStatusSupplier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `returns false when supplier emits empty list`() = runTest {
            every { multiAccountListSupplier.invoke() } returns flowOf(emptyList())

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when single crypto portfolio account`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when two crypto portfolio accounts`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockCryptoPortfolio()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loaded`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.Loaded>())

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Locked`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.Locked>())

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns false when payment account is NotCreated`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, PaymentAccountStatusValue.NotCreated)

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when payment account is Empty`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, PaymentAccountStatusValue.Empty)

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when payment account is UnderReview`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.UnderReview>())

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is IssuingCard`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.IssuingCard>())

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loading`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, PaymentAccountStatusValue.Loading)

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Deactivated`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list))
            mockPaymentStatus(WALLET_ID_1, mockk<PaymentAccountStatusValue.Deactivated>())

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when multiple wallets and one has two crypto portfolios`() = runTest {
            val list1 = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            val list2 = createAccountList(WALLET_ID_2, listOf(mockCryptoPortfolio(), mockCryptoPortfolio()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list1, list2))

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when multiple wallets and one has active payment`() = runTest {
            val list1 = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            val list2 = createAccountList(WALLET_ID_2, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(list1, list2))
            mockPaymentStatus(WALLET_ID_2, mockk<PaymentAccountStatusValue.Loaded>())

            val actual = useCase.invoke().last()

            Truth.assertThat(actual).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeSync {

        @Test
        fun `returns false when getSyncOrNull returns null`() = runTest {
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns null

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when getSyncOrNull returns empty list`() = runTest {
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns emptyList()

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when single crypto portfolio account`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when two crypto portfolio accounts`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockCryptoPortfolio()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loaded`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)
            mockPaymentStatusSync(WALLET_ID_1, mockk<PaymentAccountStatusValue.Loaded>())

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Locked`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)
            mockPaymentStatusSync(WALLET_ID_1, mockk<PaymentAccountStatusValue.Locked>())

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns false when payment account is NotCreated`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)
            mockPaymentStatusSync(WALLET_ID_1, PaymentAccountStatusValue.NotCreated)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when payment account is Empty`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)
            mockPaymentStatusSync(WALLET_ID_1, PaymentAccountStatusValue.Empty)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when payment account is UnderReview`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)
            mockPaymentStatusSync(WALLET_ID_1, mockk<PaymentAccountStatusValue.UnderReview>())

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Deactivated`() = runTest {
            val list = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list)
            mockPaymentStatusSync(WALLET_ID_1, mockk<PaymentAccountStatusValue.Deactivated>())

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when multiple wallets and one has loaded payment`() = runTest {
            val list1 = createAccountList(WALLET_ID_1, listOf(mockCryptoPortfolio()))
            val list2 = createAccountList(WALLET_ID_2, listOf(mockCryptoPortfolio(), mockPaymentAccount()))
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(list1, list2)
            mockPaymentStatusSync(WALLET_ID_2, mockk<PaymentAccountStatusValue.Loaded>())

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }
    }

    private fun mockCryptoPortfolio(): Account.CryptoPortfolio = mockk()

    private fun mockPaymentAccount(): Account.Payment = mockk()

    private fun mockPaymentStatus(walletId: UserWalletId, value: PaymentAccountStatusValue) {
        val status = mockk<AccountStatus.Payment> { every { this@mockk.value } returns value }
        every { paymentAccountStatusSupplier.invoke(walletId) } returns flowOf(status)
    }

    private fun mockPaymentStatusSync(walletId: UserWalletId, value: PaymentAccountStatusValue) {
        val status = mockk<AccountStatus.Payment> { every { this@mockk.value } returns value }
        coEvery {
            paymentAccountStatusSupplier.getSyncOrNull(PaymentAccountStatusProducer.Params(walletId), any())
        } returns status
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