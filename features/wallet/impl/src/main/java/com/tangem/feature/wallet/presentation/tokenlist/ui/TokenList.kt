package com.tangem.feature.wallet.presentation.tokenlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUM
import com.tangem.feature.wallet.presentation.tokenlist.ui.preview.PreviewTokenListUMProvider
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.MultiCurrencyContentItem

/**
 * Token list
 *
 * @param state    state
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@Composable
internal fun TokenList(state: TokenListUM, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = state.items,
            key = { _, item -> item.id },
            contentType = { _, item -> item::class.java },
            itemContent = { index, item ->
                MultiCurrencyContentItem(
                    state = item,
                    isBalanceHidden = state.isBalanceHidden,
                    modifier = Modifier
                        .animateItem()
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = state.items.lastIndex,
                            addDefaultPadding = false,
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
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.secondary)
                .padding(16.dp),
        )
    }
}
