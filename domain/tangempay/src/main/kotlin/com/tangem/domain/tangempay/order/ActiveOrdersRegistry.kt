package com.tangem.domain.tangempay.order

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class ActiveOrdersRegistry {

    private val orders = MutableStateFlow<Map<OrderRef, TangemPayOrder>>(emptyMap())

    internal fun put(orderRef: OrderRef, snapshot: TangemPayOrder) {
        orders.update { it + (orderRef to snapshot) }
    }

    internal fun remove(orderRef: OrderRef) {
        orders.update { it - orderRef }
    }

    internal fun get(userWalletId: UserWalletId): List<TangemPayOrder> {
        return orders.value.ordersOf(userWalletId)
    }

    fun observe(userWalletId: UserWalletId): Flow<List<TangemPayOrder>> {
        return orders.map { it.ordersOf(userWalletId) }.distinctUntilChanged()
    }

    private fun Map<OrderRef, TangemPayOrder>.ordersOf(userWalletId: UserWalletId): List<TangemPayOrder> {
        return filterKeys { it.userWalletId == userWalletId }.values.toList()
    }
}