package com.tangem.domain.walletconnect.model

import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData
import com.tangem.domain.wallets.models.UserWalletId

data class WcSessionProposal(
    val dAppMetaData: WcAppMetaData,
    val proposalNetwork: Map<UserWalletId, ProposalNetwork>,
    val securityStatus: Any,
) {

    data class ProposalNetwork(
        val walletId: UserWalletId,
        val missingRequired: Set<WcNetwork.Supported>,
        val required: Set<WcNetwork.Supported>,
        val available: Set<WcNetwork.Supported>,
        val notAdded: Set<WcNetwork.Supported>,
    )
}