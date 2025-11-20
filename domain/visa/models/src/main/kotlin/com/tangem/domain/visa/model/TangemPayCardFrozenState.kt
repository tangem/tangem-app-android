package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
sealed class TangemPayCardFrozenState {
    data object Pending : TangemPayCardFrozenState()
    data object Frozen : TangemPayCardFrozenState()
    data object Unfrozen : TangemPayCardFrozenState()
}