package com.tangem.features.tangempay.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_binoculars_20
import com.tangem.core.ui.test.EmptyTransactionBlockTestTags
import com.tangem.core.ui.test.TangemPayTestTags

@Immutable
sealed interface TangemPayEmptyTransactionHistoryState {

    val text: TextReference

    data class FailedToLoad(
        val onReload: () -> Unit,
        override val text: TextReference = resourceReference(R.string.transaction_history_error_failed_to_load),
    ) : TangemPayEmptyTransactionHistoryState

    data object Empty : TangemPayEmptyTransactionHistoryState {
        override val text: TextReference = resourceReference(R.string.transaction_history_empty_transactions)
    }
}

fun LazyListScope.tangemPayTxHistoryNonContentItem(
    listState: LazyListState,
    state: TangemPayEmptyTransactionHistoryState,
    modifier: Modifier = Modifier,
) {
    val itemKey = state::class.java
    item(key = itemKey, contentType = itemKey) {
        val fillRemaining = Modifier.heightIn(min = rememberRemainingViewportHeight(listState, itemKey))
        when (state) {
            is TangemPayEmptyTransactionHistoryState.Empty -> {
                TangemPayTransactionEmptyBlock(
                    state = state,
                    modifier = modifier
                        .then(fillRemaining)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                )
            }
            is TangemPayEmptyTransactionHistoryState.FailedToLoad -> {
                TangemPayFailedTransactionBlock(
                    state = state,
                    modifier = modifier
                        .then(fillRemaining)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TangemPayTransactionEmptyBlock(
    state: TangemPayEmptyTransactionHistoryState.Empty,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(EmptyTransactionBlockTestTags.BLOCK),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = TangemTheme.colors3.bg.transparent.primary,
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
                .padding(horizontal = 32.dp)
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
    state: TangemPayEmptyTransactionHistoryState.FailedToLoad,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(EmptyTransactionBlockTestTags.BLOCK),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemButton(
            modifier = Modifier.testTag(TangemPayTestTags.TRANSACTION_HISTORY_RELOAD_BUTTON),
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