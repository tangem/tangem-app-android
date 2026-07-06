package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CreateVirtualAccountOrderUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk(relaxUnitFun = true)
    private val pollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val useCase = CreateVirtualAccountOrderUseCase(onboardingRepository, pollingUseCase)

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val paymentAccountAddress = "0xcollateral"

    @Test
    fun `GIVEN stored va order id WHEN invoke THEN skips creation and polling`() = runTest {
        coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns "existing-id"

        val result = useCase(userWalletId, paymentAccountAddress)

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { onboardingRepository.createVirtualAccountOrder(any(), any()) }
        coVerify(exactly = 0) { onboardingRepository.storeVirtualAccountOrderId(any(), any()) }
        coVerify(exactly = 0) { pollingUseCase.invoke(any(), any()) }
    }

    @Test
    fun `GIVEN no stored id and create succeeds WHEN invoke THEN stores id and starts polling`() = runTest {
        coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns null
        coEvery {
            onboardingRepository.createVirtualAccountOrder(userWalletId, paymentAccountAddress)
        } returns "new-id".right()

        val result = useCase(userWalletId, paymentAccountAddress)

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { onboardingRepository.storeVirtualAccountOrderId(userWalletId, "new-id") }
        coVerify(exactly = 1) { pollingUseCase.invoke(any(), userWalletId) }
    }

    @Test
    fun `GIVEN no stored id and create fails WHEN invoke THEN returns error and does not store or poll`() = runTest {
        coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns null
        coEvery {
            onboardingRepository.createVirtualAccountOrder(userWalletId, paymentAccountAddress)
        } returns VisaApiError.Unspecified.left()

        val result = useCase(userWalletId, paymentAccountAddress)

        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { onboardingRepository.storeVirtualAccountOrderId(any(), any()) }
        coVerify(exactly = 0) { pollingUseCase.invoke(any(), any()) }
    }
}