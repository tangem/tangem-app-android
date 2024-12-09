package com.tangem.datasource.local.onramp.paymentmethods

import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import kotlinx.coroutines.flow.Flow

interface OnrampPaymentMethodsStore {

    suspend fun getSyncOrNull(key: String): List<PaymentMethodDTO>?

    fun get(key: String): Flow<List<PaymentMethodDTO>>

    suspend fun store(key: String, value: List<PaymentMethodDTO>)

    suspend fun contains(key: String): Boolean

    suspend fun clear()
}