package com.tangem.features.virtualaccount.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.topFade
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.features.virtualaccount.common.ui.TangemBalanceHeader
import com.tangem.features.virtualaccount.common.ui.TangemBalanceHeaderState
import com.tangem.features.virtualaccount.common.ui.TangemCircleActionButton
import com.tangem.features.virtualaccount.common.ui.TangemEmptyState
import com.tangem.features.virtualaccount.details.impl.R
import com.tangem.core.ui.R as CoreUiR

private val InitialTopBarHeight: Dp = 64.dp
private const val TOP_FADE_MID_STOP = 0.8f
private const val TOP_FADE_MID_ALPHA = 0.8f

@Composable
internal fun VirtualAccountMainScreen(state: VirtualAccountMainUM, modifier: Modifier = Modifier) {
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

private fun LazyListScope.body(state: VirtualAccountMainUM, listState: LazyListState) {
    item("balanceBlock") {
        BalanceBlock(
            state = state.balance,
            isBalanceHidden = state.isBalanceHidden,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens2.x4)
                .padding(top = TangemTheme.dimens2.x12),
        )
    }
    item("actionButtonsBlock") {
        SpacerH24()
        ActionBlock(state = state)
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
    state: VirtualAccountBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemBalanceHeader(
        state = when (state) {
            is VirtualAccountBalanceBlockState.Loading -> TangemBalanceHeaderState.Loading
            is VirtualAccountBalanceBlockState.Content -> TangemBalanceHeaderState.Content(
                balance = state.fiatBalance,
                isFlickering = state.isBalanceFlickering,
                isBalanceHidden = isBalanceHidden,
            )
            is VirtualAccountBalanceBlockState.Error -> TangemBalanceHeaderState.Error
        },
        label = resourceReference(R.string.token_details_balance_total),
        modifier = modifier,
    )
}

@Composable
private fun LazyItemScope.ActionBlock(state: VirtualAccountMainUM, modifier: Modifier = Modifier) {
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
private fun TopBar(state: VirtualAccountMainUM, onHeightChange: (Dp) -> Unit, modifier: Modifier = Modifier) {
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
        endContent = {
            TangemButton(
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_dots_vertical_24),
                onClick = state.onMenuClick,
                size = TangemButton.Size.X11,
                variant = TangemButton.Variant.Material,
            )
        },
    )
}

/**
 * Computes the height left between the top of the item identified by [itemKey] and the bottom of the
 * list's viewport (excluding bottom content padding). Returns `0.dp` until the item has been laid out.
 *
 * The item's own height does not affect its offset (only the items above it do), so reading the offset
 * back to size the item is stable and does not loop.
 */
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
private fun VirtualAccountMainScreenPreview() {
    TangemThemePreviewRedesign {
        VirtualAccountMainScreen(
            state = VirtualAccountMainUM(
                title = resourceReference(R.string.virtual_account_title),
                subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
                balance = VirtualAccountBalanceBlockState.Content(
                    fiatBalance = stringReference("$0.00"),
                    isBalanceFlickering = false,
                ),
                isBalanceHidden = false,
                onBackClick = {},
                onMenuClick = {},
                onAddFundsClick = {},
                onSendClick = {},
            ),
        )
    }
}