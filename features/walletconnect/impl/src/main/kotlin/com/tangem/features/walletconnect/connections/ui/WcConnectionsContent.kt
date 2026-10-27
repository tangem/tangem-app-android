package com.tangem.features.walletconnect.connections.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.common.ui.account.AccountTitle
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.GroupEntry
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_dots_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_28
import com.tangem.core.ui.test.WalletConnectScreenTestTags
import com.tangem.features.walletconnect.connections.entity.*
import com.tangem.features.walletconnect.connections.ui.preview.WcConnectionsPreviewData
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.toImmutableList

private val CardShape = RoundedCornerShape(24.dp)
private const val AGENTS_SKELETON_ITEMS = 3

@Composable
internal fun WcConnectionsContent(state: WcConnectionsState, modifier: Modifier = Modifier) {
    val connections = state.connections
    // The footer floats over the list, which has to end above it.
    var footerHeight by remember { mutableIntStateOf(0) }
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            ConnectionsTopBar(
                config = state.topAppBarConfig,
                showEndButton = connections !is WcConnections.Empty,
            )
        },
        overlay = {
            NewConnectionFooter(
                onClick = state.onNewConnectionClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
            )
        },
        content = { contentPadding ->
            when (connections) {
                is WcConnections.Loading -> ConnectionsSkeleton(modifier = Modifier.padding(contentPadding))
                is WcConnections.Empty -> EmptyConnections(modifier = Modifier.padding(contentPadding))
                is WcConnections.Content -> Connections(
                    connections = connections,
                    contentPadding = contentPadding,
                    footerHeight = with(LocalDensity.current) { footerHeight.toDp() },
                )
            }
        },
    )
}

@Composable
private fun Connections(connections: WcConnections.Content, contentPadding: PaddingValues, footerHeight: Dp) {
    Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
        // One wallet has nothing to switch between, so the row would only take up room.
        if (connections.wallets.size > 1) {
            WalletTabs(connections = connections)
        }
        ConnectionsList(wallet = connections.selectedWallet, bottomPadding = footerHeight)
    }
}

@Composable
private fun WalletTabs(connections: WcConnections.Content) {
    val walletIcon = TangemIconUM.Icon(ImageVector.vectorResource(R.drawable.ic_key_card_20))
    val tabs = remember(connections, walletIcon) {
        connections.wallets.map { wallet ->
            TangemTabItemUM.Content(
                id = wallet.userWalletId,
                label = stringReference(wallet.walletName),
                iconStart = walletIcon,
                isSelected = wallet.userWalletId == connections.selectedWalletId,
                onClick = { connections.onWalletSelect(wallet.userWalletId) },
            )
        }.toImmutableList()
    }

    TangemTabNavigation(tabs = tabs, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ConnectionsList(wallet: WcWalletConnectionsUM, bottomPadding: Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        agentsSection(agents = wallet.agents)
        if (wallet.dAppGroups.isNotEmpty()) {
            item(key = "dapps_title") { SectionTitle(stringResourceSafe(R.string.wc_connections_dapps_title)) }
            items(items = wallet.dAppGroups, key = { it.id }) { group ->
                Card { DAppGroup(group = group) }
            }
        }
    }
}

private fun LazyListScope.agentsSection(agents: WcAgentsUM) {
    if (agents is WcAgentsUM.Content && agents.agents.isEmpty()) return

    item(key = "agents_title") { SectionTitle(stringResourceSafe(R.string.wc_connections_agents_title)) }

    when (agents) {
        is WcAgentsUM.Loading -> item(key = "agents_skeleton") { Card { AgentsSkeleton() } }
        is WcAgentsUM.Error -> item(key = "agents_error") { Card { AgentsErrorItem(state = agents) } }
        is WcAgentsUM.Content -> items(items = agents.agents, key = { it.id }) { agent ->
            Card { AgentItem(agent = agent) }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
        text = text,
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
    )
}

@Composable
private fun AgentItem(agent: McpAgentInfo) {
    TangemRow(
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = agent.onClick,
        startSlot = { McpAgentAvatar(iconUrl = agent.iconUrl) },
        titleSlot = { TangemRowText(text = agent.name, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = agent.subtitle, role = TangemRowTextRole.Subtitle) },
    )
}

@Composable
private fun DAppGroup(group: WcDAppGroupUM) {
    group.title?.let { title ->
        AccountTitle(
            accountTitleUM = title,
            textStyle = TangemTheme.typography3.caption.medium,
            textColor = TangemTheme.colors3.text.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
    }
    group.connectedApps.forEach { appInfo ->
        key(group.id, appInfo.name, appInfo.iconUrl) {
            AppInfoItem(appInfo = appInfo)
        }
    }
}

@Composable
private fun AppInfoItem(appInfo: WcConnectedAppInfo) {
    TangemRow(
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = appInfo.onClick,
        startSlot = {
            AsyncImage(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag(WalletConnectScreenTestTags.APP_ICON),
                model = appInfo.iconUrl,
                error = painterResource(R.drawable.img_wc_dapp_icon_placeholder_48),
                fallback = painterResource(R.drawable.img_wc_dapp_icon_placeholder_48),
                contentDescription = appInfo.name,
            )
        },
        titleSlot = {
            TangemRowText(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag(WalletConnectScreenTestTags.APP_NAME),
                text = appInfo.name,
                role = TangemRowTextRole.Title,
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
        },
        subtitleSlot = {
            TangemRowText(
                modifier = Modifier.testTag(WalletConnectScreenTestTags.APP_URL),
                text = appInfo.subtitle,
                role = TangemRowTextRole.Subtitle,
            )
        },
    )
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.secondary,
        shape = CardShape,
    ) {
        Column(content = content)
    }
}

/**
 * Stands in for the whole list while the first read is running: the screen has no wallet to put in the
 * tab row yet, so a single section is all it can promise.
 */
@Composable
private fun ConnectionsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemShimmer(
            style = TangemTheme.typography3.caption.medium,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
        )
        Card { AgentsSkeleton() }
    }
}

@Composable
private fun AgentsSkeleton() {
    repeat(times = AGENTS_SKELETON_ITEMS) { index ->
        key(index) {
            TangemRow(
                contentLead = TangemRowContentLead.Start,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                startSlot = {
                    TangemAvatar(state = TangemAvatar.UiState.Shimmer(), size = TangemAvatar.Size.X40)
                },
                titleSlot = {
                    TangemShimmer(style = TangemTheme.typography3.body.medium, modifier = Modifier.weight(1f))
                },
                subtitleSlot = {
                    TangemShimmer(style = TangemTheme.typography3.caption.medium, modifier = Modifier.weight(1f))
                },
            )
        }
    }
}

/**
 * The row reads bottom-up — "AI agents unavailable" is the message and "Tap to reload" the call to
 * action above it — so the caption takes the row's title slot and the message its subtitle slot.
 */
@Composable
private fun AgentsErrorItem(state: WcAgentsUM.Error) {
    TangemRow(
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = state.onReloadClick,
        startSlot = {
            TangemAvatar(
                state = TangemAvatar.UiState.Icon(
                    imageVector = Icons.ic_error_20,
                    appearance = TangemAvatar.Appearance.Error,
                ),
                size = TangemAvatar.Size.X40,
            )
        },
        titleSlot = {
            TangemRowText(
                modifier = Modifier.weight(1f),
                text = stringResourceSafe(R.string.feed_promo_load_error_action),
                role = TangemRowTextRole.Subtitle,
            )
        },
        subtitleSlot = {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResourceSafe(R.string.wc_connections_agents_error_title),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        endSlot = {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                isLoading = state.isReloading,
                iconStart = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
                contentDescription = stringResourceSafe(R.string.feed_promo_load_error_action),
                onClick = state.onReloadClick,
            )
        },
    )
}

@Composable
private fun EmptyConnections(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemAvatar(
            state = TangemAvatar.UiState.Icon(
                imageVector = Icons.ic_grid_plus_28,
                appearance = TangemAvatar.Appearance.Info,
            ),
            size = TangemAvatar.Size.X72,
            modifier = Modifier.testTag(WalletConnectScreenTestTags.WALLET_CONNECT_IMAGE),
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResourceSafe(R.string.wc_connections_empty_title),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.heading.small,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            text = stringResourceSafe(R.string.wc_connections_empty_desc),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NewConnectionFooter(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemFooter(modifier = modifier) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.wc_new_connection),
            onClick = onClick,
        )
    }
}

@Composable
private fun ConnectionsTopBar(config: WcConnectionsTopAppBarConfig, showEndButton: Boolean) {
    var shouldShowDropdownMenu by rememberSaveable { mutableStateOf(false) }

    TangemTopNavigation(
        title = resourceReference(R.string.wc_connections),
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        onBack = config.startButtonUM.onClicked,
        endButtonsGroup = {
            if (!showEndButton) return@TangemTopNavigation
            Box {
                TangemButton.GroupEntry(
                    imageVector = Icons.ic_dots_horizontal_20,
                    modifier = Modifier.testTag(WalletConnectScreenTestTags.MORE_BUTTON),
                    onClick = { shouldShowDropdownMenu = true },
                )
                TangemDropdownMenu(
                    expanded = shouldShowDropdownMenu,
                    modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
                    onDismissRequest = { shouldShowDropdownMenu = false },
                    content = {
                        TangemDropdownItem(
                            item = config.disconnectAllItem,
                            dismissParent = { shouldShowDropdownMenu = false },
                        )
                    },
                )
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WcConnectionsContentPreview(
    @PreviewParameter(WcConnectionsStateProvider::class) params: WcConnectionsState,
) {
    TangemThemePreviewRedesign {
        WcConnectionsContent(state = params)
    }
}

private class WcConnectionsStateProvider : CollectionPreviewParameterProvider<WcConnectionsState>(
    listOf(
        WcConnectionsPreviewData.loadingState,
        WcConnectionsPreviewData.emptyState,
        WcConnectionsPreviewData.singleWalletState,
        WcConnectionsPreviewData.multiWalletState,
        WcConnectionsPreviewData.loadingAgentsState,
        WcConnectionsPreviewData.failedAgentsState,
        WcConnectionsPreviewData.reloadingAgentsState,
    ),
)