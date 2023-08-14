package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState

/**
 * LazyList extension for [WalletTokensListState]
 *
 * @param state    state
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.tokensListItems(state: WalletTokensListState, modifier: Modifier = Modifier) {
    itemsIndexed(
        items = state.items,
        key = { _, item ->
            when (item) {
                is WalletTokensListState.TokensListItemState.NetworkGroupTitle -> item.value.hashCode()
                is WalletTokensListState.TokensListItemState.Token -> item.state.id
            }
        },
        contentType = { _, item -> item::class.java },
        itemContent = { index, item ->
            MultiCurrencyContentItem(
                state = item,
                modifier = modifier
                    .animateItemPlacement()
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = state.items.lastIndex,
                    ),
            )
        },
    )
}
