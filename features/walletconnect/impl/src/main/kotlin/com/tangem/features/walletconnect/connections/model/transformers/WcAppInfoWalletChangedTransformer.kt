package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.utils.transformer.Transformer

internal class WcAppInfoWalletChangedTransformer(
    private val selectedUserWallet: UserWallet,
    private val proposalNetwork: WcSessionProposal.ProposalNetwork,
    private val additionallyEnabledNetworks: Set<Network>,
) : Transformer<WcAppInfoUM> {
    override fun transform(prevState: WcAppInfoUM): WcAppInfoUM {
        val contentState = prevState as? WcAppInfoUM.Content ?: return prevState
        return contentState.copy(
            walletName = selectedUserWallet.name,
            networksInfo = WcNetworksInfoConverter.convert(
                WcNetworksInfoConverter.Input(
                    missingNetworks = proposalNetwork.missingRequired,
                    requiredNetworks = proposalNetwork.required,
                    availableNetworks = proposalNetwork.available,
                    notAddedNetworks = proposalNetwork.notAdded,
                    additionallyEnabledNetworks = additionallyEnabledNetworks,
                ),
            ),
            connectButtonConfig = prevState.connectButtonConfig.copy(
                enabled = WcConnectButtonAvailabilityConverter.convert(
                    WcConnectButtonAvailabilityConverter.Input(
                        missingNetworks = proposalNetwork.missingRequired,
                        requiredNetworks = proposalNetwork.required,
                        availableNetworks = proposalNetwork.available,
                        selectedNetworks = additionallyEnabledNetworks,
                    ),
                ),
            ),
        )
    }
}