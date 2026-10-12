package com.tangem.features.walletconnect.connections.ui.preview

import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.ui.sign.accountPortfolioName
import kotlinx.collections.immutable.persistentListOf

internal object WcConnectionsPreviewData {

    private const val BASE_URL = "https://raw.githubusercontent.com/TaranVH/LOGOS/refs/heads/master/Crypto"

    private val agents = persistentListOf(
        McpAgentInfo(
            id = "agent_1",
            name = "Claude Agent",
            subtitle = "mcp.claude.com",
            iconUrl = null,
            onClick = {},
        ),
        McpAgentInfo(
            id = "agent_2",
            name = "Jira agent",
            subtitle = "mcp.atlassian.com",
            iconUrl = null,
            onClick = {},
        ),
    )

    private val dApps = persistentListOf(
        WcConnectedAppInfo(
            name = "React App",
            iconUrl = "$BASE_URL/satoshi.png",
            subtitle = "react-app.walletconnect.com",
            verifiedState = VerifiedDAppState.Verified {},
            onClick = {},
        ),
        WcConnectedAppInfo(
            name = "website",
            iconUrl = "$BASE_URL/doge_again_lol.jpg",
            subtitle = "wc.website.com",
            verifiedState = VerifiedDAppState.Unknown,
            onClick = {},
        ),
    )

    private val mainWallet = WcWalletConnectionsUM(
        userWalletId = "wallet_id_1",
        walletName = "New Wallet",
        agents = agents,
        dAppGroups = persistentListOf(
            WcDAppGroupUM(id = "group_1", title = accountPortfolioName, connectedApps = dApps),
            WcDAppGroupUM(
                id = "group_2",
                title = accountPortfolioName,
                connectedApps = persistentListOf(dApps.first()),
            ),
        ),
    )

    private val secondWallet = WcWalletConnectionsUM(
        userWalletId = "wallet_id_2",
        walletName = "My Wallet",
        agents = persistentListOf(),
        dAppGroups = persistentListOf(
            WcDAppGroupUM(id = "group_3", title = null, connectedApps = persistentListOf(dApps.last())),
        ),
    )

    val emptyState = WcConnectionsState(
        topAppBarConfig = WcConnectionsTopAppBarConfig(
            startButtonUM = TopAppBarButtonUM.Back(onBackClicked = {}),
            disconnectAllItem = TangemDropdownMenuItem(
                title = resourceReference(R.string.wc_disconnect_all),
                textColor = themedColor { TangemTheme.colors.text.warning },
                onClick = {},
            ),
        ),
        connections = WcConnections.Empty,
        onNewConnectionClick = {},
    )

    val singleWalletState = emptyState.copy(
        connections = WcConnections.Content(
            wallets = persistentListOf(mainWallet),
            selectedWalletId = mainWallet.userWalletId,
            onWalletSelect = {},
        ),
    )

    val multiWalletState = emptyState.copy(
        connections = WcConnections.Content(
            wallets = persistentListOf(mainWallet, secondWallet),
            selectedWalletId = mainWallet.userWalletId,
            onWalletSelect = {},
        ),
    )
}