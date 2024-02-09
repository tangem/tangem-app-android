package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import kotlinx.collections.immutable.ImmutableList
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletTokensListState as WalletTokensListStateV2
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletTokensListState.TokensListItemState as TokensListItemStateV2

private const val NON_CONTENT_TOKENS_LIST_KEY = "NON_CONTENT_TOKENS_LIST"

/**
 * LazyList extension for [WalletTokensListState]
 *
 * @param state    state
 * @param modifier modifier
 *
 * @author Andrew Khokhlov on 02/08/2023
 */
internal fun LazyListScope.tokensListItemsV2(
    state: WalletTokensListStateV2,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
) {
    when (state) {
        is WalletTokensListStateV2.ContentState -> {
            contentItemsV2(
                items = state.items,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
        WalletTokensListStateV2.Empty -> nonContentItem(modifier = modifier)
    }
}

private fun LazyListScope.contentItemsV2(
    items: ImmutableList<TokensListItemStateV2>,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean,
) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.id },
        contentType = { _, item -> item::class.java },
        itemContent = { index, item ->
            MultiCurrencyContentItem(
                state = item,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = items.lastIndex,
                ),
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.nonContentItem(modifier: Modifier = Modifier) {
    item(
        key = NON_CONTENT_TOKENS_LIST_KEY,
        contentType = NON_CONTENT_TOKENS_LIST_KEY,
    ) {
        Column(
            modifier = modifier
                .animateItemPlacement()
                .padding(top = TangemTheme.dimens.spacing96),
            verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_empty_64),
                contentDescription = null,
                modifier = Modifier.size(size = TangemTheme.dimens.size64),
                tint = TangemTheme.colors.icon.inactive,
            )

            Text(
                text = stringResource(id = R.string.main_empty_tokens_list_message),
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing48),
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
                style = TangemTheme.typography.caption2,
            )
        }
    }
}
