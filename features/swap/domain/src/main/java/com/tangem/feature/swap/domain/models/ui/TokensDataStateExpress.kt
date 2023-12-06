package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.domain.CryptoCurrencySwapInfo

data class TokensDataStateExpress(
    val fromGroup: CurrenciesGroup,
    val toGroup: CurrenciesGroup,
) {
    companion object {
        val EMPTY =
            TokensDataStateExpress(
                fromGroup = CurrenciesGroup(emptyList(), emptyList()),
                toGroup = CurrenciesGroup(emptyList(), emptyList()),
            )
    }
}

data class CurrenciesGroup(
    val available: List<CryptoCurrencySwapInfo>,
    val unavailable: List<CryptoCurrencySwapInfo>,
)
