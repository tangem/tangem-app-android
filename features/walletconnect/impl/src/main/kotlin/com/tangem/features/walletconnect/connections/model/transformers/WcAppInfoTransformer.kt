package com.tangem.features.walletconnect.connections.model.transformers

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class WcAppInfoTransformer(
    private val dAppSession: WcSessionProposal,
    private val onDismiss: () -> Unit,
    private val onConnect: () -> Unit,
    private val userWallet: UserWallet,
    private val proposalNetwork: WcSessionProposal.ProposalNetwork,
) : Transformer<WcAppInfoUM> {

    override fun transform(prevState: WcAppInfoUM): WcAppInfoUM {
        return WcAppInfoUM.Content(
            appName = dAppSession.dAppMetaData.name,
            appIcon = dAppSession.dAppMetaData.icons.firstOrNull().orEmpty(),
            isVerified = dAppSession.securityStatus == CheckDAppResult.SAFE,
            appSubtitle = dAppSession.dAppMetaData.description,
            notification = createNotification(dAppSession.securityStatus),
            walletName = userWallet.name,
            networksInfo = convertNetworksInfo(proposalNetwork),
            connectButtonConfig = WcPrimaryButtonConfig(
                showProgress = false,
                enabled = proposalNetwork.missingRequired.isEmpty(),
                onClick = onConnect,
            ),
            onDismiss = onDismiss,
        )
    }

    private fun createNotification(securityStatus: CheckDAppResult): WcAppInfoSecurityNotification? {
        return when (securityStatus) {
            CheckDAppResult.SAFE -> null
            CheckDAppResult.UNSAFE -> WcAppInfoSecurityNotification.SecurityRisk
            CheckDAppResult.FAILED_TO_VERIFY -> WcAppInfoSecurityNotification.UnknownDomain
        }
    }

    private fun convertNetworksInfo(proposalNetwork: WcSessionProposal.ProposalNetwork): WcNetworksInfo {
        return if (proposalNetwork.missingRequired.isNotEmpty()) {
            WcNetworksInfo.MissingRequiredNetworkInfo(
                networks = proposalNetwork.missingRequired
                    .joinToString { it.name },
            )
        } else {
            WcNetworksInfo.ContainsAllRequiredNetworks(
                items = (proposalNetwork.required + proposalNetwork.available)
                    .map {
                        WcNetworkInfoItem(
                            id = it.id.value,
                            icon = it.iconResId,
                            name = it.name,
                            symbol = it.currencySymbol,
                        )
                    }
                    .toImmutableList(),
            )
        }
    }
}