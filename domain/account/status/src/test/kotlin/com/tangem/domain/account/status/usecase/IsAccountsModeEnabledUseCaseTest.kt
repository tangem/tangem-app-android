package com.tangem.domain.account.status.usecase

import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAccountsModeEnabledUseCaseTest {

    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()

    private val useCase = IsAccountsModeEnabledUseCase(multiAccountStatusListSupplier = multiAccountStatusListSupplier)

    @AfterEach
    fun tearDown() {
        clearMocks(multiAccountStatusListSupplier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `returns false when supplier emits empty list`() = runTest {
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(emptyList())

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when single crypto portfolio account`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio()),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when two crypto portfolio accounts`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockCryptoPortfolio()),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loaded`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.Loaded>())),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Locked`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.Locked>())),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns false when payment account is NotCreated`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(PaymentAccountStatusValue.NotCreated)),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when payment account is Empty`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(PaymentAccountStatusValue.Empty)),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when payment account is UnderReview`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.UnderReview>())),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is IssuingCard`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.IssuingCard>())),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loading`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(PaymentAccountStatusValue.Loading)),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN payment account is Deactivated WHEN invoke THEN returns true`() = runTest {
            // GIVEN
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.Deactivated>())),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList))

            // WHEN
            val actual = useCase.invoke().first()

            // THEN
            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when multiple status lists and one has two crypto portfolios`() = runTest {
            val statusList1 = createAccountStatusList(statuses = listOf(mockCryptoPortfolio()))
            val statusList2 = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockCryptoPortfolio()),
            )
            every { multiAccountStatusListSupplier.invoke() } returns flowOf(listOf(statusList1, statusList2))

            val actual = useCase.invoke().first()

            Truth.assertThat(actual).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeSync {

        @Test
        fun `returns false when getSyncOrNull returns null`() = runTest {
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns null

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when getSyncOrNull returns empty list`() = runTest {
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns emptyList()

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when single crypto portfolio account`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio()),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when two crypto portfolio accounts`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockCryptoPortfolio()),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Loaded`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.Loaded>())),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when payment account is Locked`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.Locked>())),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns false when payment account is NotCreated`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(PaymentAccountStatusValue.NotCreated)),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when payment account is Empty`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(PaymentAccountStatusValue.Empty)),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when payment account is UnderReview`() = runTest {
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.UnderReview>())),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN payment account is Deactivated WHEN invokeSync THEN returns true`() = runTest {
            // GIVEN
            val statusList = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.Deactivated>())),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList)

            // WHEN
            val actual = useCase.invokeSync()

            // THEN
            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when multiple status lists and one has loaded payment`() = runTest {
            val statusList1 = createAccountStatusList(statuses = listOf(mockCryptoPortfolio()))
            val statusList2 = createAccountStatusList(
                statuses = listOf(mockCryptoPortfolio(), mockPayment(mockk<PaymentAccountStatusValue.Loaded>())),
            )
            coEvery { multiAccountStatusListSupplier.getSyncOrNull(Unit, any()) } returns listOf(statusList1, statusList2)

            val actual = useCase.invokeSync()

            Truth.assertThat(actual).isTrue()
        }
    }

    private fun mockCryptoPortfolio(): AccountStatus.CryptoPortfolio = mockk()

    private fun mockPayment(value: PaymentAccountStatusValue): AccountStatus.Payment = mockk {
        every { this@mockk.value } returns value
    }

    private fun createAccountStatusList(statuses: List<AccountStatus>): AccountStatusList = mockk {
        every { accountStatuses } returns statuses
    }
}