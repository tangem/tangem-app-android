package com.tangem.features.walletconnect.connections.model.transformers

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.walletconnect.connections.entity.WcAppInfoSecurityNotification
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.utils.transformer.Transformer

@Suppress("LongParameterList")
internal class WcAppInfoTransformer(
    private val dAppSession: WcSessionProposal,
    private val dAppVerifiedStateConverter: WcDAppVerifiedStateConverter,
    private val onDismiss: () -> Unit,
    private val onConnect: (securityStatus: CheckDAppResult) -> Unit,
    private val onWalletClick: () -> Unit,
    private val onNetworksClick: () -> Unit,
    private val userWallet: UserWallet,
    private val proposalNetwork: WcSessionProposal.ProposalNetwork,
) : Transformer<WcAppInfoUM> {

    override fun transform(prevState: WcAppInfoUM): WcAppInfoUM {
        return WcAppInfoUM.Content(
            appName = dAppSession.dAppMetaData.name,
            appIcon = dAppSession.dAppMetaData.icons.firstOrNull().orEmpty(),
            verifiedDAppState = dAppVerifiedStateConverter.convert(
                dAppSession.securityStatus to dAppSession.dAppMetaData.name,
            ),
            appSubtitle = dAppSession.dAppMetaData.description,
            notification = createNotification(dAppSession.securityStatus),
            walletName = userWallet.name,
            onWalletClick = onWalletClick,
            networksInfo = WcNetworksInfoConverter.convert(proposalNetwork),
            onNetworksClick = onNetworksClick,
            connectButtonConfig = WcPrimaryButtonConfig(
                showProgress = false,
                enabled = proposalNetwork.missingRequired.isEmpty(),
                onClick = { onConnect(dAppSession.securityStatus) },
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
}