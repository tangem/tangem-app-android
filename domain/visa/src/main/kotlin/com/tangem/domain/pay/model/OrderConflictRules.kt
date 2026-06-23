package com.tangem.domain.pay.model

/**
 * Decides whether a requested user action is allowed given the set of currently active orders
 * (an order is active while its status is NEW or PROCESSING).
 *
 * Rules:
 * - Issue (any card) — blocks Issue; does not block withdraw / freeze-unfreeze / rename of others.
 * - Freeze A         — blocks Freeze A and Unfreeze A.
 * - Unfreeze A       — symmetric to Freeze A.
 * - Withdraw         — blocks Withdraw; does not block freeze-unfreeze / rename.
 * - Reissue A        — blocks Freeze A / Unfreeze A / Reissue A.
 * - Rename           — never blocked.
 */
sealed interface ConflictResolution {
    data object Allowed : ConflictResolution

    /**
     * @property blockingOrder the active order that blocks the requested intent — useful for
     *           routing the user to the in-flight progress screen instead of a flat error.
     */
    data class Blocked(val blockingOrder: Order) : ConflictResolution
}

/**
 * Distinct user-driven intents that may conflict with active orders.
 *
 * Card-scoped intents carry `productInstanceId`: orders are matched by product instance because the
 * v1 order response carries `productInstanceId` but no card id (see [Order.cardId]). The caller has
 * the product instance via `TangemPayCard.productInstanceId`.
 */
sealed interface OrderIntent {
    data object IssueCard : OrderIntent
    data class Freeze(val productInstanceId: String) : OrderIntent
    data class Unfreeze(val productInstanceId: String) : OrderIntent
    data class Reissue(val productInstanceId: String) : OrderIntent
    data object Withdraw : OrderIntent
    data class Rename(val productInstanceId: String) : OrderIntent
}

/** Stateless evaluator of the order-conflict rules. */
object OrderConflictRules {

    fun resolve(intent: OrderIntent, activeOrders: List<Order>): ConflictResolution {
        val blockingOrder = activeOrders.firstOrNull { order -> blocks(intent, order) }
        return if (blockingOrder == null) ConflictResolution.Allowed else ConflictResolution.Blocked(blockingOrder)
    }

    private fun blocks(intent: OrderIntent, order: Order): Boolean {
        if (!order.isActive) return false
        return when (intent) {
            OrderIntent.IssueCard -> order.type.isIssuing
            OrderIntent.Withdraw -> order.type == OrderType.WITHDRAW
            is OrderIntent.Freeze -> sameProductInstance(order, intent.productInstanceId) &&
                order.type.isFreezeOrReissue()
            is OrderIntent.Unfreeze -> sameProductInstance(order, intent.productInstanceId) &&
                order.type.isFreezeOrReissue()
            is OrderIntent.Reissue -> sameProductInstance(order, intent.productInstanceId) &&
                order.type.isFreezeOrReissue()
            is OrderIntent.Rename -> false // Rename is never blocked.
        }
    }

    private fun sameProductInstance(order: Order, productInstanceId: String): Boolean {
        return order.productInstanceId == productInstanceId
    }

    private fun OrderType.isFreezeOrReissue(): Boolean {
        return this.isFreezingUnfreezing || this.isReissuing
    }
}