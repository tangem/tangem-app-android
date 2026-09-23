package com.tangem.features.polymarket.impl.common.funding

import com.tangem.domain.polymarket.model.PolymarketDepositTarget

internal sealed interface RequisitesState {
    data object Loading : RequisitesState
    data object Failed : RequisitesState
    data class Ready(val target: PolymarketDepositTarget) : RequisitesState
}