package com.tangem.features.tangempay.txhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.common.TangemPayEmptyTransactionHistoryState
import com.tangem.features.tangempay.common.tangemPayTxHistoryNonContentItem

private const val LOAD_ITEMS_BUFFER = 20

internal fun LazyListScope.tangemPayTxHistoryItems(listState: LazyListState, state: TangemPayTxHistoryUM) {
    when (state) {
        is TangemPayTxHistoryUM.Content -> {
            contentItems(listState = listState, state = state)
        }
        is TangemPayTxHistoryUM.Empty -> {
            tangemPayTxHistoryNonContentItem(listState = listState, state = TangemPayEmptyTransactionHistoryState.Empty)
        }
        is TangemPayTxHistoryUM.Error -> {
            tangemPayTxHistoryNonContentItem(
                listState = listState,
                state = TangemPayEmptyTransactionHistoryState.FailedToLoad(onReload = state.onReload),
            )
        }
        is TangemPayTxHistoryUM.Loading -> {
            loadingItems(state = state)
        }
    }
}

private fun LazyListScope.contentItems(listState: LazyListState, state: TangemPayTxHistoryUM.Content) {
    itemsIndexed(
        items = state.items,
        key = { index, item ->
            when (item) {
                is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle -> item.itemKey
                is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Title -> index + item.hashCode()
                is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction ->
                    item.transaction.id + (item.transaction as? TangemPayTransactionState.Content)?.hashCode()
            }
        },
        contentType = { _, item -> item::class.java },
        itemContent = { _, item ->
            TangemPayTxHistoryListItem(
                state = item,
                isBalanceHidden = state.isBalanceHidden,
            )
        },
    )
    item {
        InfiniteListHandler(
            listState = listState,
            buffer = LOAD_ITEMS_BUFFER,
            onLoadMore = state.loadMore,
        )
    }
}

private fun LazyListScope.loadingItems(state: TangemPayTxHistoryUM.Loading) {
    itemsIndexed(
        items = state.items,
        key = { _, item ->
            when (item) {
                is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle -> item.itemKey
                is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Title -> item.hashCode()
                is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction ->
                    item.transaction.id + (item.transaction as? TangemPayTransactionState.Content)?.hashCode()
            }
        },
        contentType = { _, item -> item::class.java },
        itemContent = { _, item ->
            TangemPayTxHistoryListItem(
                state = item,
                isBalanceHidden = true,
            )
        },
    )
}

@Composable
private fun TangemPayTxHistoryListItem(
    state: TangemPayTxHistoryUM.TangemPayTxHistoryItemUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle -> GroupTitleBlock(state, modifier)
        is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Title -> Unit
        is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Transaction -> {
            TangemPayTransaction(
                transactionState = state.transaction,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun GroupTitleBlock(
    state: TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 16.dp),
        ) {
            TangemShimmer(style = TangemTheme.typography3.subheading.medium)
        }
    } else {
        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    vertical = 12.dp,
                    horizontal = 16.dp,
                ),
            text = state.title,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun TangemPayTransaction(
    transactionState: TangemPayTransactionState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        modifier = modifier.clickable(
            enabled = transactionState is TangemPayTransactionState.Content,
            onClick = (transactionState as? TangemPayTransactionState.Content)?.onClick ?: {},
        ),
        startSlot = { Icon(state = transactionState, modifier = Modifier.size(40.dp)) },
        titleSlot = { Title(state = transactionState) },
        subtitleSlot = { Subtitle(state = transactionState) },
        valueSlot = { Amount(state = transactionState, isBalanceHidden = isBalanceHidden) },
        subvalueSlot = {
            (transactionState as? TangemPayTransactionState.Content.Spend)?.cashback?.let { cashback ->
                CashbackBadge(cashback = cashback, isBalanceHidden = isBalanceHidden)
            }
            Timestamp(state = transactionState)
        },
    )
}

@Composable
private fun CashbackBadge(
    cashback: TangemPayTransactionCashbackUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemBadge(
        text = stringReference(cashback.amount.orMaskWithStars(isBalanceHidden)),
        modifier = modifier.testTag(TangemPayTestTags.TRANSACTION_CASHBACK_BADGE),
        variant = TangemBadge.Variant.Tinted,
        status = when (cashback.style) {
            TangemPayTransactionCashbackUM.Style.Confirmed -> TangemBadge.Status.Info
            TangemPayTransactionCashbackUM.Style.Estimated -> TangemBadge.Status.Neutral
        },
        size = TangemBadge.Size.X4,
    )
}

@Composable
private fun Icon(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> TransactionListIcon(
            iconState = state.icon,
            modifier = modifier,
        )
        is TangemPayTransactionState.Loading -> TangemShimmer(
            modifier = modifier.size(40.dp),
            radius = 100.dp,
        )
    }
}

@Composable
private fun TransactionListIcon(iconState: TangemIconUM, modifier: Modifier = Modifier) {
    when (iconState) {
        is TangemIconUM.Url -> {
            TangemIcon(
                tangemIconUM = iconState,
                modifier = modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.bg.transparent.primary),
                contentAlignment = Alignment.Center,
            ) {
                TangemIcon(
                    tangemIconUM = iconState,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun Title(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> {
            TangemRowText(text = state.title.resolveReference(), role = TangemRowTextRole.Title)
        }
        is TangemPayTransactionState.Loading -> {
            TangemShimmer(modifier = modifier, style = TangemTheme.typography3.body.medium)
        }
    }
}

@Composable
private fun Subtitle(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> {
            TangemRowText(text = state.subtitle.resolveReference(), role = TangemRowTextRole.Subtitle)
        }
        is TangemPayTransactionState.Loading -> {
            TangemShimmer(modifier = modifier, style = TangemTheme.typography3.caption.medium)
        }
    }
}

@Composable
private fun Amount(state: TangemPayTransactionState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> {
            Text(
                text = state.amount.orMaskWithStars(isBalanceHidden),
                modifier = modifier,
                textAlign = TextAlign.End,
                color = state.amountColor(),
                style = TangemTheme.typography3.body.medium,
            )
        }
        is TangemPayTransactionState.Loading -> {
            TangemShimmer(modifier = modifier, style = TangemTheme.typography3.body.medium)
        }
    }
}

@Composable
private fun Timestamp(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> {
            TangemRowText(text = state.time, role = TangemRowTextRole.Subvalue)
        }
        is TangemPayTransactionState.Loading -> {
            TangemShimmer(modifier = modifier, style = TangemTheme.typography3.caption.medium)
        }
    }
}