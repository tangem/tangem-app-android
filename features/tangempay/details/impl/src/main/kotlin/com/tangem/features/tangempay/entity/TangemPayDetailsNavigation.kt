package com.tangem.features.tangempay.entity

import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayDetailsNavigation {

    @Serializable
    data class Receive(val config: TokenReceiveConfig) : TangemPayDetailsNavigation()

    @Serializable
    data class TransactionDetails(val transaction: TangemPayTxHistoryItem) : TangemPayDetailsNavigation()
}