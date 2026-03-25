package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.tokenlist.TokenList

sealed interface TokenConverterParams {
    /** Wallet mode; list of tokens for main account */
    data class Wallet(
        val accountId: AccountId,
        val tokenList: TokenList,
    ) : TokenConverterParams

    /** Account mode; list of accounts */
    data class Account(
        val accountList: AccountStatusList,
        val expandedAccounts: Set<AccountId>,
    ) : TokenConverterParams
}