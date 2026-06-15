package com.tangem.domain.pay.util

import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderType

/**
 * Deterministic order selection:
 * 1. filter by [type];
 * 2. if a card is in scope, filter by `cardId` (or `productInstanceId` when `cardId` is missing);
 * 3. pick the latest by `updatedAt` (lexicographic ISO-8601 compare).
 *
 * Returns `null` when no order matches.
 */
object OrderResolver {

    fun selectActive(
        orders: List<Order>,
        type: OrderType,
        cardId: String? = null,
        productInstanceId: String? = null,
    ): Order? {
        return orders
            .asSequence()
            .filter { it.isActive }
            .filter { it.type == type }
            .filter { matchesCard(it, cardId, productInstanceId) }
            .maxByOrNull { it.updatedAt.orEmpty() }
    }

    fun selectLatest(
        orders: List<Order>,
        type: OrderType,
        cardId: String? = null,
        productInstanceId: String? = null,
    ): Order? {
        return orders
            .asSequence()
            .filter { it.type == type }
            .filter { matchesCard(it, cardId, productInstanceId) }
            .maxByOrNull { it.updatedAt.orEmpty() }
    }

    private fun matchesCard(order: Order, cardId: String?, productInstanceId: String?): Boolean {
        // No card scope requested → any card matches.
        if (cardId == null && productInstanceId == null) return true
        // Card-scope requested but order isn't card-scoped → no match.
        if (!order.isCardScoped) return false
        if (cardId != null && order.cardId != null) return order.cardId == cardId
        if (productInstanceId != null) return order.productInstanceId == productInstanceId
        return false
    }
}