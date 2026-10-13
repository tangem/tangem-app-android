package com.tangem.features.moneyaccount.main

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.topFade
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.features.moneyaccount.common.ui.TangemBalanceHeader
import com.tangem.features.moneyaccount.common.ui.TangemBalanceHeaderState
import com.tangem.features.moneyaccount.common.ui.TangemCircleActionButton
import com.tangem.features.moneyaccount.common.ui.TangemEmptyState
import com.tangem.features.moneyaccount.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

private val InitialTopBarHeight: Dp = 64.dp
private const val TOP_FADE_MID_STOP = 0.8f
private const val TOP_FADE_MID_ALPHA = 0.8f
private const val UNAVAILABLE_BALANCE_ALPHA = 0.4f
private val MessageBannerCornerRadius: Dp = 28.dp

@Composable
internal fun MoneyAccountMainScreen(state: MoneyAccountMainUM, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.systemBars.getTop(this).toDp() }
    val bottomBarHeight = with(density) { WindowInsets.systemBars.getBottom(this).toDp() }
    var topBarTotalHeight by remember { mutableStateOf(InitialTopBarHeight + statusBarHeight) }
    val rootBackground = TangemTheme.colors3.bg.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(rootBackground),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .topFade(
                    height = topBarTotalHeight,
                    0f to rootBackground,
                    TOP_FADE_MID_STOP to rootBackground.copy(alpha = TOP_FADE_MID_ALPHA),
                    1f to Color.Transparent,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = listState,
            contentPadding = PaddingValues(
                top = topBarTotalHeight,
                bottom = TangemTheme.dimens2.x4 + bottomBarHeight,
            ),
        ) {
            body(
                state = state,
                listState = listState,
            )
        }
        TopBar(
            state = state,
            onHeightChange = { measuredHeight ->
                if (topBarTotalHeight != measuredHeight) topBarTotalHeight = measuredHeight
            },
        )
    }
}

private fun LazyListScope.body(state: MoneyAccountMainUM, listState: LazyListState) {
    val isBalanceUnavailable = state.balance is MoneyAccountBalanceBlockState.Unavailable
    val balanceAlpha = if (isBalanceUnavailable) UNAVAILABLE_BALANCE_ALPHA else 1f

    item("balanceBlock") {
        BalanceBlock(
            state = state.balance,
            isBalanceHidden = state.isBalanceHidden,
            modifier = Modifier
                .alpha(balanceAlpha)
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens2.x4)
                .padding(top = TangemTheme.dimens2.x12),
        )
    }
    item("actionButtonsBlock") {
        SpacerH24()
        ActionBlock(state = state, modifier = Modifier.alpha(balanceAlpha))
    }
    if (isBalanceUnavailable) {
        item("balanceUnavailableBanner") {
            SpacerH24()
            BalanceUnavailableBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TangemTheme.dimens2.x4),
            )
        }
    }
    item("emptyTransactions") {
        SpacerH24()
        TangemEmptyState(
            icon = Icons.ic_binoculars_20,
            text = resourceReference(R.string.virtual_account_transactions_empty),
            modifier = Modifier
                .heightIn(min = rememberRemainingViewportHeight(listState, "emptyTransactions"))
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3),
        )
    }
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
    )
}

@Composable
private fun BalanceUnavailableBanner(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TangemMessageBanner(
            modifier = Modifier.fillMaxWidth(),
            title = stringReference("Balance unavailable"),
            description = stringReference("We're having trouble loading your balance. Try again later"),
            showGlowRing = false,
            slotEnd = {
                TangemIcon(
                    modifier = Modifier.size(TangemTheme.dimens2.x5),
                    tangemIconUM = TangemIconUM.Icon(
                        imageVector = Icons.ic_error_20,
                        tintReference = { TangemTheme.colors3.icon.primary },
                    ),
                )
            },
        )
        TangemGlowRing(
            modifier = Modifier.matchParentSize(),
            variant = TangemGlowRing.Variant.Error,
            cornerRadius = MessageBannerCornerRadius,
        )
    }
}

@Composable
private fun LazyItemScope.ActionBlock(state: MoneyAccountMainUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        TangemCircleActionButton(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
            title = resourceReference(R.string.common_add_funds),
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_arrow_down_24,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
            onClick = state.onAddFundsClick,
        )
        TangemCircleActionButton(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x4),
            title = resourceReference(R.string.common_send),
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_arrow_up_24,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
            onClick = state.onSendClick,
        )
    }
}

@Composable
private fun TopBar(state: MoneyAccountMainUM, onHeightChange: (Dp) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    TangemTopBar(
        modifier = modifier
            .onSizeChanged { size -> onHeightChange(with(density) { size.height.toDp() }) }
            .statusBarsPadding(),
        title = state.title,
        subtitle = state.subtitle,
        startContent = {
            TangemButton(
                iconStart = TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_arrow_back_28),
                onClick = state.onBackClick,
                size = TangemButton.Size.X11,
                variant = TangemButton.Variant.Material,
            )
        },
        endContent = { MenuButton(items = state.menuItems) },
    )
}

@Composable
private fun MenuButton(items: ImmutableList<MoneyAccountMenuItemUM>, modifier: Modifier = Modifier) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        TangemButton(
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_dots_vertical_24),
            onClick = { isMenuShown = true },
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
            .padding(vertical = TangemTheme.dimens2.x3, horizontal = TangemTheme.dimens2.x4),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemIcon(
            modifier = Modifier.size(TangemTheme.dimens2.x5),
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

@Composable
private fun rememberRemainingViewportHeight(listState: LazyListState, itemKey: Any): Dp {
    val density = LocalDensity.current
    val remainingPx by remember(listState, itemKey) {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == itemKey }
                ?: return@derivedStateOf 0
            (info.viewportEndOffset - info.afterContentPadding - item.offset).coerceAtLeast(minimumValue = 0)
        }
    }
    return with(density) { remainingPx.toDp() }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun MoneyAccountMainScreenPreview() {
    TangemThemePreviewRedesign {
        MoneyAccountMainScreen(
            state = MoneyAccountMainUM(
                title = resourceReference(R.string.virtual_account_title),
                subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
                balance = MoneyAccountBalanceBlockState.Content(
                    fiatBalance = stringReference("$0.00"),
                    isBalanceFlickering = false,
                ),
                isBalanceHidden = false,
                menuItems = persistentListOf(),
                onBackClick = {},
                onAddFundsClick = {},
                onSendClick = {},
            ),
        )
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun MoneyAccountMainScreenBalanceUnavailablePreview() {
    TangemThemePreviewRedesign {
        MoneyAccountMainScreen(
            state = MoneyAccountMainUM(
                title = resourceReference(R.string.virtual_account_title),
                subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
                balance = MoneyAccountBalanceBlockState.Unavailable,
                isBalanceHidden = false,
                menuItems = persistentListOf(),
                onBackClick = {},
                onAddFundsClick = {},
                onSendClick = {},
            ),
        )
    }
}