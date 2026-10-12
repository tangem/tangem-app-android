package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CreateBankOfframpOrderUseCaseTest {

    private val repository: BankOfframpOrderRepository = mockk()
    private val useCase = CreateBankOfframpOrderUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN request and key WHEN invoke THEN repository receives the same key unchanged`() = runTest {
        // Arrange
        val result = BankOfframpOrderResult(orderId = "order-1", bankAccountId = "acc-1")
        coEvery { repository.createOrder(TEST_USER_WALLET_ID, orderRequest(), "key-1") } returns result.right()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID, orderRequest(), idempotencyKey = "key-1")

        // Assert
        assertThat(actual).isEqualTo(result.right())
        coVerify(exactly = 1) { repository.createOrder(TEST_USER_WALLET_ID, orderRequest(), "key-1") }
    }
}