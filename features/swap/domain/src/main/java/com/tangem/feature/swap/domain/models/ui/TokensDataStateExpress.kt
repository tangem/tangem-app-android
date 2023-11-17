package com.tangem.feature.swap.domain.models.ui

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus

data class TokensDataStateExpress(
    val fromGroup: CurrenciesGroup,
    val toGroup: CurrenciesGroup,
)

data class CurrenciesGroup(
    val available: List<CryptoCurrencyStatus>,
    val unavailable: List<CryptoCurrencyStatus>,
)

data class FoundTokensStateExpress(
    val tokensInWallet: List<TokenWithBalanceExpress>,
    val loadedTokens: List<TokenWithBalanceExpress>,
)

data class TokenWithBalanceExpress(
    val token: CryptoCurrency,
    val tokenBalanceData: TokenBalanceDataExpress? = null,
)

data class TokenBalanceDataExpress(
    val amount: String?,
    val amountEquivalent: String?,
)
