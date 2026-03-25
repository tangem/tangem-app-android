package com.tangem.domain.managetokens.model

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId

data class ManageTokensListConfig(
    val accountId: AccountId?,
    val searchText: String?,
) {
    val userWalletId: UserWalletId?
        get() = accountId?.userWalletId
}