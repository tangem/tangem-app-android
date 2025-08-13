package com.tangem.domain.managetokens.model

import com.tangem.domain.models.wallet.UserWalletId

data class ManageTokensListConfig(
    val userWalletId: UserWalletId?,
    val searchText: String?,
)