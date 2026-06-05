package com.tangem.datasource.local.visa

import com.tangem.datasource.local.visa.entity.TangemPayPendingOrderDM
import kotlinx.coroutines.flow.Flow

interface TangemPayPendingOrdersStore {

    fun getAllFlow(): Flow<List<TangemPayPendingOrderDM>>

    suspend fun getAll(): List<TangemPayPendingOrderDM>

    suspend fun getByCard(cardId: String): List<TangemPayPendingOrderDM>

    suspend fun save(order: TangemPayPendingOrderDM)

    suspend fun remove(orderId: String)
}