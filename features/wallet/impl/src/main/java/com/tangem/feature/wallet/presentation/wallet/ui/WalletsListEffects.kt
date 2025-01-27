package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.tangem.feature.wallet.presentation.wallet.ui.utils.CenterOfItemScrollingDetector
import com.tangem.feature.wallet.presentation.wallet.ui.utils.LazyListItemData
import com.tangem.feature.wallet.presentation.wallet.ui.utils.WalletsListInteractionsCollector

/**
 * Wallets list effects
 *
 * @param lazyListState       wallets list [LazyListState]
 * @param selectedWalletIndex selected wallet index
 * @param onUserScroll        lambda be invoked when user scrolls the list
 * @param onIndexChange       lambda be invoked when index is changed
 */
@Composable
internal fun WalletsListEffects(
    lazyListState: LazyListState,
    selectedWalletIndex: Int,
    onUserScroll: () -> Unit,
    onIndexChange: (Int) -> Unit,
) {
    LaunchedEffect(key1 = lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.map {
                LazyListItemData(it.index, it.size, it.offset)
            }
        }
            .collect(
                collector = CenterOfItemScrollingDetector(
                    initialIndex = selectedWalletIndex,
                    lazyListState = lazyListState,
                    onIndexChange = { newIndex -> onIndexChange(newIndex) },
                ),
            )
    }

    LaunchedEffect(Unit) {
        lazyListState.interactionSource.interactions.collect(
            collector = WalletsListInteractionsCollector(onDragStart = onUserScroll),
        )
    }
}