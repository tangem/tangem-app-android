package com.tangem.domain.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import org.junit.jupiter.api.Test

internal class OrderResolverTest {

    @Test
    fun `selectActive filters by type and active status`() {
        val orders = listOf(
            order(id = "1", type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING, updatedAt = "2026-01-01"),
            order(id = "2", type = OrderType.CARD_ISSUE, status = OrderStatus.PROCESSING, updatedAt = "2026-01-02"),
            order(id = "3", type = OrderType.CARD_ISSUE, status = OrderStatus.COMPLETED, updatedAt = "2026-01-03"),
        )

        val result = OrderResolver.selectActive(orders = orders, type = OrderType.CARD_ISSUE)

        assertThat(result?.id).isEqualTo("2")
    }

    @Test
    fun `selectActive picks the latest by updatedAt`() {
        val orders = listOf(
            order(id = "old", type = OrderType.CARD_ISSUE, status = OrderStatus.NEW, updatedAt = "2026-01-01"),
            order(id = "new", type = OrderType.CARD_ISSUE, status = OrderStatus.PROCESSING, updatedAt = "2026-06-05"),
        )

        val result = OrderResolver.selectActive(orders = orders, type = OrderType.CARD_ISSUE)

        assertThat(result?.id).isEqualTo("new")
    }

    @Test
    fun `selectActive returns null when no active order of the type exists`() {
        val orders = listOf(
            order(id = "1", type = OrderType.CARD_ISSUE, status = OrderStatus.COMPLETED, updatedAt = "2026-01-01"),
            order(id = "2", type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING, updatedAt = "2026-01-02"),
        )

        val result = OrderResolver.selectActive(orders = orders, type = OrderType.CARD_ISSUE)

        assertThat(result).isNull()
    }

    @Test
    fun `selectActive scopes by productInstanceId`() {
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

        val result = OrderResolver.selectActive(
            orders = orders,
            type = OrderType.CARD_FREEZE,
            productInstanceId = "pi-a",
        )

        assertThat(result?.id).isEqualTo("card-a")
    }

    @Test
    fun `selectActive ignores payment-account-level orders when a card scope is requested`() {
        val orders = listOf(
            order(id = "account-level", type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING, updatedAt = "x"),
        )

        val result = OrderResolver.selectActive(
            orders = orders,
            type = OrderType.WITHDRAW,
            productInstanceId = "pi-a",
        )

        assertThat(result).isNull()
    }

    @Test
    fun `selectLatest includes terminal orders`() {
        val orders = listOf(
            order(id = "1", type = OrderType.CARD_ISSUE, status = OrderStatus.COMPLETED, updatedAt = "2026-01-05"),
            order(id = "2", type = OrderType.CARD_ISSUE, status = OrderStatus.NEW, updatedAt = "2026-01-01"),
        )

        val result = OrderResolver.selectLatest(orders = orders, type = OrderType.CARD_ISSUE)

        assertThat(result?.id).isEqualTo("1")
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
        step = null,
        stepChangeCode = null,
        productInstanceId = productInstanceId,
        paymentAccountId = null,
        cardId = cardId,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = updatedAt,
    )
}