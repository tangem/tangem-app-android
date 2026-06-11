package com.tangem.features.tangempay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.RectangleShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmer
import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_binoculars_20
import com.tangem.core.ui.test.EmptyTransactionBlockTestTags
import com.tangem.features.tangempay.entity.TangemPayEmptyTransactionHistoryStateV2
import com.tangem.features.tangempay.entity.TangemPayTransactionState
import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM

private const val LOAD_ITEMS_BUFFER = 20

internal fun LazyListScope.tangemPayTxHistoryItemsV2(listState: LazyListState, state: TangemPayTxHistoryUM) {
    when (state) {
        is TangemPayTxHistoryUM.Content -> {
            contentItems(listState = listState, state = state)
        }
        is TangemPayTxHistoryUM.Empty -> {
            nonContentItem(listState = listState, state = TangemPayEmptyTransactionHistoryStateV2.Empty)
        }
        is TangemPayTxHistoryUM.Error -> {
            nonContentItem(
                listState = listState,
                state = TangemPayEmptyTransactionHistoryStateV2.FailedToLoad(onReload = state.onReload),
            )
        }
        is TangemPayTxHistoryUM.Loading -> {
            loadingItems(state = state)
        }
    }
}

private fun LazyListScope.nonContentItem(
    listState: LazyListState,
    state: TangemPayEmptyTransactionHistoryStateV2,
    modifier: Modifier = Modifier,
) {
    val itemKey = state::class.java
    item(key = itemKey, contentType = itemKey) {
        val fillRemaining = Modifier.heightIn(min = rememberRemainingViewportHeight(listState, itemKey))
        when (state) {
            is TangemPayEmptyTransactionHistoryStateV2.Empty -> {
                TangemPayTransactionEmptyBlock(
                    state = state,
                    modifier = modifier
                        .then(fillRemaining)
                        .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3)
                        .fillMaxWidth(),
                )
            }
            is TangemPayEmptyTransactionHistoryStateV2.FailedToLoad -> {
                TangemPayFailedTransactionBlock(
                    state = state,
                    modifier = modifier
                        .then(fillRemaining)
                        .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x3)
                        .fillMaxWidth(),
                )
            }
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
            TextShimmer(
                modifier = Modifier.width(TangemTheme.dimens2.x10),
                text = state.title,
                style = TextShimmerStyle.SUBHEADING,
                radius = TangemTheme.dimens2.x25,
            )
        }
    } else {
        Text(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    vertical = TangemTheme.dimens2.x3,
                    horizontal = TangemTheme.dimens2.x4,
                ),
            text = state.legacyGroupTitle.title,
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
        startSlot = { Icon(state = transactionState, modifier = Modifier.size(TangemTheme.dimens2.x10)) },
        titleSlot = { Title(state = transactionState) },
        subtitleSlot = { Subtitle(state = transactionState) },
        valueSlot = { Amount(state = transactionState, isBalanceHidden = isBalanceHidden) },
        subvalueSlot = { Timestamp(state = transactionState) },
    )
}

@Composable
private fun Icon(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> TransactionListIcon(
            iconState = state.iconV2,
            modifier = modifier,
        )
        is TangemPayTransactionState.Loading -> RectangleShimmer(
            modifier = modifier.size(TangemTheme.dimens2.x10),
            radius = TangemTheme.dimens2.x25,
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
                    .size(TangemTheme.dimens2.x10)
                    .clip(CircleShape),
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .size(TangemTheme.dimens2.x10)
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.bg.opaque.primary),
                contentAlignment = Alignment.Center,
            ) {
                TangemIcon(
                    tangemIconUM = iconState,
                    modifier = Modifier.size(TangemTheme.dimens.size20),
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
            TextShimmer(
                modifier = modifier,
                text = "Transfer",
                radius = TangemTheme.dimens2.x25,
                style = TextShimmerStyle.BODY,
            )
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
            TextShimmer(
                modifier = modifier,
                text = "Transfer",
                radius = TangemTheme.dimens2.x25,
                style = TextShimmerStyle.CAPTION,
            )
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
                color = state.amountColorV2(),
                style = TangemTheme.typography3.body.medium,
            )
        }
        is TangemPayTransactionState.Loading -> {
            TextShimmer(
                modifier = modifier,
                text = "10000",
                radius = TangemTheme.dimens2.x25,
                style = TextShimmerStyle.BODY,
            )
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
            TextShimmer(
                modifier = modifier,
                text = "00:00",
                radius = TangemTheme.dimens2.x25,
                style = TextShimmerStyle.CAPTION,
            )
        }
    }
}

@Composable
private fun TangemPayTransactionEmptyBlock(
    state: TangemPayEmptyTransactionHistoryStateV2.Empty,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(EmptyTransactionBlockTestTags.BLOCK),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens2.x10)
                .background(
                    color = TangemTheme.colors3.bg.opaque.primary,
                    shape = CircleShape,
                )
                .padding(10.dp)
                .testTag(EmptyTransactionBlockTestTags.ICON),
            imageVector = Icons.ic_binoculars_20,
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )

        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing32)
                .testTag(EmptyTransactionBlockTestTags.TEXT),
            textAlign = TextAlign.Center,
            text = state.text.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun TangemPayFailedTransactionBlock(
    state: TangemPayEmptyTransactionHistoryStateV2.FailedToLoad,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(EmptyTransactionBlockTestTags.BLOCK),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemButton(
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_refresh_20),
            onClick = state.onReload,
        )

        Text(
            modifier = Modifier.testTag(EmptyTransactionBlockTestTags.TEXT),
            textAlign = TextAlign.Center,
            text = state.text.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

/**
 * Computes the height left between the top of the item identified by [itemKey] and the bottom of the
 * list's viewport (excluding bottom content padding). Returns `0.dp` until the item has been laid out.
 *
 * The item's own height does not affect its offset (only the items above it do), so reading the offset
 * back to size the item is stable and does not loop.
 */
@Composable
private fun rememberRemainingViewportHeight(listState: LazyListState, itemKey: Any): Dp {
    val density = LocalDensity.current
    val remainingPx by remember(listState, itemKey) {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == itemKey }
                ?: return@derivedStateOf 0
            (info.viewportEndOffset - info.afterContentPadding - item.offset).coerceAtLeast(minimumValue = 0)
        }
    }
    return with(density) { remainingPx.toDp() }
}