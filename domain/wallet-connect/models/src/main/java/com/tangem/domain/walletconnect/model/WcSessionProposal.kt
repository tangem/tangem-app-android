package com.tangem.domain.walletconnect.model

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.model.sdkcopy.WcAppMetaData

data class WcSessionProposal(
    val dAppMetaData: WcAppMetaData,
    val proposalNetwork: Map<UserWallet, ProposalNetwork>,
    val proposalAccountNetwork: Map<AccountId, ProposalNetwork>?,
    val securityStatus: CheckDAppResult,
) {

    data class ProposalNetwork(
        val wallet: UserWallet,
        val account: Account?,
        val missingRequired: Set<Network>,
        val required: Set<Network>,
        val available: Set<Network>,
        val notAdded: Set<Network>,
    )
}