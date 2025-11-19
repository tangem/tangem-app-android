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
    data class WalletMode(
        val connections: ImmutableList<WcConnectionsUM>,
    ) : WcConnections

    data class AccountMode(
        val items: ImmutableList<WcConnectionsItem>,
    ) : WcConnections

    fun isNotEmpty(): Boolean = when (this) {
        is AccountMode -> items.isNotEmpty()
        is WalletMode -> connections.isNotEmpty()
        Loading -> false
    }
}

internal sealed interface WcConnectionsItem {
    val id: String

    data class WalletHeader(
        override val id: String,
        val walletName: String,
        val isLocked: Boolean,
    ) : WcConnectionsItem

    data class PortfolioConnections(
        override val id: String,
        val portfolioTitle: AccountTitleUM,
        val connectedApps: ImmutableList<WcConnectedAppInfo>,
    ) : WcConnectionsItem
}

internal data class WcConnectionsUM(
    val userWalletId: String,
    val walletName: String,
    val connectedApps: ImmutableList<WcConnectedAppInfo>,
)

internal data class WcConnectedAppInfo(
    val name: String,
    val iconUrl: String,
    val verifiedState: VerifiedDAppState,
    val subtitle: String,
    val onClick: () -> Unit,
)