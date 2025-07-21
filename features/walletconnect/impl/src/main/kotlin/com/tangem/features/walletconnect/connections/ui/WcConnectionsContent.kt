package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.components.snackbar.TangemSnackbarHost
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.features.walletconnect.connections.entity.WcConnectedAppInfo
import com.tangem.features.walletconnect.connections.entity.WcConnectionsState
import com.tangem.features.walletconnect.connections.entity.WcConnectionsTopAppBarConfig
import com.tangem.features.walletconnect.connections.entity.WcConnectionsUM
import com.tangem.features.walletconnect.connections.ui.preview.WcConnectionsPreviewData
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList

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
private fun Content(
    connections: ImmutableList<WcConnectionsUM>,
    onNewConnectionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (connections.isEmpty()) {
        EmptyConnectionsBlock(modifier = modifier, onNewConnectionClick = onNewConnectionClick)
    } else {
        ConnectionsBlock(modifier = modifier, connections = connections)
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
            modifier = Modifier.size(76.dp),
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
private fun ConnectionItem(connection: WcConnectionsUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        key("${connection.userWalletId}_${connection.walletName}") {
            Text(
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
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
private fun AppInfoItem(appInfo: WcConnectedAppInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        AsyncImage(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius8)),
            model = appInfo.iconUrl,
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
                    text = appInfo.name,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle2,
                )
                if (appInfo.verifiedState is VerifiedDAppState.Verified) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.img_approvale2_20),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                }
            }
            Text(
                text = appInfo.subtitle.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
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
            IconButton(onClick = config.startButtonUM.onIconClicked) {
                Icon(
                    painter = painterResource(id = config.startButtonUM.iconRes),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = "Back",
                )
            }
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
        WcConnectionsPreviewData.stateWithEmptyConnections,
        WcConnectionsPreviewData.fullState,
    ),
)