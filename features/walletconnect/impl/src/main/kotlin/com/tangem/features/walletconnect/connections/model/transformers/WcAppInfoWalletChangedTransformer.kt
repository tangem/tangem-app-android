package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.utils.transformer.Transformer

internal class WcAppInfoWalletChangedTransformer(
    private val selectedUserWallet: UserWallet,
    private val proposalNetwork: WcSessionProposal.ProposalNetwork,
) : Transformer<WcAppInfoUM> {
    override fun transform(prevState: WcAppInfoUM): WcAppInfoUM {
        val contentState = prevState as? WcAppInfoUM.Content ?: return prevState
        return contentState.copy(
            walletName = selectedUserWallet.name,
            networksInfo = WcNetworksInfoConverter.convert(proposalNetwork),
            connectButtonConfig = prevState.connectButtonConfig.copy(
                enabled = proposalNetwork.missingRequired.isEmpty(),
            ),
        )
    }
}