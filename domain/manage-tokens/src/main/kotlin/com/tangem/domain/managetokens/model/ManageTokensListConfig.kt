package com.tangem.domain.managetokens.model

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId

sealed interface ManageTokensListConfig {

    val userWalletId: UserWalletId?
    val searchText: String?

    // old way
    data class Wallet(
        override val userWalletId: UserWalletId?,
        override val searchText: String?,
    ) : ManageTokensListConfig

    // new way
    data class Account(
        val accountId: AccountId?,
        override val searchText: String?,
    ) : ManageTokensListConfig {

        override val userWalletId: UserWalletId?
            get() = accountId?.userWalletId
    }
}