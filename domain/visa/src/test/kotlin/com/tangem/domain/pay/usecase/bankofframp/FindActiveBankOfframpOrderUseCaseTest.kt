package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FindActiveBankOfframpOrderUseCaseTest {

    private val repository: CustomerOrderRepository = mockk()
    private val useCase = FindActiveBankOfframpOrderUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN two active orders WHEN invoke THEN the most recently updated returned`() = runTest {
        // Arrange
        val older = order(id = "older", updatedAt = "2026-09-20T10:00:00Z")
        val newer = order(id = "newer", updatedAt = "2026-09-21T10:00:00Z")
        stubFindOrders(older, newer)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(newer.right())
    }

    @Test
    fun `GIVEN invoke WHEN orders requested THEN filtered by bank offramp type and active statuses`() = runTest {
        // Arrange
        stubFindOrders()

        // Act
        useCase(TEST_USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) {
            repository.findOrders(
                userWalletId = TEST_USER_WALLET_ID,
                types = setOf(OrderType.BANK_OFFRAMP_RAIN),
                statuses = OrderStatus.activeStatuses,
            )
        }
    }

    @Test
    fun `GIVEN no active orders WHEN invoke THEN null returned`() = runTest {
        // Arrange
        stubFindOrders()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN an order without updatedAt WHEN invoke THEN it does not win over a timestamped one`() = runTest {
        // Arrange
        val undated = order(id = "undated", updatedAt = null)
        val dated = order(id = "dated", updatedAt = "2026-09-21T10:00:00Z")
        stubFindOrders(undated, dated)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(dated.right())
    }

    @Test
    fun `GIVEN a terminal order leaks through the backend filter WHEN invoke THEN it is dropped`() = runTest {
        // Arrange
        stubFindOrders(order(id = "completed", updatedAt = "2026-09-21T10:00:00Z", status = OrderStatus.COMPLETED))

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN findOrders fails WHEN invoke THEN the error is passed through`() = runTest {
        // Arrange
        coEvery { repository.findOrders(any(), any(), any()) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private fun stubFindOrders(vararg orders: Order) {
        coEvery {
            repository.findOrders(
                userWalletId = TEST_USER_WALLET_ID,
                types = setOf(OrderType.BANK_OFFRAMP_RAIN),
                statuses = OrderStatus.activeStatuses,
            )
        } returns orders.toList().right()
    }

    private fun order(
        id: String,
        updatedAt: String?,
        status: OrderStatus = OrderStatus.PROCESSING,
    ): Order = Order(
        id = id,
        customerId = "customer",
        type = OrderType.BANK_OFFRAMP_RAIN,
        status = status,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = updatedAt,
    )
}