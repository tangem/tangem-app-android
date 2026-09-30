package com.tangem.spend.datasource.pay.serialization

import com.tangem.spend.datasource.pay.models.response.TangemPayTxHistoryResponse
import com.tangem.utils.serialization.SerializersModuleProvider
import com.tangem.utils.serialization.polymorphicUnknownFallback
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.modules.SerializersModule

object TangemPaySerializersModuleProvider : SerializersModuleProvider {

    override fun provide(): SerializersModule = SerializersModule {
        polymorphicUnknownFallback<TangemPayTxHistoryResponse.Transaction> { type, payload ->
            TangemPayTxHistoryResponse.Transaction.Unknown(
                id = (payload["id"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                type = type,
                payload = payload,
            )
        }
    }
}