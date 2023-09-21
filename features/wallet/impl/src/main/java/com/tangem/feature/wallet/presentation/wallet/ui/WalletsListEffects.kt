package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.ui.utils.ScrollOffsetCollector
import com.tangem.feature.wallet.presentation.wallet.ui.utils.WalletsListInteractionsCollector

@Composable
internal fun WalletsListEffects(
    lazyListState: LazyListState,
    walletsListConfig: WalletsListConfig,
    isAutoScroll: State<Boolean>,
    onAutoScrollReset: () -> Unit,
) {
    LaunchedEffect(key1 = lazyListState, key2 = walletsListConfig.onWalletChange) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect(
                collector = ScrollOffsetCollector(
                    lazyListState = lazyListState,
                    walletsListConfig = walletsListConfig,
                    isAutoScroll = isAutoScroll,
                ),
            )
    }

    LaunchedEffect(Unit) {
        lazyListState.interactionSource.interactions.collect(
            collector = WalletsListInteractionsCollector(onDragStart = onAutoScrollReset),
        )
    }
}