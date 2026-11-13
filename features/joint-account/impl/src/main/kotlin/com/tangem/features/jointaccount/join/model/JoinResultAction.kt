package com.tangem.features.jointaccount.join.model

import com.tangem.domain.models.wallet.UserWalletId

internal sealed interface JoinResultAction {
    data object CloseFlow : JoinResultAction
    data object None : JoinResultAction
    data class OpenMain(val userWalletId: UserWalletId) : JoinResultAction
}