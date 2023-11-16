package com.tangem.feature.swap.domain.models.ui

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.models.domain.SwapPair

data class TokensDataStateExpress(
    val initialCryptoCurrency: CryptoCurrency,
    val preselectTokens: PreselectTokensExpress,
    val foundTokensState: FoundTokensStateExpress,
    val pairs: List<SwapPair>,
)

data class FoundTokensStateExpress(
    val tokensInWallet: List<TokenWithBalanceExpress>,
    val loadedTokens: List<TokenWithBalanceExpress>,
)

data class PreselectTokensExpress(
    val fromToken: CryptoCurrency,
    val toToken: CryptoCurrency,
)

data class TokenWithBalanceExpress(
    val token: CryptoCurrency,
    val tokenBalanceData: TokenBalanceDataExpress? = null,
)

data class TokenBalanceDataExpress(
    val amount: String?,
    val amountEquivalent: String?,
)