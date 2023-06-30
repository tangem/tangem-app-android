package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.WalletsListConfig
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector

private const val MIN_OFFSET = -2
private const val MAX_OFFSET = 2

/**
 * Wallets list component
 *
 * @param config   config
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class, InternalCoroutinesApi::class)
@Composable
internal fun WalletsList(config: WalletsListConfig, modifier: Modifier = Modifier) {
    val horizontalCardPadding = TangemTheme.dimens.spacing16
    val itemWidth = LocalConfiguration.current.screenWidthDp.dp - horizontalCardPadding * 2

    val lazyListState = rememberLazyListState()
    LazyRow(
        modifier = modifier.background(color = TangemTheme.colors.background.secondary),
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState),
    ) {
        items(items = config.wallets, key = WalletCardState::id) { state ->
            WalletCard(state = state, modifier = Modifier.width(itemWidth))
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow(lazyListState::firstVisibleItemScrollOffset)
            .collect(
                collector = FlowCollector { firstVisibleItemScrollOffset ->
                    val itemSize = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                    val offset = lazyListState.firstVisibleItemIndex * itemSize + firstVisibleItemScrollOffset
                    val currentIndex = offset / itemSize
                    if (offset - currentIndex * itemSize in MIN_OFFSET..MAX_OFFSET) {
                        config.onWalletChange(currentIndex)
                    }
                },
            )
    }
}

@Preview
@Composable
private fun Preview_WalletHeader_LightTheme() {
    TangemTheme(isDark = false) {
        WalletsList(config = WalletPreviewData.walletListConfig)
    }
}

@Preview
@Composable
private fun Preview_WalletHeader_DarkTheme() {
    TangemTheme(isDark = true) {
        WalletsList(config = WalletPreviewData.walletListConfig)
    }
}