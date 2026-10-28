package com.tangem.features.walletconnect.connections.entity

import androidx.compose.runtime.Immutable
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
    val agents: WcAgentsUM,
    val dAppGroups: ImmutableList<WcDAppGroupUM>,
)

/**
 * State of the Connected Agents section.
 *
 * The agent list is read for every wallet at once, so [Loading] and [Error] are the same for all of
 * them — only [Content] is the wallet's own.
 */
@Immutable
internal sealed interface WcAgentsUM {

    data object Loading : WcAgentsUM

    /** The list could not be read. [isReloading] is the owner's retry, still running. */
    data class Error(val isReloading: Boolean, val onReloadClick: () -> Unit) : WcAgentsUM

    /** An empty list hides the section: the wallet has no agents, which is not worth a heading. */
    data class Content(val agents: ImmutableList<McpAgentInfo>) : WcAgentsUM
}

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