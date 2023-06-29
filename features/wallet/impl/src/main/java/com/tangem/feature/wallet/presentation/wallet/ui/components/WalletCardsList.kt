package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.WalletCardState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Wallets list
 *
 * @param wallets  list of wallet state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WalletCardsList(wallets: ImmutableList<WalletCardState>, modifier: Modifier = Modifier) {
    val horizontalCardPadding = TangemTheme.dimens.spacing16
    val itemWidth = LocalConfiguration.current.screenWidthDp.dp - horizontalCardPadding * 2

    val lazyListState = rememberLazyListState()
    LazyRow(
        modifier = modifier.background(color = TangemTheme.colors.background.secondary),
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState),
    ) {
        items(items = wallets, key = WalletCardState::id) { state ->
            WalletCard(state = state, modifier = Modifier.width(itemWidth))
        }
    }
}

@Preview
@Composable
private fun Preview_WalletHeader_LightTheme() {
    TangemTheme(isDark = false) {
        WalletCardsList(
            wallets = persistentListOf(
                WalletPreviewData.walletCardContentState,
                WalletPreviewData.walletCardLoadingState,
                WalletPreviewData.walletCardHiddenContentState,
                WalletPreviewData.walletCardErrorState,
            ),
        )
    }
}

@Preview
@Composable
private fun Preview_WalletHeader_DarkTheme() {
    TangemTheme(isDark = true) {
        WalletCardsList(
            wallets = persistentListOf(
                WalletPreviewData.walletCardContentState,
                WalletPreviewData.walletCardLoadingState,
                WalletPreviewData.walletCardHiddenContentState,
                WalletPreviewData.walletCardErrorState,
            ),
        )
    }
}