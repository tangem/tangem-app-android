package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.ui.utils.ScrollOffsetCollector

/**
 * Wallet screen side effects
 *
 * @param lazyListState     lazy list state
 * @param walletsListConfig wallets list config
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletSideEffects(lazyListState: LazyListState, walletsListConfig: WalletsListConfig) {
    LaunchedEffect(key1 = walletsListConfig.selectedWalletIndex) {
        lazyListState.scrollToItem(walletsListConfig.selectedWalletIndex)
    }

    val dragInteraction = lazyListState.interactionSource.interactions.collectAsState(initial = null)
    LaunchedEffect(key1 = lazyListState, key2 = walletsListConfig.onWalletChange) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect(
                collector = ScrollOffsetCollector(
                    lazyListState = lazyListState,
                    dragInteraction = dragInteraction,
                    callback = walletsListConfig.onWalletChange,
                ),
            )
    }
}