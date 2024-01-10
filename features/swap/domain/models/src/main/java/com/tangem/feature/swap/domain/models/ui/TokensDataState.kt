package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.domain.Currency

data class TokensDataState(
    val preselectTokens: PreselectTokens,
    val foundTokensState: FoundTokensState,
)

data class FoundTokensState(
    val tokensInWallet: List<TokenWithBalance>,
    val loadedTokens: List<TokenWithBalance>,
)

data class PreselectTokens(
    val fromToken: Currency,
    val toToken: Currency,
)

data class TokenWithBalance(
    val token: Currency,
    val tokenBalanceData: TokenBalanceData? = null,
)

data class TokenBalanceData(
    val amount: String?,
    val amountEquivalent: String?,
)
