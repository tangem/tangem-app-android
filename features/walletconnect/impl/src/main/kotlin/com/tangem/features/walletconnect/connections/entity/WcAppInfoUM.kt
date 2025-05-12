package com.tangem.features.walletconnect.connections.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class WcAppInfoUM : TangemBottomSheetConfigContent {

    abstract val onDismiss: () -> Unit
    abstract val connectButtonConfig: WcPrimaryButtonConfig

    data class Loading(
        override val onDismiss: () -> Unit,
        override val connectButtonConfig: WcPrimaryButtonConfig,
    ) : WcAppInfoUM()

    data class Content(
        val appName: String,
        val appIcon: String,
        val isVerified: Boolean,
        val appSubtitle: String,
        val notification: WcAppInfoSecurityNotification?,
        val walletName: String,
        val onWalletClick: () -> Unit,
        val networksInfo: WcNetworksInfo,
        override val connectButtonConfig: WcPrimaryButtonConfig,
        override val onDismiss: () -> Unit,
    ) : WcAppInfoUM()
}

sealed class WcAppInfoSecurityNotification(
    title: TextReference,
    subtitle: TextReference,
) : NotificationUM.Info(title = title, subtitle = subtitle) {
    data object UnknownDomain : WcAppInfoSecurityNotification(
        title = resourceReference(R.string.wc_alert_audit_unknown_domain),
        subtitle = resourceReference(R.string.wc_alert_domain_issues_description),
    )

    data object SecurityRisk : WcAppInfoSecurityNotification(
        title = resourceReference(R.string.wc_notification_security_risk_title),
        subtitle = resourceReference(R.string.wc_notification_security_risk_subtitle),
    )
}

@Immutable
internal sealed class WcNetworksInfo {
    data class MissingRequiredNetworkInfo(val networks: String) : WcNetworksInfo()
    data class ContainsAllRequiredNetworks(val items: ImmutableList<WcNetworkInfoItem>) : WcNetworksInfo()
}