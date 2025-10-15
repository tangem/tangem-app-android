package com.tangem.features.tangempay.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.rememberAsyncImagePainter
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.buttons.actions.ActionButton
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.transactions.TxHistoryGroupTitle
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.EmptyTransactionBlockTestTags
import com.tangem.core.ui.test.TransactionHistoryBlockTestTags
import com.tangem.features.tangempay.entity.TangemPayEmptyTransactionHistoryState
import com.tangem.features.tangempay.entity.TangemPayTransactionState
import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM

private const val LOAD_ITEMS_BUFFER = 20

internal fun LazyListScope.tangemPayTxHistoryItems(listState: LazyListState, state: TangemPayTxHistoryUM) {
    when (state) {
        is TangemPayTxHistoryUM.Content -> contentItems(listState = listState, state = state)
        is TangemPayTxHistoryUM.Empty -> nonContentItem(state = TangemPayEmptyTransactionHistoryState.Empty)
        is TangemPayTxHistoryUM.Error -> nonContentItem(
            state = TangemPayEmptyTransactionHistoryState.FailedToLoad(onReload = state.onReload),
        )
        is TangemPayTxHistoryUM.Loading -> loadingItems(state = state)
    }
}

private fun LazyListScope.nonContentItem(state: TangemPayEmptyTransactionHistoryState, modifier: Modifier = Modifier) {
    item(key = state::class.java, contentType = state::class.java) {
        TangemPayEmptyTransactionBlock(
            state = state,
            modifier = modifier
                .padding(horizontal = TangemTheme.dimens.spacing16, vertical = TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        )
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
        itemContent = { index, item ->
            TangemPayTxHistoryListItem(
                state = item,
                isBalanceHidden = state.isBalanceHidden,
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = state.items.lastIndex,
                ),
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
        itemContent = { index, item ->
            TangemPayTxHistoryListItem(
                state = item,
                isBalanceHidden = true,
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = state.items.lastIndex,
                ),
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
        is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.GroupTitle -> {
            TxHistoryGroupTitle(config = state.legacyGroupTitle, modifier = modifier)
        }
        is TangemPayTxHistoryUM.TangemPayTxHistoryItemUM.Title -> {
            TangemPayTxHistoryTitle(modifier = modifier)
        }
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
private fun TangemPayTxHistoryTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResourceSafe(id = R.string.common_transactions),
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.subtitle2,
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(top = TangemTheme.dimens.spacing12)
            .padding(horizontal = TangemTheme.dimens.spacing12)
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size24)
            .testTag(TransactionHistoryBlockTestTags.TITLE_TEXT),
    )
}

@Suppress("LongMethod")
@Composable
private fun TangemPayTransaction(
    transactionState: TangemPayTransactionState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .clickable(
                enabled = transactionState is TangemPayTransactionState.Content,
                onClick = (transactionState as? TangemPayTransactionState.Content)?.onClick ?: {},
            )
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size56)
            .padding(
                vertical = TangemTheme.dimens.spacing8,
                horizontal = TangemTheme.dimens.spacing12,
            ),
        color = TangemTheme.colors.background.primary,
    ) {
        @Suppress("DestructuringDeclarationWithTooManyEntries")
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
            val (iconItem, titleItem, subtitleItem, amountItem, timestampItem) = createRefs()

            createVerticalChain(titleItem, subtitleItem, chainStyle = ChainStyle.Spread)
            createVerticalChain(amountItem, timestampItem, chainStyle = ChainStyle.Spread)

            Icon(
                state = transactionState,
                modifier = Modifier
                    .size(TangemTheme.dimens.size40)
                    .constrainAs(iconItem) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent)
                    },
            )

            Title(
                state = transactionState,
                modifier = Modifier
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing4,
                    )
                    .constrainAs(titleItem) {
                        top.linkTo(parent.top)
                        bottom.linkTo(subtitleItem.top)
                        start.linkTo(iconItem.end)
                        end.linkTo(amountItem.start)
                        width = Dimension.fillToConstraints
                    },
            )

            Subtitle(
                state = transactionState,
                modifier = Modifier
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing4,
                    )
                    .constrainAs(subtitleItem) {
                        top.linkTo(titleItem.bottom)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(iconItem.end)
                        end.linkTo(timestampItem.start)
                        width = Dimension.fillToConstraints
                    },
            )

            Amount(
                state = transactionState,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.constrainAs(amountItem) {
                    top.linkTo(parent.top)
                    bottom.linkTo(timestampItem.top)
                    start.linkTo(titleItem.end)
                    end.linkTo(parent.end)
                    width = Dimension.preferredWrapContent
                },
            )

            Timestamp(
                state = transactionState,
                modifier = Modifier.constrainAs(timestampItem) {
                    top.linkTo(amountItem.bottom)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                },
            )
        }
    }
}

@Composable
private fun Icon(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content.Spend -> {
            if (state.iconUrl != null) {
                RemoteIcon(modifier = modifier, url = state.iconUrl)
            } else {
                LocalStaticIcon(modifier = modifier, id = R.drawable.ic_category_24)
            }
        }
        is TangemPayTransactionState.Content.Fee -> LocalStaticIcon(modifier = modifier, id = R.drawable.ic_percent_24)
        is TangemPayTransactionState.Content.Payment -> LocalStaticIcon(
            modifier = modifier,
            id = if (state.isIncome) R.drawable.ic_arrow_down_24 else R.drawable.ic_arrow_up_24,
        )
        is TangemPayTransactionState.Loading -> {
            CircleShimmer(modifier = modifier.size(TangemTheme.dimens.size40))
        }
    }
}

@Composable
private fun RemoteIcon(url: String, modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier
            .size(TangemTheme.dimens.size40)
            .clip(CircleShape),
        painter = rememberAsyncImagePainter(url),
        contentDescription = null,
        tint = Color.Unspecified,
    )
}

@Composable
private fun LocalStaticIcon(@DrawableRes id: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size40)
            .background(color = TangemTheme.colors.icon.secondary.copy(alpha = 0.1F), shape = CircleShape),
    ) {
        Icon(
            painter = painterResource(id),
            contentDescription = null,
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .align(Alignment.Center),
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun Title(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> {
            Text(
                modifier = modifier,
                text = state.title.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle2,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
        is TangemPayTransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size70, height = TangemTheme.dimens.size12),
            )
        }
    }
}

@Composable
private fun Subtitle(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> {
            Text(
                text = state.subtitle.resolveReference(),
                modifier = modifier,
                textAlign = TextAlign.Start,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
            )
        }
        is TangemPayTransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size52, height = TangemTheme.dimens.size12),
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
                color = state.amountColor(),
                style = TangemTheme.typography.body2,
            )
        }
        is TangemPayTransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size72, height = TangemTheme.dimens.size12),
            )
        }
    }
}

@Composable
private fun Timestamp(state: TangemPayTransactionState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayTransactionState.Content -> {
            Text(
                text = state.time,
                modifier = modifier,
                textAlign = TextAlign.End,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption2,
            )
        }
        is TangemPayTransactionState.Loading -> {
            RectangleShimmer(
                modifier = modifier.size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12),
            )
        }
    }
}

@Composable
private fun TangemPayEmptyTransactionBlock(
    state: TangemPayEmptyTransactionHistoryState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(color = TangemTheme.colors.background.primary)
            .padding(vertical = TangemTheme.dimens.spacing24)
            .testTag(EmptyTransactionBlockTestTags.BLOCK),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens.size64)
                .testTag(EmptyTransactionBlockTestTags.ICON),
            painter = painterResource(id = state.iconRes),
            tint = TangemTheme.colors.icon.inactive,
            contentDescription = null,
        )

        Text(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing32)
                .testTag(EmptyTransactionBlockTestTags.TEXT),
            textAlign = TextAlign.Center,
            text = state.text.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )

        when (state) {
            is TangemPayEmptyTransactionHistoryState.Empty -> Unit
            is TangemPayEmptyTransactionHistoryState.FailedToLoad -> ActionButton(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                config = state.actionButtonConfig,
            )
        }
    }
}