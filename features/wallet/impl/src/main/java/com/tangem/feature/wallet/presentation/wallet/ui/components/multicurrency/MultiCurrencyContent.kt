package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.ui.decorations.walletContentItemDecoration

/**
 * LazyList extension for [WalletTokensListState]
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.tokensListItems(state: WalletTokensListState, modifier: Modifier = Modifier) {
    itemsIndexed(
        items = state.items,
        key = { index, _ -> index },
        itemContent = { index, item ->
            MultiCurrencyContentItem(
                state = item,
                modifier = modifier.walletContentItemDecoration(
                    currentIndex = index,
                    lastIndex = state.items.lastIndex,
                ),
            )
        },
    )
}