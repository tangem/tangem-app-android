package com.tangem.core.ui.components.marketprice

import androidx.compose.runtime.Immutable

@Immutable
sealed interface PriceChangeState {

    data class Content(val valueInPercent: String, val type: PriceChangeType) : PriceChangeState

    data object Unknown : PriceChangeState
}