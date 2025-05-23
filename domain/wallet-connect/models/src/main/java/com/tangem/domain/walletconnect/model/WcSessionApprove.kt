package com.tangem.domain.walletconnect.model

import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWallet

data class WcSessionApprove(
    val wallet: UserWallet,
    val network: List<Network>,
)
