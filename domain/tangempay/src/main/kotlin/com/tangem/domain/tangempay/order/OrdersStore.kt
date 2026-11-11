package com.tangem.domain.tangempay.order

import com.tangem.domain.models.wallet.UserWalletId

interface OrdersStore {

    suspend fun save(order: OrderRef)

    suspend fun remove(order: OrderRef)

    suspend fun getAll(userWalletId: UserWalletId): List<OrderRef>

    suspend fun clear(userWalletId: UserWalletId)
}