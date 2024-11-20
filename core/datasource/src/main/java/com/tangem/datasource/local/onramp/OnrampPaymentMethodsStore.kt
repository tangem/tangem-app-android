package com.tangem.datasource.local.onramp

import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO

interface OnrampPaymentMethodsStore {

    suspend fun getSyncOrNull(key: String): List<PaymentMethodDTO>?

    suspend fun store(key: String, value: List<PaymentMethodDTO>)

    suspend fun contains(key: String): Boolean
}
