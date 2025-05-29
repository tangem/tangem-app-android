package com.tangem.features.walletconnect.connections.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

data class WcConnectedAppInfoUM(
    val appName: String,
    val appIcon: String,
    val isVerified: Boolean,
    val verifiedDAppState: VerifiedDAppState,
    val appSubtitle: String,
    val walletName: String,
    val networks: ImmutableList<WcNetworkInfoItem.Required>,
    val disconnectButtonConfig: WcPrimaryButtonConfig,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent