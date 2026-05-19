package com.tangem.feature.swap.domain.models.domain

sealed class SwapFeeState {
    data object Enough : SwapFeeState()
    data class NotEnough(
        val currencyName: String? = null,
        val currencySymbol: String? = null,
    ) : SwapFeeState()
}