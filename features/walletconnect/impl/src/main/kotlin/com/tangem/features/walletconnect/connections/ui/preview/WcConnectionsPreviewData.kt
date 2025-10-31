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
import timber.log.Timber
import java.util.UUID

internal object WcConnectionsPreviewData {
    private const val BASE_URL = "https://raw.githubusercontent.com/TaranVH/LOGOS/refs/heads/master/Crypto"
    private val connections = persistentListOf(
        WcConnectionsUM(
            userWalletId = "wallet_id_1",
            walletName = "Wallet 2.0",
            connectedApps = persistentListOf(
                WcConnectedAppInfo(
                    name = "Moralis",
                    iconUrl = "$BASE_URL/satoshi.png",
                    subtitle = "https://react-app.walletconnect.com/",
                    verifiedState = VerifiedDAppState.Verified {},
                    onClick = { Timber.d("Moralis clicked") },
                ),
                WcConnectedAppInfo(
                    name = "website.com",
                    iconUrl = "$BASE_URL/doge_again_lol.jpg",
                    subtitle = "https://react-app.walletconnect.com/",
                    onClick = { Timber.d("website.com clicked") },
                    verifiedState = VerifiedDAppState.Unknown,
                ),
            ),
        ),
        WcConnectionsUM(
            userWalletId = "wallet_id_2",
            walletName = "Bitcoin",
            connectedApps = persistentListOf(
                WcConnectedAppInfo(
                    name = "React app React app React app React app React app",
                    iconUrl = "$BASE_URL/LiteCoin.png",
                    subtitle = "https://react-app.walletconnect.com/",
                    verifiedState = VerifiedDAppState.Verified {},
                    onClick = { Timber.d("React app clicked") },
                ),
            ),
        ),
        WcConnectionsUM(
            userWalletId = "wallet_id_3",
            walletName = "Wallet 2.0",
            connectedApps = persistentListOf(
                WcConnectedAppInfo(
                    name = "Moralis",
                    iconUrl = "$BASE_URL/satoshi.png",
                    subtitle = "https://react-app.walletconnect.com/",
                    verifiedState = VerifiedDAppState.Verified {},
                    onClick = { Timber.d("Moralis clicked") },
                ),
                WcConnectedAppInfo(
                    name = "website.com",
                    iconUrl = "$BASE_URL/doge_again_lol.jpg",
                    subtitle = "https://react-app.walletconnect.com/",
                    verifiedState = VerifiedDAppState.Unknown,
                    onClick = { Timber.d("website.com clicked") },
                ),
            ),
        ),
        WcConnectionsUM(
            userWalletId = "wallet_id_4",
            walletName = "Bitcoin",
            connectedApps = persistentListOf(
                WcConnectedAppInfo(
                    name = "React app",
                    iconUrl = "$BASE_URL/LiteCoin.png",
                    subtitle = "https://react-app.walletconnect.com/",
                    onClick = { Timber.d("React app clicked") },
                    verifiedState = VerifiedDAppState.Verified {},
                ),
            ),
        ),
        WcConnectionsUM(
            userWalletId = "wallet_id_5",
            walletName = "Wallet 2.0",
            connectedApps = persistentListOf(
                WcConnectedAppInfo(
                    name = "Moralis",
                    iconUrl = "$BASE_URL/satoshi.png",
                    subtitle = "https://react-app.walletconnect.com/",
                    verifiedState = VerifiedDAppState.Verified {},
                    onClick = { Timber.d("Moralis clicked") },
                ),
                WcConnectedAppInfo(
                    name = "website.com",
                    iconUrl = "$BASE_URL/doge_again_lol.jpg",
                    subtitle = "https://react-app.walletconnect.com/",
                    onClick = { Timber.d("website.com clicked") },
                    verifiedState = VerifiedDAppState.Unknown,
                ),
            ),
        ),
        WcConnectionsUM(
            userWalletId = "wallet_id_6",
            walletName = "Bitcoin",
            connectedApps = persistentListOf(
                WcConnectedAppInfo(
                    name = "React app",
                    iconUrl = "$BASE_URL/LiteCoin.png",
                    subtitle = "https://react-app.walletconnect.com/",
                    verifiedState = VerifiedDAppState.Verified {},
                    onClick = { Timber.d("React app clicked") },
                ),
            ),
        ),
    )
    val stateWithEmptyConnections = WcConnectionsState(
        topAppBarConfig = WcConnectionsTopAppBarConfig(
            startButtonUM = TopAppBarButtonUM.Back(
                onBackClicked = {},
                enabled = true,
            ),
            disconnectAllItem = TangemDropdownMenuItem(
                title = resourceReference(R.string.wc_disconnect_all),
                textColor = themedColor { TangemTheme.colors.text.warning },
                onClick = { },
            ),
        ),
        connections = WcConnections.WalletMode(persistentListOf()),
        onNewConnectionClick = {},
    )

    val connectedDapp
        get() = WcConnectedAppInfo(
            name = "React app",
            iconUrl = "$BASE_URL/LiteCoin.png",
            subtitle = "https://react-app.walletconnect.com/",
            verifiedState = VerifiedDAppState.Verified {},
            onClick = { Timber.d("React app clicked") },
        )

    val walletHeader
        get() = WcConnectionsItem.WalletHeader(
            id = UUID.randomUUID().toString(),
            walletName = "User Wallet",
            isLocked = false,
        )

    val lockedWallet
        get() = WcConnectionsItem.WalletHeader(
            id = UUID.randomUUID().toString(),
            walletName = "Locked Wallet",
            isLocked = true,
        )

    val accountStateItems
        get() = persistentListOf(
            walletHeader,
            WcConnectionsItem.PortfolioConnections(
                id = UUID.randomUUID().toString(),
                portfolioTitle = accountPortfolioName,
                connectedApps = persistentListOf(connectedDapp),
            ),
            walletHeader,
            WcConnectionsItem.PortfolioConnections(
                id = UUID.randomUUID().toString(),
                portfolioTitle = accountPortfolioName,
                connectedApps = persistentListOf(connectedDapp, connectedDapp),
            ),
            WcConnectionsItem.PortfolioConnections(
                id = UUID.randomUUID().toString(),
                portfolioTitle = accountPortfolioName,
                connectedApps = persistentListOf(connectedDapp),
            ),
            WcConnectionsItem.PortfolioConnections(
                id = UUID.randomUUID().toString(),
                portfolioTitle = accountPortfolioName,
                connectedApps = persistentListOf(connectedDapp),
            ),
            lockedWallet,
            lockedWallet,
        )

    val fullState = stateWithEmptyConnections.copy(connections = WcConnections.WalletMode(connections))
    val accountState = stateWithEmptyConnections.copy(connections = WcConnections.AccountMode(accountStateItems))
    val loadingState = stateWithEmptyConnections.copy(connections = WcConnections.Loading)
}