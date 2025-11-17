package com.tangem.feature.swap.domain.models.ui

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.CryptoCurrencySwapInfo
import com.tangem.feature.swap.domain.models.domain.SwapProvider

data class TokensDataStateExpress(
    val fromGroup: CurrenciesGroup,
    val toGroup: CurrenciesGroup,
    val allProviders: List<SwapProvider>,
) {
    companion object {
        val EMPTY = TokensDataStateExpress(
            fromGroup = CurrenciesGroup(emptyList(), emptyList(), emptyList(), false),
            toGroup = CurrenciesGroup(emptyList(), emptyList(), emptyList(), false),
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
    val accountCurrencyList: List<AccountSwapAvailability>,
    val isAfterSearch: Boolean,
)

data class AccountSwapAvailability(
    val account: Account.CryptoPortfolio,
    val currencyList: List<AccountSwapCurrency>,
)

data class AccountSwapCurrency(
    val isAvailable: Boolean,
    val account: Account.CryptoPortfolio,
    val cryptoCurrencyStatus: CryptoCurrencyStatus,
    val providers: List<SwapProvider>,
)