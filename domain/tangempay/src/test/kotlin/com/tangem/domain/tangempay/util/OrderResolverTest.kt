package com.tangem.domain.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.Order
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.OrderType
import org.junit.jupiter.api.Test

internal class OrderResolverTest {

    @Test
    fun `GIVEN orders of several types and statuses WHEN selectActive THEN filters by type and active status`() {
        // Arrange
        val orders = listOf(
            order(id = "1", type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING, updatedAt = "2026-01-01"),
            order(id = "2", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.PROCESSING, updatedAt = "2026-01-02"),
            order(id = "3", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.COMPLETED, updatedAt = "2026-01-03"),
        )

        // Act
        val result = OrderResolver.selectActive(orders = orders, type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC)

        // Assert
        assertThat(result?.id).isEqualTo("2")
    }

    @Test
    fun `GIVEN several active orders WHEN selectActive THEN picks the latest by updatedAt`() {
        // Arrange
        val orders = listOf(
            order(id = "old", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.NEW, updatedAt = "2026-01-01"),
            order(id = "new", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.PROCESSING, updatedAt = "2026-06-05"),
        )

        // Act
        val result = OrderResolver.selectActive(orders = orders, type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC)

        // Assert
        assertThat(result?.id).isEqualTo("new")
    }

    @Test
    fun `GIVEN no active order of the type WHEN selectActive THEN returns null`() {
        // Arrange
        val orders = listOf(
            order(id = "1", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.COMPLETED, updatedAt = "2026-01-01"),
            order(id = "2", type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING, updatedAt = "2026-01-02"),
        )

        // Act
        val result = OrderResolver.selectActive(orders = orders, type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN orders of several product instances WHEN selectActive THEN scopes by productInstanceId`() {
        // Arrange
        val orders = listOf(
            order(
                id = "card-a",
                type = OrderType.CARD_FREEZE,
                status = OrderStatus.PROCESSING,
                productInstanceId = "pi-a",
                updatedAt = "2026-01-02",
            ),
            order(
                id = "card-b",
                type = OrderType.CARD_FREEZE,
                status = OrderStatus.PROCESSING,
                productInstanceId = "pi-b",
                updatedAt = "2026-01-03",
            ),
        )

        // Act
        val result = OrderResolver.selectActive(
            orders = orders,
            type = OrderType.CARD_FREEZE,
            productInstanceId = "pi-a",
        )

        // Assert
        assertThat(result?.id).isEqualTo("card-a")
    }

    @Test
    fun `GIVEN a card scope WHEN selectActive THEN payment-account-level orders are ignored`() {
        // Arrange
        val orders = listOf(
            order(id = "account-level", type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING, updatedAt = "x"),
        )

        // Act
        val result = OrderResolver.selectActive(
            orders = orders,
            type = OrderType.WITHDRAW,
            productInstanceId = "pi-a",
        )

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN active virtual orders of different types WHEN selectActiveByTypes THEN latest virtual one is picked`() {
        // Arrange
        val orders = listOf(
            order(id = "kyc", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.NEW, updatedAt = "2026-01-01"),
            order(id = "rain", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN, status = OrderStatus.PROCESSING, updatedAt = "2026-01-03"),
            order(id = "done", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2, status = OrderStatus.COMPLETED, updatedAt = "2026-01-05"),
        )

        // Act
        val result = OrderResolver.selectActiveByTypes(orders = orders, types = OrderType.virtualIssueCardTypes)

        // Assert
        assertThat(result?.id).isEqualTo("rain")
    }

    @Test
    fun `GIVEN only an active plastic order WHEN selectActiveByTypes with virtual types THEN null`() {
        // Arrange
        val orders = listOf(
            order(id = "plastic", type = OrderType.CARD_ISSUE_PLASTIC_RAIN, status = OrderStatus.PROCESSING, updatedAt = "2026-01-01"),
        )

        // Act
        val result = OrderResolver.selectActiveByTypes(orders = orders, types = OrderType.virtualIssueCardTypes)

        // Assert
        assertThat(result).isNull()
    }

    private fun order(
        id: String,
        type: OrderType,
        status: OrderStatus,
        productInstanceId: String? = null,
        cardId: String? = null,
        updatedAt: String? = null,
    ): Order = Order(
        id = id,
        customerId = null,
        type = type,
        status = status,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = productInstanceId,
        paymentAccountId = null,
        cardId = cardId,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = updatedAt,
    )
}