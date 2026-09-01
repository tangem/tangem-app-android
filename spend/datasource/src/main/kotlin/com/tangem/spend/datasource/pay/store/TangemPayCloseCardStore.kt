package com.tangem.spend.datasource.pay.store

interface TangemPayCloseCardStore {

    suspend fun storeCloseOrderId(cardId: String, orderId: String)

    suspend fun removeCloseOrderId(cardId: String)

    suspend fun getOrderId(cardId: String): String?
}