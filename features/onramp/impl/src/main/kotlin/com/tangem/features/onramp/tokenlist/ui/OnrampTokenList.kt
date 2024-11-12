package com.tangem.features.onramp.tokenlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.ui.preview.PreviewTokenListUMProvider

/**
 * Token list
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun TokenList(state: TokenListUM, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        itemsIndexed(
            items = state.items,
            key = { _, item -> item.id },
            contentType = { _, item -> item::class.java },
            itemContent = { index, item ->
                TokenListItem(
                    state = item,
                    isBalanceHidden = state.isBalanceHidden,
                    modifier = Modifier
                        .animateItem()
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = state.items.lastIndex,
                            addDefaultPadding = false,
                            backgroundColor = TangemTheme.colors.background.primary,
                        ),
                )
            },
        )
    }
}

@Preview
@Composable
private fun Preview_TokenList(@PreviewParameter(PreviewTokenListUMProvider::class) state: TokenListUM) {
    TangemThemePreview {
        TokenList(
            state = state,
            contentPadding = PaddingValues(all = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.secondary),
        )
    }
}