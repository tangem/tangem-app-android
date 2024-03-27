package com.tangem.managetokens.presentation.managetokens.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.managetokens.state.AddCustomTokenButton
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState

private const val PLACEHOLDER_ITEMS_COUNT = 50

@Composable
internal fun TokensList(
    tokens: LazyPagingItems<TokenItemState>,
    addCustomTokenButton: AddCustomTokenButton,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Text(
                text = stringResource(id = R.string.manage_tokens_list_header_title),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
            )
        }

        if (tokens.loadState.refresh is LoadState.Loading) {
            items(PLACEHOLDER_ITEMS_COUNT) {
                TokenRowItem(state = TokenItemState.Loading(it.toString()))
            }
        } else {
            val tokensList = tokens.itemSnapshotList
            if (tokensList.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.manage_tokens_nothing_found),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                        modifier = Modifier.padding(
                            top = TangemTheme.dimens.spacing8,
                            start = TangemTheme.dimens.spacing16,
                            end = TangemTheme.dimens.spacing16,
                        ),
                    )
                }
            }

            items(items = tokensList.items, key = TokenItemState::id) { token ->
                TokenRowItem(state = token)
            }

            if (addCustomTokenButton.isVisible) {
                item {
                    AddCustomTokenButton(onButtonClick = addCustomTokenButton.onClick)
                }
            }
        }
    }
}
