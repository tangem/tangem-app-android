package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet

data class TokenListWithWallet(
    val tokenList: TokenList,
    val wallet: UserWallet,
)