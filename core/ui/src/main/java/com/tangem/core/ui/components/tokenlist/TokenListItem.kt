package com.tangem.core.ui.components.tokenlist

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.tokenlist.internal.NetworkTitleItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resolveReference

/**
 * Multi-currency content item
 *
 * @param state           component UI model
 * @param isBalanceHidden flag that shows/hides balance
 * @param modifier        modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TokenListItem(state: TokensListItemUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TokensListItemUM.NetworkGroupTitle -> {
            NetworkTitleItem(networkName = state.name.resolveReference(), modifier = modifier)
        }
        is TokensListItemUM.Token -> {
            TokenItem(
                state = state.state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
        is TokensListItemUM.SearchBar -> {
            SearchBar(state = state.searchBarUM, modifier = modifier.padding(all = 12.dp))
        }
    }
}