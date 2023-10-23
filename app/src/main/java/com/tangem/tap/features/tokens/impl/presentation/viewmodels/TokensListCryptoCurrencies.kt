package com.tangem.tap.features.tokens.impl.presentation.viewmodels

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.TokenWithBlockchain

internal data class TokensListCryptoCurrencies(
    val coins: List<Blockchain>,
    val tokens: List<TokenWithBlockchain>,
)