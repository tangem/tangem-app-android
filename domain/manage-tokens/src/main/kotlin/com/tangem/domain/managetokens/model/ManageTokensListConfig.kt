package com.tangem.domain.managetokens.model

import com.tangem.domain.wallets.models.UserWalletId

data class ManageTokensListConfig(
    val userWalletId: UserWalletId?,
    val searchText: String?,
)
