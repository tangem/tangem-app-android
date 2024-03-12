package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import com.tangem.feature.wallet.presentation.wallet.ui.utils.LazyListItemData
import com.tangem.feature.wallet.presentation.wallet.ui.utils.ScrollOffsetCollector
import com.tangem.feature.wallet.presentation.wallet.ui.utils.WalletsListInteractionsCollector

@Suppress("LongParameterList")
@Composable
internal fun WalletsListEffects(
    lazyListState: LazyListState,
    selectedWalletIndex: Int,
    onWalletChange: (Int) -> Unit,
    onSelectedWalletIndexSet: (Int) -> Unit,
    isAutoScroll: State<Boolean>,
    onAutoScrollReset: () -> Unit,
) {
    LaunchedEffect(key1 = lazyListState, key2 = onWalletChange) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.map {
                LazyListItemData(it.index, it.size, it.offset)
            }
        }
            .collect(
                collector = ScrollOffsetCollector(
                    selectedWalletIndex = selectedWalletIndex,
                    lazyListState = lazyListState,
                    onWalletChange = { newIndex ->
                        // Auto scroll must not change wallet
                        if (isAutoScroll.value) {
                            onSelectedWalletIndexSet(newIndex)
                        } else {
                            onSelectedWalletIndexSet(newIndex)
                            onWalletChange(newIndex)
                        }
                    },
                ),
            )
    }

    LaunchedEffect(Unit) {
        lazyListState.interactionSource.interactions.collect(
            collector = WalletsListInteractionsCollector(onDragStart = onAutoScrollReset),
        )
    }
}