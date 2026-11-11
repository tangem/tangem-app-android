package com.tangem.domain.tangempay.order

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayOrder
import java.util.concurrent.ConcurrentHashMap

class ActiveOrdersRegistry {

    private val orders = ConcurrentHashMap<OrderRef, TangemPayOrder>()

    internal fun put(orderRef: OrderRef, snapshot: TangemPayOrder) {
        orders[orderRef] = snapshot
    }

    internal fun remove(orderRef: OrderRef) {
        orders.remove(orderRef)
    }

    internal fun get(userWalletId: UserWalletId): List<TangemPayOrder> {
        return orders.filterKeys { it.userWalletId == userWalletId }.values.toList()
    }
}