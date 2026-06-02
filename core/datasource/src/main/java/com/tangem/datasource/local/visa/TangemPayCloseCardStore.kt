package com.tangem.datasource.local.visa

interface TangemPayCloseCardStore {

    suspend fun setCloseOrderId(cardId: String, orderId: String?)

    suspend fun getOrderId(cardId: String): String?
}