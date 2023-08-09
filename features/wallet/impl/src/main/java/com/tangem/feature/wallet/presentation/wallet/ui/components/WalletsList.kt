package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.ui.components.common.WalletCard

/**
 * Wallets list component
 *
 * @param config        config
 * @param lazyListState main content container list state
 *
 * @author Andrew Khokhlov on 30/05/2023
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WalletsList(config: WalletsListConfig, lazyListState: LazyListState) {
    val horizontalCardPadding = TangemTheme.dimens.spacing16
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth by remember(screenWidth) { derivedStateOf { screenWidth - horizontalCardPadding * 2 } }

    LazyRow(
        modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState),
    ) {
        items(items = config.wallets, key = { it.id.stringValue }) { state ->
            WalletCard(
                state = state,
                modifier = Modifier
                    .animateItemPlacement()
                    .width(itemWidth),
            )
        }
    }
}

@Preview
@Composable
private fun Preview_WalletsList_LightTheme() {
    TangemTheme(isDark = false) {
        WalletsList(config = WalletPreviewData.walletListConfig, lazyListState = rememberLazyListState())
    }
}

@Preview
@Composable
private fun Preview_WalletsList_DarkTheme() {
    TangemTheme(isDark = true) {
        WalletsList(config = WalletPreviewData.walletListConfig, lazyListState = rememberLazyListState())
    }
}
