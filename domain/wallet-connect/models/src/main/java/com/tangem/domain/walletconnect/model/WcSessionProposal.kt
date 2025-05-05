package com.tangem.domain.walletconnect.model

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.wallets.models.UserWallet

data class WcSessionProposal(
    val dAppMetaData: WcAppMetaData,
    val proposalNetwork: Map<UserWallet, ProposalNetwork>,
    val securityStatus: CheckDAppResult,
) {

    data class ProposalNetwork(
        val wallet: UserWallet,
        val missingRequired: Set<Network>,
        val required: Set<Network>,
        val available: Set<Network>,
        val notAdded: Set<Network>,
    )
}