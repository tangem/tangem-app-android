package com.tangem.domain.walletconnect.model

import com.tangem.domain.wallets.models.UserWalletId

data class WcSessionApprove(
    val walletId: UserWalletId,
    val network: List<WcNetwork.Supported>,
)