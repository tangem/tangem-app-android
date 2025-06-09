package com.tangem.features.walletconnect.connections.entity

import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import kotlinx.collections.immutable.ImmutableList

internal data class WcConnectionsState(
    val topAppBarConfig: WcConnectionsTopAppBarConfig,
    val connections: ImmutableList<WcConnectionsUM>,
    val onNewConnectionClick: () -> Unit,
)

internal data class WcConnectionsTopAppBarConfig(
    val startButtonUM: TopAppBarButtonUM,
    val disconnectAllItem: TangemDropdownMenuItem,
)

internal data class WcConnectionsUM(
    val userWalletId: String,
    val walletName: String,
    val connectedApps: ImmutableList<WcConnectedAppInfo>,
)

internal data class WcConnectedAppInfo(
    val name: String,
    val iconUrl: String,
    val verifiedState: VerifiedDAppState,
    val subtitle: TextReference = stringReference("Connected App"),
    val onClick: () -> Unit,
)