package com.tangem.datasource.local.visa

interface TangemPayCloseCardStore {

    suspend fun storeCloseOrderId(cardId: String, orderId: String)

    suspend fun removeCloseOrderId(cardId: String)

    suspend fun getOrderId(cardId: String): String?
}