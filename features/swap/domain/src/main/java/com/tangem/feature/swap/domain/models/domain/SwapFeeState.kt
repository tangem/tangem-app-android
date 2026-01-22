package com.tangem.feature.swap.domain.models.domain

import com.tangem.domain.models.currency.CryptoCurrency

sealed class SwapFeeState {
    data object Enough : SwapFeeState()
    data class NotEnough(
        val feeCurrency: CryptoCurrency? = null,
        val currencyName: String? = null,
        val currencySymbol: String? = null,
    ) : SwapFeeState()
}