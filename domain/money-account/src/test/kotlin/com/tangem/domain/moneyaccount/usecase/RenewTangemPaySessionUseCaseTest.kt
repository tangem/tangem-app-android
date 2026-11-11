package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.usecase.ProduceTangemPayInitialDataUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RenewTangemPaySessionUseCaseTest {

    private val produceTangemPayInitialDataUseCase: ProduceTangemPayInitialDataUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()

    private val useCase = RenewTangemPaySessionUseCase(
        produceTangemPayInitialDataUseCase = produceTangemPayInitialDataUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(produceTangemPayInitialDataUseCase, paymentAccountStatusFetcher, moneyAccountStatusFetcher)
    }

    @Test
    fun `GIVEN session renewed WHEN invoke THEN both statuses fetched and right returned`() = runTest {
        // Arrange
        coEvery { produceTangemPayInitialDataUseCase(USER_WALLET_ID) } returns Unit.right()
        coEvery { paymentAccountStatusFetcher.invoke(PAYMENT_PARAMS) } returns Unit.right()
        coEvery { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            paymentAccountStatusFetcher.invoke(PAYMENT_PARAMS)
            moneyAccountStatusFetcher.invoke(USER_WALLET_ID)
        }
    }

    @Test
    fun `GIVEN session renewal fails WHEN invoke THEN statuses not fetched and left returned`() = runTest {
        // Arrange
        val error = IllegalStateException("Error")
        coEvery { produceTangemPayInitialDataUseCase(USER_WALLET_ID) } returns error.left()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(error.left())
        coVerify(exactly = 0) {
            paymentAccountStatusFetcher.invoke(any<PaymentAccountStatusFetcher.Params>())
            moneyAccountStatusFetcher.invoke(any<MoneyAccountStatusFetcher.Params>())
        }
    }

    @Test
    fun `GIVEN Payment fetch in progress WHEN invoke THEN Money status fetched concurrently`() = runTest {
        // Arrange
        coEvery { produceTangemPayInitialDataUseCase(USER_WALLET_ID) } returns Unit.right()
        coEvery { paymentAccountStatusFetcher.invoke(PAYMENT_PARAMS) } coAnswers { awaitCancellation() }
        coEvery { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        // Act
        val job = launch { useCase(USER_WALLET_ID) }
        runCurrent()

        // Assert
        coVerify(exactly = 1) { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) }
        assertThat(job.isCompleted).isFalse()

        job.cancel()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val PAYMENT_PARAMS = PaymentAccountStatusFetcher.Params(USER_WALLET_ID)
    }
}