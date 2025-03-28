package com.tangem.domain.walletconnect.model

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionProposal
import com.tangem.domain.wallets.models.UserWallet

data class WcSessionProposal(
    val dAppInfo: WcSdkSessionProposal,
// [REDACTED_TODO_COMMENT]
    val wallet: UserWallet,
    val missingChains: List<Network>,
    val availableChains: List<Network>,
    val notAddedChains: List<Network>,
)
