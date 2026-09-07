package com.tangem.features.jointaccount.join.model

import com.tangem.domain.models.wallet.UserWalletId

internal sealed interface JoinResultAction {
    data class OpenMembers(val userWalletId: UserWalletId, val cryptoAccountId: String) : JoinResultAction
    data object CloseFlow : JoinResultAction
    data object None : JoinResultAction
}