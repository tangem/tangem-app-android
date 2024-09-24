package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.domain.CryptoCurrencySwapInfo
import com.tangem.feature.swap.domain.models.domain.SwapProvider

data class TokensDataStateExpress(
    val fromGroup: CurrenciesGroup,
    val toGroup: CurrenciesGroup,
    val allProviders: List<SwapProvider>,
) {
    companion object {
        val EMPTY =
            TokensDataStateExpress(
                fromGroup = CurrenciesGroup(emptyList(), emptyList(), false),
                toGroup = CurrenciesGroup(emptyList(), emptyList(), false),
                allProviders = emptyList(),
            )
    }
}

fun TokensDataStateExpress.getGroupWithReverse(isReverseFromTo: Boolean): CurrenciesGroup {
    return if (isReverseFromTo) {
        this.fromGroup
    } else {
        this.toGroup
    }
}

data class CurrenciesGroup(
    val available: List<CryptoCurrencySwapInfo>,
    val unavailable: List<CryptoCurrencySwapInfo>,
    val afterSearch: Boolean,
)