package com.tangem.features.walletconnect.connections.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class WcAppInfoUM(
    val appName: String,
    val appIcon: String,
    val isVerified: Boolean,
    val appSubtitle: String,
    val notificationUM: NotificationUM?,
    val walletName: String,
    val networksInfo: WcNetworksInfo,
    val onDismiss: () -> Unit,
    val onConnect: () -> Unit,
) : TangemBottomSheetConfigContent

@Immutable
internal sealed class WcNetworksInfo {
    data class MissingRequiredNetworkInfo(val network: String) : WcNetworksInfo()
    data class ContainsAllRequiredNetworks(val icons: ImmutableList<Int>) : WcNetworksInfo()
}