package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import com.tangem.common.ui.account.AccountTitle
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TopAppBarButton
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.WalletConnectScreenTestTags
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.features.walletconnect.connections.ui.preview.WcConnectionsPreviewData
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList

private const val LOCKED_WALLET_ALPHA = 0.5f

@Composable
internal fun WcConnectionsContent(
    state: WcConnectionsState,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.secondary,
        snackbarHost = {
            TangemSnackbarHost(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                hostState = snackbarHostState,
            )
        },
        topBar = {
            ConnectionsTopBar(
                modifier = Modifier.statusBarsPadding(),
                config = state.topAppBarConfig,
                showEndButton = state.connections.isNotEmpty(),
            )
        },
        floatingActionButton = {
            if (state.connections.isNotEmpty()) {
                NewConnectionButton(
                    onClick = state.onNewConnectionClick,
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        content = { innerPadding ->
            Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                connections = state.connections,
                onNewConnectionClick = state.onNewConnectionClick,
            )
        },
    )
}

@Composable
private fun Content(connections: WcConnections, onNewConnectionClick: () -> Unit, modifier: Modifier = Modifier) {
    when {
        connections is WcConnections.Loading -> return
        !connections.isNotEmpty() -> EmptyConnectionsBlock(
            modifier = modifier,
            onNewConnectionClick = onNewConnectionClick,
        )
        connections is WcConnections.AccountMode -> AccountModeContent(
            modifier = modifier,
            items = connections.items,
        )
        connections is WcConnections.WalletMode -> ConnectionsBlock(
            modifier = modifier,
            connections = connections.connections,
        )
    }
}

@Composable
private fun EmptyConnectionsBlock(onNewConnectionClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.img_wallet_connect_76),
            contentDescription = "Wallet Connect",
            modifier = Modifier
                .size(76.dp)
                .testTag(WalletConnectScreenTestTags.WALLET_CONNECT_IMAGE),
        )
        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing24),
            text = stringResourceSafe(R.string.wc_no_sessions_title),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.h3,
        )
        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
            text = stringResourceSafe(R.string.wc_no_sessions_desc),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body1,
            textAlign = TextAlign.Center,
        )
        NewConnectionButton(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing48),
            onClick = onNewConnectionClick,
        )
    }
}

@Composable
private fun ConnectionsBlock(connections: ImmutableList<WcConnectionsUM>, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                top = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing76 + bottomBarHeight,
            ),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            items(
                items = connections,
                key = { it.userWalletId },
                itemContent = {
                    ConnectionItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = TangemTheme.colors.background.primary,
                                shape = RoundedCornerShape(TangemTheme.dimens.radius14),
                            )
                            .clip(RoundedCornerShape(TangemTheme.dimens.radius14)),
                        connection = it,
                    )
                },
            )
        }
        BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun AccountModeContent(items: ImmutableList<WcConnectionsItem>, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing76 + bottomBarHeight,
            ),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id },
                itemContent = { index, item ->
                    val previousItem = items.getOrNull(index.dec())

                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .getOffsetModifier(item, previousItem)
                    when (item) {
                        is WcConnectionsItem.WalletHeader -> WalletHeader(item, itemModifier)
                        is WcConnectionsItem.PortfolioConnections -> PortfolioItem(
                            modifier = itemModifier
                                .background(
                                    color = TangemTheme.colors.background.primary,
                                    shape = RoundedCornerShape(TangemTheme.dimens.radius14),
                                )
                                .clip(RoundedCornerShape(TangemTheme.dimens.radius14)),
                            connection = item,
                        )
                    }
                },
            )
        }
        BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private fun Modifier.getOffsetModifier(item: WcConnectionsItem, previousItem: WcConnectionsItem?): Modifier = when {
    item is WcConnectionsItem.WalletHeader && previousItem == null -> padding(top = 8.dp)
    item is WcConnectionsItem.WalletHeader && previousItem !is WcConnectionsItem.WalletHeader ->
        padding(top = 16.dp)
    item is WcConnectionsItem.WalletHeader && previousItem is WcConnectionsItem.WalletHeader ->
        padding(top = 8.dp)
    item is WcConnectionsItem.PortfolioConnections && previousItem is WcConnectionsItem.WalletHeader ->
        padding(top = 8.dp)
    item is WcConnectionsItem.PortfolioConnections && previousItem is WcConnectionsItem.PortfolioConnections ->
        padding(top = 8.dp)
    else -> this
}

@Composable
private fun ConnectionItem(connection: WcConnectionsUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        key("${connection.userWalletId}_${connection.walletName}") {
            Text(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp)
                    .testTag(WalletConnectScreenTestTags.WALLET_NAME),
                text = connection.walletName,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
            )
        }
        connection.connectedApps.fastForEach { appInfo ->
            key("${connection.userWalletId}_${connection.walletName}_${appInfo.name}_${appInfo.iconUrl}") {
                AppInfoItem(
                    appInfo = appInfo,
                    modifier = Modifier
                        .clickable(onClick = appInfo.onClick)
                        .padding(all = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun WalletHeader(connection: WcConnectionsItem.WalletHeader, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .conditional(connection.isLocked) { alpha(LOCKED_WALLET_ALPHA) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = connection.walletName,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (connection.isLocked) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors.button.secondary,
                        shape = CircleShape,
                    )
                    .padding(4.dp),
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    tint = TangemTheme.colors.icon.informative,
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_locked_24),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun PortfolioItem(connection: WcConnectionsItem.PortfolioConnections, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        key(connection.id) {
            AccountTitle(
                textStyle = TangemTheme.typography.caption1,
                textColor = TangemTheme.colors.text.primary1,
                accountTitleUM = connection.portfolioTitle,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
            )
        }
        connection.connectedApps.fastForEach { appInfo ->
            key("${connection.id}_${appInfo.name}_${appInfo.iconUrl}") {
                AppInfoItem(
                    appInfo = appInfo,
                    modifier = Modifier
                        .clickable(onClick = appInfo.onClick)
                        .padding(all = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AppInfoItem(appInfo: WcConnectedAppInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius8))
                .testTag(WalletConnectScreenTestTags.APP_ICON),
            model = appInfo.iconUrl,
            error = painterResource(R.drawable.img_wc_dapp_icon_placeholder_48),
            fallback = painterResource(R.drawable.img_wc_dapp_icon_placeholder_48),
            contentDescription = appInfo.name,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag(WalletConnectScreenTestTags.APP_NAME),
                    text = appInfo.name,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (appInfo.verifiedState is VerifiedDAppState.Verified) {
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .testTag(WalletConnectScreenTestTags.APPROVE_ICON),
                        painter = painterResource(R.drawable.img_approvale2_20),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                }
            }
            Text(
                text = appInfo.subtitle,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
                modifier = Modifier.testTag(WalletConnectScreenTestTags.APP_URL),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionsTopBar(
    config: WcConnectionsTopAppBarConfig,
    showEndButton: Boolean,
    modifier: Modifier = Modifier,
) {
    var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
    // TangemTopAppBar is not friendly with dropdown menu
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        navigationIcon = {
            TopAppBarButton(
                button = config.startButtonUM,
                tint = TangemTheme.colors.icon.primary1,
                modifier = Modifier.testTag(WalletConnectScreenTestTags.MORE_BUTTON),
            )
        },
        title = {
            Text(
                text = stringResourceSafe(R.string.wc_connections),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            AnimatedVisibility(showEndButton) {
                IconButton(onClick = { showDropdownMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vertical_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = "More",
                    )
                }
            }

            TangemDropdownMenu(
                expanded = showDropdownMenu,
                modifier = Modifier.background(TangemTheme.colors.background.primary),
                onDismissRequest = { showDropdownMenu = false },
                content = {
                    TangemDropdownItem(
                        item = config.disconnectAllItem,
                        dismissParent = { showDropdownMenu = false },
                    )
                },
            )
        },
    )
}

@Composable
private fun NewConnectionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PrimaryButton(
        modifier = modifier,
        text = stringResourceSafe(R.string.wc_new_connection),
        onClick = onClick,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcConnectionsContentPreview(
    @PreviewParameter(WcConnectionsStateProvider::class) params: WcConnectionsState,
) {
    TangemThemePreview {
        WcConnectionsContent(state = params)
    }
}

private class WcConnectionsStateProvider : CollectionPreviewParameterProvider<WcConnectionsState>(
    listOf(
        WcConnectionsPreviewData.loadingState,
        WcConnectionsPreviewData.stateWithEmptyConnections,
        WcConnectionsPreviewData.fullState,
        WcConnectionsPreviewData.accountState,
    ),
)