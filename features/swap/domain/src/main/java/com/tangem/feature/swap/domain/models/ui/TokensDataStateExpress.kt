package com.tangem.feature.swap.domain.models.ui

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.models.domain.CryptoCurrencySwapInfo

data class TokensDataStateExpress(
    val initialCryptoCurrency: CryptoCurrency,
    val fromGroup: CurrenciesGroup,
    val toGroup: CurrenciesGroup,
)

data class CurrenciesGroup(
    val available: List<CryptoCurrencySwapInfo>,
    val unavailable: List<CryptoCurrencySwapInfo>,
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
