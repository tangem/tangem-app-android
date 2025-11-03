package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.tokenlist.TokenList

sealed interface TokenConverterParams {
    data class Wallet(
        val portfolioId: PortfolioId,
        val tokenList: TokenList,
    ) : TokenConverterParams

    data class Account(
        val accountList: AccountStatusList,
        val expandedAccounts: Set<AccountId>,
    ) : TokenConverterParams
}