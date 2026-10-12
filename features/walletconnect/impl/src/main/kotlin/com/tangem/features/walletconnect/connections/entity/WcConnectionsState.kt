package com.tangem.features.walletconnect.connections.entity

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import kotlinx.collections.immutable.ImmutableList

internal data class WcConnectionsState(
    val topAppBarConfig: WcConnectionsTopAppBarConfig,
    val connections: WcConnections,
    val onNewConnectionClick: () -> Unit,
)

internal data class WcConnectionsTopAppBarConfig(
    val startButtonUM: TopAppBarButtonUM,
    val disconnectAllItem: TangemDropdownMenuItem,
)

internal sealed interface WcConnections {

    data object Loading : WcConnections

    data object Empty : WcConnections

    /**
     * Wallets the screen can show, and the one it is on.
     *
     * A wallet is listed only when it has agents or dApps of its own, so [wallets] doubles as the tab
     * row: one entry means there is nothing to switch between.
     */
    data class Content(
        val wallets: ImmutableList<WcWalletConnectionsUM>,
        val selectedWalletId: String,
        val onWalletSelect: (String) -> Unit,
    ) : WcConnections {

        val selectedWallet: WcWalletConnectionsUM
            get() = wallets.first { it.userWalletId == selectedWalletId }
    }
}

internal data class WcWalletConnectionsUM(
    val userWalletId: String,
    val walletName: String,
    val agents: ImmutableList<McpAgentInfo>,
    val dAppGroups: ImmutableList<WcDAppGroupUM>,
)

/** dApps of one account — or of the whole wallet when accounts are off, and then [title] is null. */
internal data class WcDAppGroupUM(
    val id: String,
    val title: AccountTitleUM?,
    val connectedApps: ImmutableList<WcConnectedAppInfo>,
)

internal data class WcConnectedAppInfo(
    val name: String,
    val iconUrl: String,
    val verifiedState: VerifiedDAppState,
    val subtitle: String,
    val onClick: () -> Unit,
)

internal data class McpAgentInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val iconUrl: String?,
    val onClick: () -> Unit,
)