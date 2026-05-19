package com.tangem.domain.pay

import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayDetailsConfig(
    val customerId: String,
    val cardId: String,
    val isPinSet: Boolean,
    val cardFrozenState: TangemPayCardFrozenState,
    val cardNumberEnd: String,
    val isReissuing: Boolean,
    val chainId: Int,
    val isTangemPayDeactivated: Boolean,
    val displayName: CardDisplayName?,
)