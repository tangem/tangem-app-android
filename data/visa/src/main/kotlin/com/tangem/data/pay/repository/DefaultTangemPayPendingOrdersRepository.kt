package com.tangem.data.pay.repository

import com.tangem.datasource.local.visa.TangemPayPendingOrdersStore
import com.tangem.datasource.local.visa.entity.TangemPayPendingOrderDM
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayPendingOrdersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultTangemPayPendingOrdersRepository @Inject constructor(
    private val store: TangemPayPendingOrdersStore,
) : TangemPayPendingOrdersRepository {

    override fun getAllFlow(): Flow<List<TangemPayPendingOrder>> {
        return store.getAllFlow().map { orders -> orders.map { it.toDomain() } }
    }

    override suspend fun getAll(): List<TangemPayPendingOrder> {
        return store.getAll().map { it.toDomain() }
    }

    override suspend fun getByCard(cardId: String): List<TangemPayPendingOrder> {
        return store.getByCard(cardId).map { it.toDomain() }
    }

    override suspend fun save(order: TangemPayPendingOrder) {
        store.save(order.toDM())
    }

    override suspend fun remove(orderId: String) {
        store.remove(orderId)
    }

    private fun TangemPayPendingOrder.toDM() = TangemPayPendingOrderDM(
        orderId = orderId,
        userWalletId = userWalletId.stringValue,
        cardId = cardId,
        type = type.toString(),
        status = status.toString(),
    )

    private fun TangemPayPendingOrderDM.toDomain() = TangemPayPendingOrder(
        orderId = orderId,
        userWalletId = UserWalletId(userWalletId),
        cardId = cardId,
        type = TangemPayPendingOrder.Type.fromString(type),
        status = OrderStatus.fromString(status),
    )
}