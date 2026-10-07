package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FindActiveMoneyAccountCreationOrderUseCaseTest {

    private val findActiveOrdersUseCase: FindActiveOrdersUseCase = mockk()
    private val useCase = FindActiveMoneyAccountCreationOrderUseCase(findActiveOrdersUseCase = findActiveOrdersUseCase)

    @BeforeEach
    fun resetMocks() {
        clearMocks(findActiveOrdersUseCase)
    }

    @Test
    fun `GIVEN invoke WHEN orders requested THEN filtered by money account issue type`() = runTest {
        // Arrange
        stubActiveOrders()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) {
            findActiveOrdersUseCase(
                userWalletId = USER_WALLET_ID,
                types = setOf(OrderType.ACCOUNT_ISSUE_BANKING_RAIN),
            )
        }
    }

    @Test
    fun `GIVEN several money account orders WHEN invoke THEN the most recently updated returned`() = runTest {
        // Arrange
        val older = createOrder(id = "older", updatedAt = "2026-09-01T00:00:00Z")
        val newer = createOrder(id = "newer", updatedAt = "2026-09-02T00:00:00Z")
        stubActiveOrders(older, newer)

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(newer.right())
    }

    @Test
    fun `GIVEN orders of other types WHEN invoke THEN they are ignored`() = runTest {
        // Arrange
        val expected = createOrder(id = "account", updatedAt = "2026-09-01T00:00:00Z")
        stubActiveOrders(
            createOrder(id = "card", type = OrderType.CARD_ISSUE_PLASTIC_RAIN, updatedAt = "2026-09-03T00:00:00Z"),
            expected,
        )

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN no active orders WHEN invoke THEN null returned`() = runTest {
        // Arrange
        stubActiveOrders()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN a terminal order leaks through the backend filter WHEN invoke THEN it is dropped`() = runTest {
        // Arrange
        stubActiveOrders(createOrder(id = "completed", status = OrderStatus.COMPLETED))

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN find active orders fails WHEN invoke THEN error passed through`() = runTest {
        // Arrange
        coEvery { findActiveOrdersUseCase(any(), any()) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    private fun stubActiveOrders(vararg orders: Order) {
        coEvery { findActiveOrdersUseCase(any(), any()) } returns orders.toList().right()
    }

    private fun createOrder(
        id: String,
        type: OrderType = OrderType.ACCOUNT_ISSUE_BANKING_RAIN,
        status: OrderStatus = OrderStatus.PROCESSING,
        updatedAt: String? = null,
    ) = Order(
        id = id,
        customerId = null,
        type = type,
        status = status,
        step = OrderStep.STARTING,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = updatedAt,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}