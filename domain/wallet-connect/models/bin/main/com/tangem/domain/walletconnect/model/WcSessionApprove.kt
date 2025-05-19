package com.tangem.domain.walletconnect.model

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet

data class WcSessionApprove(
    val wallet: UserWallet,
    val network: List<Network>,
)