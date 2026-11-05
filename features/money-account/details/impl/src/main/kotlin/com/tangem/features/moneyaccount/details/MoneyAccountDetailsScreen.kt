package com.tangem.features.moneyaccount.details

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshSlidingContainer
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.core.ui.test.MoneyAccountTestTags
import com.tangem.features.moneyaccount.common.ui.TangemBalanceHeader
import com.tangem.features.moneyaccount.common.ui.TangemBalanceHeaderState
import com.tangem.features.moneyaccount.common.ui.TangemCircleActionButton
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.features.tangempay.common.TangemPayEmptyTransactionHistoryState
import com.tangem.features.tangempay.common.tangemPayTxHistoryNonContentItem
import com.tangem.features.txhistory.component.TxHistoryComponent
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val InitialTopBarHeight: Dp = 64.dp
private const val UNAVAILABLE_BALANCE_ALPHA = 0.4f

@Composable
internal fun MoneyAccountDetailsScreen(
    state: MoneyAccountDetailsUM,
    txHistoryComponent: TxHistoryComponent?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val txHistory = txHistoryComponent?.let { collectTxHistory(it) }
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.systemBars.getTop(this).toDp() }
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    var topBarTotalHeight by remember { mutableStateOf(InitialTopBarHeight + statusBarHeight) }
    val rootBackground = TangemTheme.colors3.bg.primary
    val buttonsHazeState = rememberHazeState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(rootBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(rootBackground)
                .hazeSourceTangem(state = buttonsHazeState, zIndex = -1f),
        )
        TangemPullToRefreshSlidingContainer(
            config = state.pullToRefreshConfig,
            indicatorOffset = topBarTotalHeight,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = listState,
                contentPadding = PaddingValues(
                    top = topBarTotalHeight,
                    bottom = 16.dp + bottomBarHeight,
                ),
            ) {
                body(
                    state = state,
                    listState = listState,
                    txHistory = txHistory,
                    buttonsHazeState = buttonsHazeState,
                )
            }
        }
        TopBar(
            state = state,
            onHeightChange = { measuredHeight ->
                if (topBarTotalHeight != measuredHeight) topBarTotalHeight = measuredHeight
            },
        )
    }
}

private fun LazyListScope.body(
    state: MoneyAccountDetailsUM,
    listState: LazyListState,
    txHistory: TxHistory?,
    buttonsHazeState: HazeState,
) {
    val balanceAlpha = if (state.isBalanceUnavailable) UNAVAILABLE_BALANCE_ALPHA else 1f

    item("balanceBlock") {
        BalanceBlock(
            state = state.balance,
            isBalanceHidden = state.isBalanceHidden,
            modifier = Modifier
                .alpha(balanceAlpha)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp),
        )
    }
    item("actionButtonsBlock") {
        SpacerH24()
        CompositionLocalProvider(LocalHazeState provides buttonsHazeState) {
            ActionBlock(state = state, modifier = Modifier.alpha(balanceAlpha))
        }
    }
    if (state.messageBanner != null) {
        item("messageBanner") {
            SpacerH24()
            TangemMessageBanner(
                state = state.messageBanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
    }
    if (txHistory != null) {
        item("transactionsSpacer") {
            SpacerH24()
        }
        when (txHistory.state) {
            is TxHistoryItemsUM.Empty -> tangemPayTxHistoryNonContentItem(
                listState = listState,
                state = TangemPayEmptyTransactionHistoryState.Empty,
            )
            is TxHistoryItemsUM.Error -> tangemPayTxHistoryNonContentItem(
                listState = listState,
                state = TangemPayEmptyTransactionHistoryState.FailedToLoad(onReload = state.onTxHistoryReloadClick),
            )
            else -> with(txHistory.component) {
                txHistoryContent(listState = listState, state = txHistory.state)
            }
        }
    }
}

private class TxHistory(val component: TxHistoryComponent, val state: TxHistoryItemsUM)

@Composable
private fun collectTxHistory(component: TxHistoryComponent): TxHistory {
    val state by component.txHistoryState.collectAsStateWithLifecycle()
    return TxHistory(component = component, state = state)
}

@Composable
private fun BalanceBlock(
    state: MoneyAccountBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemBalanceHeader(
        state = when (state) {
            is MoneyAccountBalanceBlockState.Loading -> TangemBalanceHeaderState.Loading
            is MoneyAccountBalanceBlockState.Content -> TangemBalanceHeaderState.Content(
                balance = state.fiatBalance,
                isFlickering = state.isBalanceFlickering,
                isBalanceHidden = isBalanceHidden,
            )
            is MoneyAccountBalanceBlockState.Unavailable -> TangemBalanceHeaderState.Error
        },
        label = resourceReference(R.string.token_details_balance_total),
        modifier = modifier,
        balanceModifier = Modifier.testTag(MoneyAccountTestTags.DETAILS_BALANCE),
    )
}

@Composable
private fun LazyItemScope.ActionBlock(state: MoneyAccountDetailsUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        TangemCircleActionButton(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag(MoneyAccountTestTags.ADD_FUNDS_BUTTON),
            title = resourceReference(R.string.common_add_funds),
            icon = TangemIconUM.Icon(imageVector = Icons.ic_arrow_down_24),
            onClick = state.onAddFundsClick,
            isEnabled = state.isAddFundsEnabled,
        )
        TangemCircleActionButton(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag(MoneyAccountTestTags.SEND_BUTTON),
            title = resourceReference(R.string.common_send),
            icon = TangemIconUM.Icon(imageVector = Icons.ic_arrow_up_24),
            onClick = state.onSendClick,
            isEnabled = state.isSendEnabled,
        )
    }
}

@Composable
private fun TopBar(state: MoneyAccountDetailsUM, onHeightChange: (Dp) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    TangemTopNavigation(
        modifier = modifier.onSizeChanged { size -> onHeightChange(with(density) { size.height.toDp() }) },
        title = state.title,
        subtitle = state.subtitle,
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        blurBackground = false,
        onBack = state.onBackClick,
        endButton = { MenuButton(items = state.menuItems, onClick = state.onMenuClick) },
    )
}

@Composable
private fun MenuButton(
    items: ImmutableList<MoneyAccountMenuItemUM>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        TangemButton(
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_dots_vertical_24),
            onClick = {
                isMenuShown = true
                onClick()
            },
            size = TangemButton.Size.X11,
            variant = TangemButton.Variant.Material,
        )
        TangemContextMenu(
            expanded = isMenuShown,
            onDismissRequest = { isMenuShown = false },
            offset = DpOffset.Zero,
        ) {
            items.fastForEach { item ->
                MenuItem(item = item, onMenuDismiss = { isMenuShown = false })
            }
        }
    }
}

@Composable
private fun MenuItem(item: MoneyAccountMenuItemUM, onMenuDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableSingle {
                item.onClick()
                onMenuDismiss()
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemIcon(
            modifier = Modifier.size(20.dp),
            tangemIconUM = item.icon,
        )
        Text(
            text = item.title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
        )
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun MoneyAccountDetailsScreenPreview() {
    TangemThemePreviewRedesign {
        MoneyAccountDetailsScreen(
            state = MoneyAccountDetailsUM(
                title = resourceReference(R.string.virtual_account_title),
                subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
                balance = MoneyAccountBalanceBlockState.Content(
                    fiatBalance = stringReference("$0.00"),
                    isBalanceFlickering = false,
                ),
                isBalanceHidden = false,
                isBalanceUnavailable = false,
                messageBanner = null,
                menuItems = persistentListOf(),
                onMenuClick = {},
                onBackClick = {},
                onAddFundsClick = {},
                onSendClick = {},
                isAddFundsEnabled = true,
                isSendEnabled = true,
                txHistoryCurrency = null,
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
                onTxHistoryReloadClick = {},
            ),
            txHistoryComponent = PreviewTxHistoryComponent,
        )
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun MoneyAccountDetailsScreenBalanceUnavailablePreview() {
    TangemThemePreviewRedesign {
        MoneyAccountDetailsScreen(
            state = MoneyAccountDetailsUM(
                title = resourceReference(R.string.virtual_account_title),
                subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
                balance = MoneyAccountBalanceBlockState.Unavailable,
                isBalanceHidden = false,
                isBalanceUnavailable = true,
                messageBanner = TangemMessageBanner.State(
                    title = resourceReference(R.string.money_account_balance_unavailable_title),
                    variant = TangemMessageBanner.Variant.Error,
                    description = resourceReference(R.string.money_account_balance_unavailable_description),
                ),
                menuItems = persistentListOf(),
                onMenuClick = {},
                onBackClick = {},
                onAddFundsClick = {},
                onSendClick = {},
                isAddFundsEnabled = false,
                isSendEnabled = false,
                txHistoryCurrency = null,
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
                onTxHistoryReloadClick = {},
            ),
            txHistoryComponent = PreviewTxHistoryComponent,
        )
    }
}

private object PreviewTxHistoryComponent : TxHistoryComponent {
    override val txHistoryState: StateFlow<TxHistoryItemsUM> = MutableStateFlow(
        value = TxHistoryItemsUM.Empty(isBalanceHidden = false, onExploreClick = {}),
    )

    override fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryItemsUM) = Unit

    @Composable
    override fun Content(modifier: Modifier) = Unit
}