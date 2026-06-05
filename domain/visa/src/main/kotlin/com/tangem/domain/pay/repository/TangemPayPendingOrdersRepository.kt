package com.tangem.domain.pay.repository

import com.tangem.domain.pay.model.TangemPayPendingOrder
import kotlinx.coroutines.flow.Flow

interface TangemPayPendingOrdersRepository {

    fun getAllFlow(): Flow<List<TangemPayPendingOrder>>

    suspend fun getAll(): List<TangemPayPendingOrder>

    suspend fun getByCard(cardId: String): List<TangemPayPendingOrder>

    suspend fun save(order: TangemPayPendingOrder)

    suspend fun remove(orderId: String)
}