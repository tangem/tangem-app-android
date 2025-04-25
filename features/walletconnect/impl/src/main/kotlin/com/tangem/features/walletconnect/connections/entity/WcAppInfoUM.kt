package com.tangem.features.walletconnect.connections.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
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
        val notificationUM: NotificationUM?,
        val walletName: String,
        val networksInfo: WcNetworksInfo,
        override val connectButtonConfig: WcPrimaryButtonConfig,
        override val onDismiss: () -> Unit,
    ) : WcAppInfoUM()
}

@Immutable
internal sealed class WcNetworksInfo {
    data class MissingRequiredNetworkInfo(val networks: String) : WcNetworksInfo()
    data class ContainsAllRequiredNetworks(val icons: ImmutableList<Int>) : WcNetworksInfo()
}
