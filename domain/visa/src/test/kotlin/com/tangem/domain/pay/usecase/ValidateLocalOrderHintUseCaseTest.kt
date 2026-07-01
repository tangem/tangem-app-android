package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ValidateLocalOrderHintUseCaseTest {

    private val orderRepository: CustomerOrderRepository = mockk()
    private val onboardingRepository: OnboardingRepository = mockk(relaxUnitFun = true)
    private val useCase = ValidateLocalOrderHintUseCase(orderRepository, onboardingRepository)
    private val userWalletId = UserWalletId("1234567890ABCDEF")

    @Test
    fun `WHEN no hint exists THEN returns null`() = runTest {
        coEvery { onboardingRepository.getOrderId(userWalletId) } returns null

        val result = useCase(userWalletId)

        assertThat(result.getOrNull()).isNull()
        coVerify(exactly = 0) { orderRepository.getOrderData(any(), any()) }
    }

    @Test
    fun `WHEN hint points to active order THEN returns it`() = runTest {
        coEvery { onboardingRepository.getOrderId(userWalletId) } returns "order-id"
        val orderData = OrderData(customerId = "c1", status = OrderStatus.PROCESSING, withdrawTxHash = null)
        coEvery { orderRepository.getOrderData(userWalletId, "order-id") } returns orderData.right()

        val result = useCase(userWalletId)

        assertThat(result.getOrNull()).isEqualTo(orderData)
        coVerify(exactly = 0) { onboardingRepository.clearOrderId(any()) }
    }

    @Test
    fun `WHEN hint points to terminal order THEN clears hint and returns null`() = runTest {
        coEvery { onboardingRepository.getOrderId(userWalletId) } returns "order-id"
        val terminal = OrderData(customerId = "c1", status = OrderStatus.COMPLETED, withdrawTxHash = null)
        coEvery { orderRepository.getOrderData(userWalletId, "order-id") } returns terminal.right()

        val result = useCase(userWalletId)

        assertThat(result.getOrNull()).isNull()
        coVerify(exactly = 1) { onboardingRepository.clearOrderId(userWalletId) }
    }

    @Test
    fun `WHEN repository fails THEN propagates error and does not clear hint`() = runTest {
        coEvery { onboardingRepository.getOrderId(userWalletId) } returns "order-id"
        coEvery { orderRepository.getOrderData(userWalletId, "order-id") } returns VisaApiError.Unspecified.left()

        val result = useCase(userWalletId)

        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { onboardingRepository.clearOrderId(any()) }
    }
}