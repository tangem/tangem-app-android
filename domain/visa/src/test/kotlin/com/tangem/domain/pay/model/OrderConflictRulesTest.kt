package com.tangem.domain.pay.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class OrderConflictRulesTest {

    private val cardA = "cardA"
    private val cardB = "cardB"

    @Test
    fun `IssueCard is blocked by an active issue order`() {
        val active = listOf(order(type = OrderType.CARD_ISSUE, status = OrderStatus.PROCESSING))

        val resolution = OrderConflictRules.resolve(OrderIntent.IssueCard, active)

        assertThat(resolution).isInstanceOf(ConflictResolution.Blocked::class.java)
    }

    @Test
    fun `IssueCard is blocked by an active additional-issue order`() {
        val active = listOf(order(type = OrderType.CARD_ISSUE_ADDITIONAL, status = OrderStatus.NEW))

        val resolution = OrderConflictRules.resolve(OrderIntent.IssueCard, active)

        assertThat(resolution).isInstanceOf(ConflictResolution.Blocked::class.java)
    }

    @Test
    fun `IssueCard is allowed when only withdraw is active`() {
        val active = listOf(order(type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING))

        val resolution = OrderConflictRules.resolve(OrderIntent.IssueCard, active)

        assertThat(resolution).isEqualTo(ConflictResolution.Allowed)
    }

    @Test
    fun `Freeze on cardA is blocked by an active freeze on cardA`() {
        val active = listOf(
            order(type = OrderType.CARD_FREEZE, status = OrderStatus.PROCESSING, productInstanceId = cardA),
        )

        val resolution = OrderConflictRules.resolve(OrderIntent.Freeze(cardA), active)

        assertThat(resolution).isInstanceOf(ConflictResolution.Blocked::class.java)
    }

    @Test
    fun `Freeze on cardA is allowed when freeze on cardB is active`() {
        val active = listOf(
            order(type = OrderType.CARD_FREEZE, status = OrderStatus.PROCESSING, productInstanceId = cardB),
        )

        val resolution = OrderConflictRules.resolve(OrderIntent.Freeze(cardA), active)

        assertThat(resolution).isEqualTo(ConflictResolution.Allowed)
    }

    @Test
    fun `Unfreeze on cardA is blocked by an active reissue on cardA`() {
        val active = listOf(
            order(type = OrderType.CARD_REISSUE, status = OrderStatus.PROCESSING, productInstanceId = cardA),
        )

        val resolution = OrderConflictRules.resolve(OrderIntent.Unfreeze(cardA), active)

        assertThat(resolution).isInstanceOf(ConflictResolution.Blocked::class.java)
    }

    @Test
    fun `Reissue on cardA is blocked by an active freeze on cardA`() {
        val active = listOf(
            order(type = OrderType.CARD_FREEZE, status = OrderStatus.PROCESSING, productInstanceId = cardA),
        )

        val resolution = OrderConflictRules.resolve(OrderIntent.Reissue(cardA), active)

        assertThat(resolution).isInstanceOf(ConflictResolution.Blocked::class.java)
    }

    @Test
    fun `Withdraw is blocked by an active withdraw`() {
        val active = listOf(order(type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING))

        val resolution = OrderConflictRules.resolve(OrderIntent.Withdraw, active)

        assertThat(resolution).isInstanceOf(ConflictResolution.Blocked::class.java)
    }

    @Test
    fun `Withdraw is allowed by an active card-scoped freeze`() {
        val active = listOf(
            order(type = OrderType.CARD_FREEZE, status = OrderStatus.PROCESSING, productInstanceId = cardA),
        )

        val resolution = OrderConflictRules.resolve(OrderIntent.Withdraw, active)

        assertThat(resolution).isEqualTo(ConflictResolution.Allowed)
    }

    @Test
    fun `Rename is never blocked`() {
        val active = listOf(
            order(type = OrderType.CARD_FREEZE, status = OrderStatus.PROCESSING, productInstanceId = cardA),
            order(type = OrderType.WITHDRAW, status = OrderStatus.PROCESSING),
            order(type = OrderType.CARD_ISSUE, status = OrderStatus.PROCESSING),
        )

        val resolution = OrderConflictRules.resolve(OrderIntent.Rename(cardA), active)

        assertThat(resolution).isEqualTo(ConflictResolution.Allowed)
    }

    @Test
    fun `Terminal-status orders never block`() {
        val terminal = listOf(
            order(type = OrderType.CARD_ISSUE, status = OrderStatus.COMPLETED),
            order(type = OrderType.CARD_ISSUE, status = OrderStatus.CANCELED),
        )

        val resolution = OrderConflictRules.resolve(OrderIntent.IssueCard, terminal)

        assertThat(resolution).isEqualTo(ConflictResolution.Allowed)
    }

    private fun order(
        type: OrderType,
        status: OrderStatus,
        productInstanceId: String? = null,
    ): Order = Order(
        id = "id-$type-$status",
        customerId = "customer",
        type = type,
        status = status,
        step = null,
        stepChangeCode = null,
        productInstanceId = productInstanceId,
        paymentAccountId = null,
        // Mirrors production: the v1 order response has no card id; conflicts match by productInstanceId.
        cardId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )
}