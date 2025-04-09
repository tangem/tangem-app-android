package com.tangem.domain.walletconnect.model

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.wallets.models.UserWallet

data class WcSessionProposal(
    val dAppMetaData: WcAppMetaData,
    val proposalNetwork: Map<UserWallet, ProposalNetwork>,
    val securityStatus: Any,
) {

    data class ProposalNetwork(
        val wallet: UserWallet,
        val missingRequired: Set<Network>,
        val required: Set<Network>,
        val available: Set<Network>,
        val notAdded: Set<Network>,
    )
}